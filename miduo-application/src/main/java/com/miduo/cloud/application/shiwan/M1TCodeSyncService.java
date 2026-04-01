package com.miduo.cloud.application.shiwan;

import com.miduo.cloud.common.config.M1SyncCursorStore;
import com.miduo.cloud.common.config.ShiwanM2SettingsDto;
import com.miduo.cloud.common.config.ShiwanM2SettingsFileLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 1 号机 T_Code 表定时同步到 2 号机 CodeRelationUpload。
 * 每 3 秒执行一次：从 1 号机 SQL Server 读取 SerialNo &gt; lastSynced 且 Status=0 的记录，
 * 插入 2 号机 MySQL CodeRelationUpload。同步落库阶段 OrderNo/ProductNO/TagNo 统一写空字符串，
 * 待后续盒箱关联成功时再回填当前选中的订单号与产品编码。
 * 启用条件：shiwan.m2.m1-sync.enabled=true
 * 运行时控制：调用 startSync()/stopSync() 动态开启/关闭每次执行逻辑。
 */
@Service
@ConditionalOnProperty(name = "shiwan.m2.m1-sync.enabled", havingValue = "true")
public class M1TCodeSyncService {

    private static final Logger log = LoggerFactory.getLogger(M1TCodeSyncService.class);
    private static final String JTDS_DRIVER = "net.sourceforge.jtds.jdbc.Driver";
    private static final String JTDS_URL_TEMPLATE = "jdbc:jtds:sqlserver://%s:%s/%s;loginTimeout=3;socketTimeout=3000";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    /** 每轮最多同步记录数，避免单次任务占用连接过久。 */
    private static final int MAX_ROWS_PER_SYNC = 300;
    /** 前端可拉取的同步事件队列最大容量 */
    private static final int MAX_SYNC_EVENTS = 200;

    /** 运行时开关：只有 true 时才执行同步逻辑 */
    private volatile boolean syncActive = false;
    /** 防重入：上一轮未完成时跳过下一轮触发。 */
    private final AtomicBoolean syncRunning = new AtomicBoolean(false);

    /** 同步事件序号，单调递增 */
    private final AtomicLong syncEventSeq = new AtomicLong(0);
    /** 同步结果事件队列，供前端轮询 */
    private final Deque<Map<String, Object>> syncEventQueue = new ArrayDeque<>(MAX_SYNC_EVENTS);

    private final JdbcTemplate jdbcTemplate;

    public M1TCodeSyncService(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /** 前端开始采集后调用：激活同步 */
    public void startSync() {
        // 清空待拉取事件，避免停止后再开始采集时前端 lastSeq 从 0 把历史事件整批重复展示
        synchronized (this) {
            syncEventQueue.clear();
        }
        syncActive = true;
        log.info("1号机 T_Code 同步已启动");
    }

    /** 前端停止采集后调用：暂停同步 */
    public void stopSync() {
        syncActive = false;
        log.info("1号机 T_Code 同步已停止");
    }

    public boolean isSyncActive() {
        return syncActive;
    }

    @Scheduled(fixedDelay = 3000)
    public void sync() {
        if (!syncActive) {
            return;
        }
        if (!syncRunning.compareAndSet(false, true)) {
            log.debug("1号机 T_Code 同步: 上一轮仍在执行，跳过本轮触发");
            return;
        }
        try {
            doSync();
        } finally {
            syncRunning.set(false);
        }
    }

    private void doSync() {
        ShiwanM2SettingsDto config = ShiwanM2SettingsFileLoader.load();
        if (config == null || config.getM1DbConnection() == null) {
            log.warn("1号机 T_Code 同步: 未找到 M1 连接配置(shiwan-m2-settings.json 中 m1DbConnection)，跳过本次");
            return;
        }
        ShiwanM2SettingsDto.M1DbConnection m1 = config.getM1DbConnection();
        String host = m1.getHost() != null ? m1.getHost().trim() : null;
        String port = m1.getPort() != null ? m1.getPort().trim() : "1433";
        String database = m1.getDatabase() != null ? m1.getDatabase().trim() : null;
        String tableName = m1.getTableName() != null && m1.getTableName().matches("^[a-zA-Z0-9_]+$")
                ? m1.getTableName().trim() : "T_Code";
        String username = m1.getUsername() != null ? m1.getUsername().trim() : null;
        String password = m1.getPassword() != null ? m1.getPassword() : "";
        if (host == null || host.isEmpty() || database == null || database.isEmpty() || username == null || username.isEmpty()) {
            log.warn("1号机 T_Code 同步: M1 连接配置不完整(host={} database={} username={})，跳过本次", host, database, username);
            return;
        }

        Long lastSynced = M1SyncCursorStore.loadLastSyncedSerialNo();
        if (lastSynced == null) {
            // 游标文件不存在（首次同步）：优先使用配置中设置的初始游标值
            Long initialSerialNo = m1.getInitialSerialNo();
            if (initialSerialNo != null && initialSerialNo > 0) {
                lastSynced = initialSerialNo;
                log.info("1号机 T_Code 同步: 首次同步，使用配置的初始游标 SerialNo={}", lastSynced);
            } else {
                lastSynced = 0L;
                log.info("1号机 T_Code 同步: 首次同步，未配置初始游标，从 SerialNo=0 开始拉取全部数据");
            }
            // 立即持久化初始游标，防止游标文件始终不存在导致每轮都打印"首次同步"
            M1SyncCursorStore.saveLastSyncedSerialNo(lastSynced);
        }

        // 同步落库阶段不写订单号/产品编号，待后续盒箱关联成功后回填
        String orderNo = "";
        String productNo = "";

        String jdbcUrl = String.format(JTDS_URL_TEMPLATE, host, port, database);
        try {
            Class.forName(JTDS_DRIVER);
        } catch (ClassNotFoundException e) {
            log.warn("1号机 T_Code 同步: jTDS 驱动未加载, 跳过本次");
            return;
        }

        // ---- 查询语句（源：1号机 SQL Server → T_Code） ----
        // 注意：不过滤 Status，同步全部 SerialNo > lastSynced 的记录
        String querySql = "SELECT TOP " + MAX_ROWS_PER_SYNC + " SerialNo, BagCode, BoxCode FROM " + tableName
                + " WHERE SerialNo > ? ORDER BY SerialNo ASC";
        log.debug("1号机 T_Code 同步 [查询] SQL: {} | 参数: SerialNo > {} | 目标: {}@{}:{}/{}",
                querySql, lastSynced, username, host, port, database);

        List<TCodeRow> rows = new ArrayList<>();
        long maxSerialNo = lastSynced;
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            log.debug("1号机 T_Code 同步 [查询] 连接成功: {}@{}:{}/{}", username, host, port, database);
            try (PreparedStatement ps = conn.prepareStatement(querySql)) {
                ps.setLong(1, lastSynced);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int sn = rs.getInt("SerialNo");
                        String bagCode = rs.getString("BagCode");
                        String boxCode = rs.getString("BoxCode");
                        if (sn > maxSerialNo) maxSerialNo = sn;
                        rows.add(new TCodeRow(sn, bagCode != null ? bagCode : "", boxCode != null ? boxCode : ""));
                    }
                }
            }
            log.info("1号机 T_Code 同步 [查询] 结果: 共读取 {} 条 (SerialNo > {}, 上限 {}条/批)",
                    rows.size(), lastSynced, MAX_ROWS_PER_SYNC);
            if (!rows.isEmpty()) {
                log.debug("1号机 T_Code 同步 [查询] SerialNo 范围: {} ~ {} | 首条样本: SerialNo={} BagCode={} BoxCode={}",
                        rows.get(0).serialNo, maxSerialNo,
                        rows.get(0).serialNo, rows.get(0).bagCode, rows.get(0).boxCode);
            }
        } catch (Exception e) {
            log.warn("1号机 T_Code 同步 [查询] 连接或查询失败: {}@{}:{}/{} — {}", username, host, port, database, e.getMessage());
            pushSyncEvent("SYNC_ERROR", "1号机同步失败：连接或查询失败 — " + e.getMessage());
            return;
        }

        if (rows.isEmpty()) {
            log.info("1号机 T_Code 同步 [查询] 暂无新数据 (SerialNo > {})，等待下次轮询", lastSynced);
            return;
        }

        // ---- 插入语句（目标：2号机 MySQL → CodeRelationUpload） ----
        // 字段映射：T_Code.BoxCode → MediumSerialNumber(箱码), T_Code.BagCode → SmallSerialNumber(盒码)
        // BigSerialNumber/OrderNo/ProductNO 留空，待盒箱关联成功后由 ShiwanM2BoxCaseService 回填
        // 重复判断规则：Medium+Small 均重复 → status=5（重码）；仅一个重复 → status=4（待剔除）；均不重复 → status=0
        String insertSql =
                "INSERT INTO CodeRelationUpload (" +
                        "BiggerSerialNumber, BigSerialNumber, MediumSerialNumber, SmallSerialNumber, " +
                        "DxCode, SalesCode, VirtualSerialNumber, IsVirtual, ProductNO, OrderNo, Status, TagNo, Qty, Type, " +
                        "WarehouseNo, BatchNo, AddTime, ErrCount, Msg, IsUpload, IsDel, TeamName) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        LocalDateTime now = LocalDateTime.now();
        String empty = "";
        String tagNo = "";
        int insertedCount = 0;
        int status4Count = 0;
        int status5Count = 0;

        // 判断每条记录的重复状态，决定插入时的 status
        List<TCodeRowWithStatus> rowsToInsert = new ArrayList<>();
        for (TCodeRow row : rows) {
            boolean mediumExists = mediumCodeExists(row.boxCode);
            boolean smallExists  = smallCodeExists(row.bagCode);
            int status;
            String msg;
            if (mediumExists && smallExists) {
                status = 5;
                msg = "同步重复码(盒码+瓶码均重复)";
                status5Count++;
            } else if (mediumExists) {
                status = 4;
                msg = "同步重复码(盒码重复)";
                status4Count++;
            } else if (smallExists) {
                status = 4;
                msg = "同步重复码(瓶码重复)";
                status4Count++;
            } else {
                status = 0;
                msg = empty;
            }
            rowsToInsert.add(new TCodeRowWithStatus(row, status, msg));
        }

        if (status4Count > 0 || status5Count > 0) {
            log.info("1号机 T_Code 同步 [重复处理] 本批检测到重复码：status=4({}) status=5({})",
                    status4Count, status5Count);
        }

        try {
            int[] results = jdbcTemplate.batchUpdate(insertSql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    TCodeRowWithStatus rws = rowsToInsert.get(i);
                    TCodeRow row = rws.row;
                    ps.setString(1, empty);          // BiggerSerialNumber（垛码，成垛时回填）
                    ps.setString(2, empty);          // BigSerialNumber（箱码，盒箱关联时回填）
                    ps.setString(3, row.boxCode);    // MediumSerialNumber ← T_Code.BoxCode（中层箱码）
                    ps.setString(4, row.bagCode);    // SmallSerialNumber  ← T_Code.BagCode（小层盒码）
                    ps.setString(5, empty);
                    ps.setString(6, empty);
                    ps.setString(7, empty);
                    ps.setInt(8, 0);
                    ps.setString(9, productNo);      // ProductNO（盒箱关联时回填）
                    ps.setString(10, orderNo);       // OrderNo（盒箱关联时回填）
                    ps.setInt(11, rws.status);       // status=0/4/5 按重复情况决定
                    ps.setString(12, tagNo);
                    ps.setInt(13, 0);
                    ps.setInt(14, 1);
                    ps.setString(15, empty);
                    ps.setString(16, empty);
                    ps.setObject(17, now);
                    ps.setInt(18, 0);
                    ps.setString(19, rws.msg);       // Msg 记录重复原因
                    ps.setInt(20, 0);
                    ps.setInt(21, 0);                // IsDel=0
                    ps.setString(22, empty);
                }

                @Override
                public int getBatchSize() {
                    return rowsToInsert.size();
                }
            });
            for (int r : results) {
                if (r > 0) insertedCount++;
            }
        } catch (Exception e) {
            log.warn("1号机 T_Code 同步 [插入] 批量写入失败，回退逐条写入: {}", e.getMessage());
            for (TCodeRowWithStatus rws : rowsToInsert) {
                TCodeRow row = rws.row;
                try {
                    int affected = jdbcTemplate.update(
                            insertSql,
                            empty, empty, row.boxCode, row.bagCode,
                            empty, empty, empty, 0, productNo, orderNo, rws.status, tagNo, 0, 1,
                            empty, empty, now, 0, rws.msg, 0, 0, empty
                    );
                    if (affected > 0) insertedCount++;
                } catch (Exception ex) {
                    log.warn("1号机 T_Code 同步 [插入] 单条写入失败 SerialNo={} BoxCode={} BagCode={}: {}",
                            row.serialNo, row.boxCode, row.bagCode, ex.getMessage());
                }
            }
        }

        M1SyncCursorStore.saveLastSyncedSerialNo(maxSerialNo);
        log.info("1号机 T_Code 同步 [结果] 本批读取 {} 条，写入 {} 条（含 status=4:{} status=5:{}），游标更新至 SerialNo={}",
                rows.size(), insertedCount, status4Count, status5Count, maxSerialNo);

        // 推送同步结果事件供前端轮询
        if (insertedCount > 0) {
            StringBuilder msg = new StringBuilder("本次成功同步了 ").append(insertedCount).append(" 条瓶盒数据");
            if (status4Count > 0) msg.append("，其中 ").append(status4Count).append(" 条标记为待剔除");
            if (status5Count > 0) msg.append("，其中 ").append(status5Count).append(" 条标记为重码");
            pushSyncEvent("SYNC_RESULT", msg.toString());
        }

        // 热表校验：仅对 status=0 的新插入数据校验（有问题则更新为 status=4）
        List<TCodeRow> normalRows = new ArrayList<>();
        for (TCodeRowWithStatus rws : rowsToInsert) {
            if (rws.status == 0) normalRows.add(rws.row);
        }
        List<TCodeRow> hotPassedRows = checkHotTableAndMarkStatus4(normalRows);
        checkColdTableAndMarkStatus4(hotPassedRows);
    }

    /** 盒码（MediumSerialNumber）是否已存在于 CodeRelationUpload（IsDel=0）。 */
    private boolean mediumCodeExists(String mediumCode) {
        if (mediumCode == null || mediumCode.trim().isEmpty()) return false;
        Long cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM CodeRelationUpload WHERE IsDel = 0 AND MediumSerialNumber = ?",
                Long.class, mediumCode);
        return cnt != null && cnt > 0;
    }

    /** 瓶码（SmallSerialNumber）是否已存在于 CodeRelationUpload（IsDel=0）。 */
    private boolean smallCodeExists(String smallCode) {
        if (smallCode == null || smallCode.trim().isEmpty()) return false;
        Long cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM CodeRelationUpload WHERE IsDel = 0 AND SmallSerialNumber = ?",
                Long.class, smallCode);
        return cnt != null && cnt > 0;
    }

    /**
     * 对本批同步的记录逐条检查码位数和热表：
     * 1. 若 BagCode 位数不符配置（smallCodeDigits != -1）或 BoxCode 位数不符配置（mediumCodeDigits != -1），
     * 2. 或 BagCode 不在 CodePackageItemHot(PackageType=1) 或 BoxCode 不在 CodePackageItemHot(PackageType=2)，
     * 则将 CodeRelationUpload 对应记录的 Status 更新为 4（待剔除），并在 Msg 字段记录原因。
     */
    private List<TCodeRow> checkHotTableAndMarkStatus4(List<TCodeRow> rows) {
        if (rows == null || rows.isEmpty()) return Collections.emptyList();

        ShiwanM2SettingsDto cfg = ShiwanM2SettingsFileLoader.load();
        int smallCodeDigits  = -1;
        int mediumCodeDigits = -1;
        if (cfg != null && cfg.getCodeDigits() != null) {
            smallCodeDigits  = cfg.getCodeDigits().getSmallCodeDigits();
            mediumCodeDigits = cfg.getCodeDigits().getMediumCodeDigits();
        }

        int markedCount = 0;
        List<TCodeRow> passedRows = new ArrayList<>();
        for (TCodeRow row : rows) {
            try {
                String rejectMsg = null;

                // 1. 码位数校验（-1 表示不校验）
                if (smallCodeDigits != -1 && row.bagCode != null
                        && row.bagCode.length() != smallCodeDigits) {
                    rejectMsg = "小标位数不匹配，应为" + smallCodeDigits + "位，实际" + row.bagCode.length() + "位";
                } else if (mediumCodeDigits != -1 && row.boxCode != null
                        && row.boxCode.length() != mediumCodeDigits) {
                    rejectMsg = "中标位数不匹配，应为" + mediumCodeDigits + "位，实际" + row.boxCode.length() + "位";
                }

                // 2. 热表校验（码位数已通过时才检查热表）
                if (rejectMsg == null) {
                    boolean bagInHot = isInHot(1, row.bagCode);
                    boolean boxInHot = isInHot(2, row.boxCode);
                    if (!bagInHot) {
                        rejectMsg = "瓶码不在码包热表（小标PackageType=1）";
                    } else if (!boxInHot) {
                        rejectMsg = "盒码不在码包热表（中标PackageType=2）";
                    }
                }

                if (rejectMsg != null) {
                    final String msg = rejectMsg;
                    int affected = jdbcTemplate.update(
                            "UPDATE CodeRelationUpload SET Status = 4, Msg = ? " +
                            "WHERE MediumSerialNumber = ? AND SmallSerialNumber = ? AND IsDel = 0 AND Status = 0",
                            msg, row.boxCode, row.bagCode);
                    if (affected > 0) {
                        markedCount++;
                        log.debug("1号机同步校验不通过：bagCode={} boxCode={} 原因：{} → Status=4",
                                row.bagCode, row.boxCode, msg);
                    }
                } else {
                    // 仅热表校验通过的数据继续进入冷表判重
                    passedRows.add(row);
                }
            } catch (Exception e) {
                log.warn("1号机同步校验时发生异常 SerialNo={}: {}", row.serialNo, e.getMessage());
            }
        }
        if (markedCount > 0) {
            log.info("1号机 T_Code 同步 [校验] 本批共将 {} 条记录置为 Status=4（待剔除）", markedCount);
        }
        return passedRows;
    }

    /**
     * 对热表校验通过的数据执行冷表判重：
     * 1) 先按盒码（PackageType=2）批量查询冷表；
     * 2) 盒码未命中的记录再按瓶码（PackageType=1）批量查询冷表；
     * 3) 命中任一冷表则置 Status=4。
     */
    private void checkColdTableAndMarkStatus4(List<TCodeRow> rows) {
        if (rows == null || rows.isEmpty()) return;

        Set<String> boxCodes = new LinkedHashSet<>();
        for (TCodeRow row : rows) {
            if (row != null && row.boxCode != null) {
                String v = row.boxCode.trim();
                if (!v.isEmpty()) boxCodes.add(v);
            }
        }
        Set<String> coldBoxCodes = batchQueryColdCodeValues(2, boxCodes);

        List<TCodeRow> needCheckBagRows = new ArrayList<>();
        for (TCodeRow row : rows) {
            String box = row == null || row.boxCode == null ? "" : row.boxCode.trim();
            if (!coldBoxCodes.contains(box)) {
                needCheckBagRows.add(row);
            }
        }

        Set<String> bagCodes = new LinkedHashSet<>();
        for (TCodeRow row : needCheckBagRows) {
            if (row != null && row.bagCode != null) {
                String v = row.bagCode.trim();
                if (!v.isEmpty()) bagCodes.add(v);
            }
        }
        Set<String> coldBagCodes = batchQueryColdCodeValues(1, bagCodes);

        int markedCount = 0;
        for (TCodeRow row : rows) {
            String box = row == null || row.boxCode == null ? "" : row.boxCode.trim();
            String bag = row == null || row.bagCode == null ? "" : row.bagCode.trim();
            String rejectMsg = null;
            if (!box.isEmpty() && coldBoxCodes.contains(box)) {
                rejectMsg = "盒码已在冷表中（中标PackageType=2）";
            } else if (!bag.isEmpty() && coldBagCodes.contains(bag)) {
                rejectMsg = "瓶码已在冷表中（小标PackageType=1）";
            }
            if (rejectMsg != null) {
                int affected = jdbcTemplate.update(
                        "UPDATE CodeRelationUpload SET Status = 4, Msg = ? " +
                        "WHERE MediumSerialNumber = ? AND SmallSerialNumber = ? AND IsDel = 0 AND Status = 0",
                        rejectMsg, row.boxCode, row.bagCode);
                if (affected > 0) {
                    markedCount++;
                    log.debug("1号机同步冷表判重命中：bagCode={} boxCode={} 原因：{} → Status=4",
                            row.bagCode, row.boxCode, rejectMsg);
                }
            }
        }
        if (markedCount > 0) {
            log.info("1号机 T_Code 同步 [冷表校验] 本批共将 {} 条记录置为 Status=4", markedCount);
        }
    }

    /** 批量查询冷表命中的码值集合（分批 IN，避免 SQL 过长）。 */
    private Set<String> batchQueryColdCodeValues(int packageType, Set<String> codeValues) {
        if (codeValues == null || codeValues.isEmpty()) {
            return Collections.emptySet();
        }
        final int batchSize = 200;
        List<String> all = new ArrayList<>(codeValues);
        Set<String> found = new HashSet<>();
        for (int i = 0; i < all.size(); i += batchSize) {
            int end = Math.min(i + batchSize, all.size());
            List<String> batch = all.subList(i, end);
            String placeholders = String.join(",", Collections.nCopies(batch.size(), "?"));
            List<Object> args = new ArrayList<>();
            args.add(packageType);
            args.addAll(batch);
            List<String> part = jdbcTemplate.queryForList(
                    "SELECT CodeValue FROM CodePackageItemCold WHERE PackageType = ? AND CodeValue IN (" + placeholders + ")",
                    String.class, args.toArray());
            if (part != null && !part.isEmpty()) {
                found.addAll(part);
            }
        }
        return found;
    }

    private boolean isInHot(int packageType, String codeValue) {
        if (codeValue == null || codeValue.trim().isEmpty()) return false;
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM CodePackageItemHot WHERE PackageType = ? AND CodeValue = ?",
                Integer.class, packageType, codeValue.trim());
        return cnt != null && cnt > 0;
    }

    private CurrentTask getCurrentTask() {
        try {
            List<Map<String, Object>> list = jdbcTemplate.queryForList(
                    "SELECT po.OrderNo, pod.ProductNO FROM ProductionOrder po " +
                            "LEFT JOIN ProductionOrderDetail pod ON po.Id = pod.OrderId AND (pod.IsDel = 0 OR pod.IsDel IS NULL) " +
                            "WHERE po.OrderStatus = 1 ORDER BY po.Id DESC LIMIT 1"
            );
            if (list != null && !list.isEmpty()) {
                Map<String, Object> row = list.get(0);
                Object on = row.get("OrderNo");
                Object pn = row.get("ProductNO");
                return new CurrentTask(on != null ? on.toString() : "", pn != null ? pn.toString() : "");
            }
        } catch (Exception e) {
            log.trace("查询当前 2 号机任务失败: {}", e.getMessage());
        }
        return null;
    }

    private static class CurrentTask {
        final String orderNo;
        final String productNo;
        CurrentTask(String orderNo, String productNo) {
            this.orderNo = orderNo;
            this.productNo = productNo;
        }
    }

    /** 推送一条同步事件到队列（线程安全） */
    private synchronized void pushSyncEvent(String type, String message) {
        long seq = syncEventSeq.incrementAndGet();
        Map<String, Object> evt = new LinkedHashMap<>();
        evt.put("seq", seq);
        evt.put("type", type);
        evt.put("message", message);
        evt.put("time", LocalDateTime.now().format(TIME_FMT));
        if (syncEventQueue.size() >= MAX_SYNC_EVENTS) syncEventQueue.pollFirst();
        syncEventQueue.addLast(evt);
    }

    /** 返回 seq > lastSeq 的同步事件列表，供前端轮询（线程安全） */
    public synchronized List<Map<String, Object>> pollSyncEvents(long lastSeq) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> evt : syncEventQueue) {
            Object seqObj = evt.get("seq");
            if (seqObj instanceof Number && ((Number) seqObj).longValue() > lastSeq) {
                result.add(evt);
            }
        }
        return result;
    }

    private static class TCodeRow {
        final int serialNo;
        final String bagCode;
        final String boxCode;
        TCodeRow(int serialNo, String bagCode, String boxCode) {
            this.serialNo = serialNo;
            this.bagCode = bagCode;
            this.boxCode = boxCode;
        }
    }

    private static class TCodeRowWithStatus {
        final TCodeRow row;
        final int status;
        final String msg;
        TCodeRowWithStatus(TCodeRow row, int status, String msg) {
            this.row = row;
            this.status = status;
            this.msg = msg;
        }
    }
}
