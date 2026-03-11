package com.miduo.cloud.common.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 石湾 2 号机系统设置 JSON 的 DTO（后端读取 shiwan-m2-settings.json 用）。
 * 与前端 ShiwanM2Settings 结构兼容，仅包含后端需要的字段。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShiwanM2SettingsDto {

    private DbConnection dbConnection;
    private PalletRule palletRule;

    public DbConnection getDbConnection() { return dbConnection; }
    public void setDbConnection(DbConnection dbConnection) { this.dbConnection = dbConnection; }
    public PalletRule getPalletRule() { return palletRule; }
    public void setPalletRule(PalletRule palletRule) { this.palletRule = palletRule; }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DbConnection {
        private String host;
        private String port;
        private String database;
        private String username;
        private String password;
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public String getPort() { return port; }
        public void setPort(String port) { this.port = port; }
        public String getDatabase() { return database; }
        public void setDatabase(String database) { this.database = database; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PalletRule {
        private String prefix;
        private String lineCode;
        public String getPrefix() { return prefix; }
        public void setPrefix(String prefix) { this.prefix = prefix; }
        public String getLineCode() { return lineCode; }
        public void setLineCode(String lineCode) { this.lineCode = lineCode; }
    }
}
