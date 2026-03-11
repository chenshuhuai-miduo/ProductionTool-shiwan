package com.miduo.cloud.controller;

import com.miduo.cloud.common.config.ShiwanM2SettingsDto;
import com.miduo.cloud.common.config.ShiwanM2SettingsFileLoader;
import com.miduo.cloud.common.dto.ApiResult;
import org.springframework.web.bind.annotation.*;

import java.sql.DriverManager;
import java.util.Map;

/**
 * 石湾 2 号机系统设置相关接口（P03-02）。
 * 提供数据库连接测试等，供前端系统设置弹窗调用。
 */
@RestController
@RequestMapping("/api/shiwan-m2/settings")
@CrossOrigin
public class ShiwanM2SettingsController {

    private static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String JDBC_URL_TEMPLATE = "jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=Asia/Shanghai&connectTimeout=5000&socketTimeout=5000";

    /** 1 号机 SQL Server（jTDS） */
    private static final String JTDS_DRIVER = "net.sourceforge.jtds.jdbc.Driver";
    private static final String JTDS_URL_TEMPLATE = "jdbc:jtds:sqlserver://%s:%s/%s;loginTimeout=5;socketTimeout=5000";

    /**
     * 测试数据库连接（2 号机本地 MySQL）。
     * POST /api/shiwan-m2/settings/test-db-connection
     * Body: { "host": "", "port": "3306", "database": "", "username": "", "password": "" }
     */
    @PostMapping("/test-db-connection")
    public ApiResult<Boolean> testDbConnection(@RequestBody Map<String, String> body) {
        String host = body != null ? body.get("host") : null;
        String port = body != null ? body.get("port") : "3306";
        String database = body != null ? body.get("database") : null;
        String username = body != null ? body.get("username") : null;
        String password = body != null ? body.get("password") : null;
        if (host == null || host.isEmpty() || database == null || database.isEmpty() || username == null || username.isEmpty()) {
            return ApiResult.error("缺少必填项：host、database、username");
        }
        if (port == null || port.isEmpty()) port = "3306";
        if (password == null) password = "";
        String url = String.format(JDBC_URL_TEMPLATE, host.trim(), port.trim(), database.trim());
        try {
            Class.forName(MYSQL_DRIVER);
        } catch (ClassNotFoundException e) {
            return ApiResult.error("MySQL 驱动未加载，请确认依赖已引入");
        }
        try (java.sql.Connection conn = DriverManager.getConnection(url, username.trim(), password)) {
            if (conn.isValid(3)) {
                return ApiResult.success("连接成功", true);
            }
        } catch (Exception e) {
            return ApiResult.error("连接失败：" + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
        return ApiResult.error("连接失败");
    }

    /**
     * 测试 1 号机数据库连接（SQL Server，用于 T_Code 同步）。
     * POST /api/shiwan-m2/settings/test-m1-db-connection
     * Body: { "host": "", "port": "1433", "database": "", "username": "", "password": "" }
     */
    @PostMapping("/test-m1-db-connection")
    public ApiResult<Boolean> testM1DbConnection(@RequestBody Map<String, String> body) {
        String host = body != null ? body.get("host") : null;
        String port = body != null ? body.get("port") : "1433";
        String database = body != null ? body.get("database") : null;
        String username = body != null ? body.get("username") : null;
        String password = body != null ? body.get("password") : null;
        if (host == null || host.isEmpty() || database == null || database.isEmpty() || username == null || username.isEmpty()) {
            return ApiResult.error("缺少必填项：host、database、username");
        }
        if (port == null || port.isEmpty()) port = "1433";
        if (password == null) password = "";
        String url = String.format(JTDS_URL_TEMPLATE, host.trim(), port.trim(), database.trim());
        try {
            Class.forName(JTDS_DRIVER);
        } catch (ClassNotFoundException e) {
            return ApiResult.error("SQL Server 驱动(jTDS)未加载，请确认已引入 jtds 依赖");
        }
        try (java.sql.Connection conn = DriverManager.getConnection(url, username.trim(), password)) {
            if (conn.isValid(3)) {
                return ApiResult.success("连接成功", true);
            }
        } catch (Exception e) {
            return ApiResult.error("连接失败：" + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
        return ApiResult.error("连接失败");
    }

    /**
     * 门禁用：根据已保存的配置检测 1 号机 SQL Server 是否可连接。
     * GET /api/shiwan-m2/settings/check-m1-connection
     * 读取 shiwan-m2-settings.json 中的 m1DbConnection，用与 test-m1-db-connection 相同的 JDBC 逻辑检测。
     */
    @GetMapping("/check-m1-connection")
    public ApiResult<Boolean> checkM1Connection() {
        ShiwanM2SettingsDto config = ShiwanM2SettingsFileLoader.load();
        if (config == null || config.getM1DbConnection() == null) {
            return ApiResult.error(400, "未配置 1 号机连接，请在系统设置中配置");
        }
        ShiwanM2SettingsDto.M1DbConnection m1 = config.getM1DbConnection();
        String host = m1.getHost();
        String port = m1.getPort() != null && !m1.getPort().isEmpty() ? m1.getPort() : "1433";
        String database = m1.getDatabase();
        String username = m1.getUsername();
        String password = m1.getPassword() != null ? m1.getPassword() : "";
        if (host == null || host.isEmpty() || database == null || database.isEmpty() || username == null || username.isEmpty()) {
            return ApiResult.error(400, "1 号机连接配置不完整，请在系统设置中填写 host、database、username");
        }
        String url = String.format(JTDS_URL_TEMPLATE, host.trim(), port.trim(), database.trim());
        try {
            Class.forName(JTDS_DRIVER);
        } catch (ClassNotFoundException e) {
            return ApiResult.error("SQL Server 驱动(jTDS)未加载，请确认已引入 jtds 依赖");
        }
        try (java.sql.Connection conn = DriverManager.getConnection(url, username.trim(), password)) {
            if (conn.isValid(3)) {
                return ApiResult.success("1 号机连接正常", true);
            }
        } catch (Exception e) {
            return ApiResult.error("1 号机连接失败：" + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
        return ApiResult.error("1 号机连接失败");
    }
}
