package com.miduo.cloud.config;

import com.miduo.cloud.common.config.ShiwanM2SettingsDto;
import com.miduo.cloud.common.config.ShiwanM2SettingsFileLoader;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * 石湾 2 号机：使用 shiwan-m2-settings.json 中的数据库连接配置作为 DataSource。
 * 启用方式：配置 shiwan.m2.datasource.enabled=true（如 application-shiwan-m2.properties 或启动参数）。
 * 启用后创建的 DataSource 为 @Primary，供 MyBatis 等使用。
 */
@Configuration
@ConditionalOnProperty(name = "shiwan.m2.datasource.enabled", havingValue = "true")
public class ShiwanM2DataSourceConfig {

    private static final String JDBC_URL_TEMPLATE = "jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8";

    @Bean
    @Primary
    public DataSource shiwanM2DataSource() {
        ShiwanM2SettingsDto dto = ShiwanM2SettingsFileLoader.load();
        if (dto == null || dto.getDbConnection() == null) {
            throw new IllegalStateException("shiwan.m2.datasource.enabled=true 但未找到或无法解析 shiwan-m2-settings.json 中的 dbConnection 配置");
        }
        ShiwanM2SettingsDto.DbConnection c = dto.getDbConnection();
        String host = c.getHost() != null ? c.getHost().trim() : "127.0.0.1";
        String port = c.getPort() != null ? c.getPort().trim() : "3306";
        String database = c.getDatabase() != null ? c.getDatabase().trim() : "";
        String username = c.getUsername() != null ? c.getUsername().trim() : "root";
        String password = c.getPassword() != null ? c.getPassword() : "";

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format(JDBC_URL_TEMPLATE, host, port, database));
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setConnectionTestQuery("SELECT 1");
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        return new HikariDataSource(config);
    }
}
