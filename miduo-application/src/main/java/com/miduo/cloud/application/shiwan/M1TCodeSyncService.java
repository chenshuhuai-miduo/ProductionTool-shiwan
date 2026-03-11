package com.miduo.cloud.application.shiwan;

import com.miduo.cloud.common.config.M1SyncCursorStore;
import com.miduo.cloud.common.config.ShiwanM2SettingsDto;
import com.miduo.cloud.common.config.ShiwanM2SettingsFileLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 1 号机 T_Code 表定时同步到 2 号机 CodeRelationUpload。
 * 每 1 秒执行一次：从 1 号机 SQL Server 读取 SerialNo &gt; lastSynced 且 Status=0 的记录，
 * 插入 2 号机 MySQL CodeRelationUpload，OrderNo/ProductNO 取当前 2 号机生产订单号与产品编码，AddTime 取当前时间。
 * 启用条件：shiwan.m2.m1-sync.enabled=true
 */
@Service
@ConditionalOnProperty(name = "shiwan.m2.m1-sync.enabled", havingValue = "true")
public class M1TCodeSyncService {

    private static final Logger log = LoggerFactory.getLogger(M1TCodeSyncService.class);
    private static final String JTDS_DRIVER = "net.sourceforge.jtds.jdbc.Driver";
    private static final String JTDS_URL_TEMPLATE = "jdbc:jtds:sqlserver://%s:%s/%s;loginTimeout=3;socketTimeout=3000";

    private final JdbcTemplate jdbcTemplate;

    public M1TCodeSyncService(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Scheduled(fixedRate = 1000)
    public void sync() {
        ShiwanM2SettingsDto config = ShiwanM2SettingsFileLoader.load();
        if (config == null || config.getM1DbConnection() == null) {
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
            return;
        }

        Long lastSynced = M1SyncCursorStore.loadLastSyncedSerialNo();
        if (lastSynced == null) lastSynced = 0L;

        CurrentTask currentTask = getCurrentTask();
        String orderNo = currentTask != null ? currentTask.orderNo : "";
        String productNo = currentTask != null ? currentTask.productNo : "";

        String jdbcUrl = String.format(JTDS_URL_TEMPLATE, host, port, database);
        try {
            Class.forName(JTDS_DRIVER);
        } catch (ClassNotFoundException e) {
            log.warn("1号机 T_Code 同步: jTDS 驱动未加载, 跳过本次");
            return;
        }

        List<TCodeRow> rows = new ArrayList<>();
        long maxSerialNo = lastSynced;
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            String sql = "SELECT SerialNo, BagCode, BoxCode FROM " + tableName + " WHERE SerialNo > ? AND (Status = 0 OR Status = '0') ORDER BY SerialNo ASC";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
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
        } catch (Exception e) {
            log.debug("1号机 T_Code 查询失败: {}", e.getMessage());
            return;
        }

        if (rows.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        String empty = "";
        String tagNo = "M1-SYNC";
        for (TCodeRow row : rows) {
            try {
                jdbcTemplate.update(
                        "INSERT INTO CodeRelationUpload (" +
                                "BiggerSerialNumber, BigSerialNumber, MediumSerialNumber, SmallSerialNumber, " +
                                "DxCode, SalesCode, VirtualSerialNumber, IsVirtual, ProductNO, OrderNo, Status, TagNo, Qty, Type, " +
                                "WarehouseNo, BatchNo, AddTime, ErrCount, Msg, IsUpload, IsDel, TeamName) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        empty, empty, row.boxCode, row.bagCode,
                        empty, empty, empty, 0, productNo, orderNo, 0, tagNo, 0, 1,
                        empty, empty, now, 0, empty, 1, 0, empty
                );
            } catch (Exception e) {
                log.warn("1号机 T_Code 同步写入 CodeRelationUpload 失败 SerialNo={}: {}", row.serialNo, e.getMessage());
            }
        }

        M1SyncCursorStore.saveLastSyncedSerialNo(maxSerialNo);
        if (log.isDebugEnabled() && !rows.isEmpty()) {
            log.debug("1号机 T_Code 同步: 写入 {} 条, lastSyncedSerialNo={}", rows.size(), maxSerialNo);
        }
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
