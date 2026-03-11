package com.miduo.cloud.controller;

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
}
