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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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
    /** 每轮最多同步记录数，避免单次任务占用连接过久。 */
    private static final int MAX_ROWS_PER_SYNC = 300;

    /** 运行时开关：只有 true 时才执行同步逻辑 */
    private volatile boolean syncActive = false;
    /** 防重入：上一轮未完成时跳过下一轮触发。 */
    private final AtomicBoolean syncRunning = new AtomicBoolean(false);

    private final JdbcTemplate jdbcTemplate;

    public M1TCodeSyncService(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /** 前端开始采集后调用：激活同步 */
    public void startSync() {
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
        log.info("1号机 T_Code 同步 [查询] SQL: {} | 参数: SerialNo > {} | 目标: {}@{}:{}/{}",
                querySql, lastSynced, username, host, port, database);

        List<TCodeRow> rows = new ArrayList<>();
        long maxSerialNo = lastSynced;
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            log.info("1号机 T_Code 同步 [查询] 连接成功: {}@{}:{}/{}", username, host, port, database);
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
                log.info("1号机 T_Code 同步 [查询] SerialNo 范围: {} ~ {} | 首条样本: SerialNo={} BagCode={} BoxCode={}",
                        rows.get(0).serialNo, maxSerialNo,
                        rows.get(0).serialNo, rows.get(0).bagCode, rows.get(0).boxCode);
            }
        } catch (Exception e) {
            log.warn("1号机 T_Code 同步 [查询] 连接或查询失败: {}@{}:{}/{} — {}", username, host, port, database, e.getMessage());
            return;
        }

        if (rows.isEmpty()) {
            log.info("1号机 T_Code 同步 [查询] 暂无新数据 (SerialNo > {})，等待下次轮询", lastSynced);
            return;
        }

        // ---- 插入语句（目标：2号机 MySQL → CodeRelationUpload） ----
        // 字段映射：T_Code.BoxCode → MediumSerialNumber(箱码), T_Code.BagCode → SmallSerialNumber(盒码)
        // BigSerialNumber/OrderNo/ProductNO 留空，待盒箱关联成功后由 ShiwanM2BoxCaseService 回填
        String insertSql =
                "INSERT INTO CodeRelationUpload (" +
                        "BiggerSerialNumber, BigSerialNumber, MediumSerialNumber, SmallSerialNumber, " +
                        "DxCode, SalesCode, VirtualSerialNumber, IsVirtual, ProductNO, OrderNo, Status, TagNo, Qty, Type, " +
                        "WarehouseNo, BatchNo, AddTime, ErrCount, Msg, IsUpload, IsDel, TeamName) " +
                        "SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? " +
                        "FROM DUAL " +
                        "WHERE NOT EXISTS (" +
                        "SELECT 1 FROM CodeRelationUpload " +
                        "WHERE MediumSerialNumber = ? AND SmallSerialNumber = ? AND IsDel = 0)";
        log.info("1号机 T_Code 同步 [插入] SQL: {} | 待写入 {} 条到 CodeRelationUpload (去重: MediumSerialNumber+SmallSerialNumber)",
                insertSql, rows.size());

        LocalDateTime now = LocalDateTime.now();
        String empty = "";
        String tagNo = "";
        int insertedCount = 0;
        int skippedCount = 0;
        try {
            int[] results = jdbcTemplate.batchUpdate(insertSql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    TCodeRow row = rows.get(i);
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
                    ps.setInt(11, 0);
                    ps.setString(12, tagNo);
                    ps.setInt(13, 0);
                    ps.setInt(14, 1);
                    ps.setString(15, empty);
                    ps.setString(16, empty);
                    ps.setObject(17, now);
                    ps.setInt(18, 0);
                    ps.setString(19, empty);
                    ps.setInt(20, 0);
                    ps.setInt(21, 0);                // IsDel=0
                    ps.setString(22, empty);
                    ps.setString(23, row.boxCode);   // WHERE NOT EXISTS: MediumSerialNumber
                    ps.setString(24, row.bagCode);   // WHERE NOT EXISTS: SmallSerialNumber
                }

                @Override
                public int getBatchSize() {
                    return rows.size();
                }
            });
            for (int r : results) {
                if (r > 0) insertedCount++;
                else skippedCount++;
            }
        } catch (Exception e) {
            log.warn("1号机 T_Code 同步 [插入] 批量写入失败，回退逐条写入: {}", e.getMessage());
            for (TCodeRow row : rows) {
                try {
                    int affected = jdbcTemplate.update(
                            insertSql,
                            empty, empty, row.boxCode, row.bagCode,
                            empty, empty, empty, 0, productNo, orderNo, 0, tagNo, 0, 1,
                            empty, empty, now, 0, empty, 1, 0, empty,
                            row.boxCode, row.bagCode
                    );
                    if (affected > 0) insertedCount++;
                    else skippedCount++;
                } catch (Exception ex) {
                    log.warn("1号机 T_Code 同步 [插入] 单条写入失败 SerialNo={} BoxCode={} BagCode={}: {}",
                            row.serialNo, row.boxCode, row.bagCode, ex.getMessage());
                    skippedCount++;
                }
            }
        }

        M1SyncCursorStore.saveLastSyncedSerialNo(maxSerialNo);
        log.info("1号机 T_Code 同步 [结果] 本批读取 {} 条，新增写入 {} 条，重复跳过 {} 条，游标更新至 SerialNo={}",
                rows.size(), insertedCount, skippedCount, maxSerialNo);
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
}
