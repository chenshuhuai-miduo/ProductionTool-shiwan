package com.miduo.cloud.config;

import com.miduo.cloud.common.config.ShiwanM2SettingsDto;
import com.miduo.cloud.common.config.ShiwanM2SettingsFileLoader;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

/**
 * 石湾 2 号机：使用 shiwan-m2-settings.json 中的数据库连接配置作为 DataSource。
 * 启用方式：配置 shiwan.m2.datasource.enabled=true（如 application-shiwan-m2.properties 或启动参数）。
 * 启用后创建的 DataSource 为 @Primary，供 MyBatis 等使用。
 */
@Configuration
@ConditionalOnProperty(name = "shiwan.m2.datasource.enabled", havingValue = "true")
public class ShiwanM2DataSourceConfig {

    // characterEncoding 必须用 Java charset 名称（UTF-8），而非 MySQL 的 utf8mb4。
    // 服务端字符集由 MySQL 自身配置决定；驱动层 UTF-8 已可正确传输全 Unicode。
    private static final String JDBC_URL_TEMPLATE =
            "jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=UTF-8";

    @Bean
    @Primary
    public DataSource shiwanM2DataSource(Environment env) {
        ShiwanM2SettingsDto dto = ShiwanM2SettingsFileLoader.load();
        HikariConfig config = new HikariConfig();
        if (dto != null && dto.getDbConnection() != null) {
            ShiwanM2SettingsDto.DbConnection c = dto.getDbConnection();
            String host = c.getHost() != null ? c.getHost().trim() : "127.0.0.1";
            String port = c.getPort() != null ? c.getPort().trim() : "3306";
            String database = c.getDatabase() != null ? c.getDatabase().trim() : "";
            String username = c.getUsername() != null ? c.getUsername().trim() : "root";
            String password = c.getPassword() != null ? c.getPassword() : "";

            config.setJdbcUrl(String.format(JDBC_URL_TEMPLATE, host, port, database));
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            // 配置文件缺失或无 dbConnection 时，回退到 spring.datasource.*，避免启动阶段硬失败
            config.setJdbcUrl(env.getProperty(
                    "spring.datasource.url",
                    "jdbc:mysql://127.0.0.1:3306/mysql?useSSL=false&serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=UTF-8"));
            config.setUsername(env.getProperty("spring.datasource.username", "root"));
            config.setPassword(env.getProperty("spring.datasource.password", ""));
            config.setDriverClassName(env.getProperty("spring.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver"));
        }
        config.setConnectionTestQuery("SELECT 1");
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        // 启动时不强制建立连接：-1 表示连接失败时不抛出异常，连接池后台持续重试
        // 主界面打开后由前端主动触发 check-db-connection 端点，将结果显示在操作日志中
        config.setInitializationFailTimeout(-1);
        return new HikariDataSource(config);
    }
}
