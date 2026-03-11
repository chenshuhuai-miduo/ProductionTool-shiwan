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
    private M1DbConnection m1DbConnection;
    private Long lastSyncedM1SerialNo;
    private PalletRule palletRule;
    private UploadConfig upload;
    private ApiConfig api;

    public DbConnection getDbConnection() { return dbConnection; }
    public void setDbConnection(DbConnection dbConnection) { this.dbConnection = dbConnection; }
    public M1DbConnection getM1DbConnection() { return m1DbConnection; }
    public void setM1DbConnection(M1DbConnection m1DbConnection) { this.m1DbConnection = m1DbConnection; }
    public Long getLastSyncedM1SerialNo() { return lastSyncedM1SerialNo; }
    public void setLastSyncedM1SerialNo(Long lastSyncedM1SerialNo) { this.lastSyncedM1SerialNo = lastSyncedM1SerialNo; }
    public PalletRule getPalletRule() { return palletRule; }
    public void setPalletRule(PalletRule palletRule) { this.palletRule = palletRule; }
    public UploadConfig getUpload() { return upload; }
    public void setUpload(UploadConfig upload) { this.upload = upload; }
    public ApiConfig getApi() { return api; }
    public void setApi(ApiConfig api) { this.api = api; }

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
    public static class M1DbConnection {
        private String host;
        private String port;
        private String database;
        private String tableName;
        private String username;
        private String password;
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public String getPort() { return port; }
        public void setPort(String port) { this.port = port; }
        public String getDatabase() { return database; }
        public void setDatabase(String database) { this.database = database; }
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UploadConfig {
        private boolean autoUpload = true;
        public boolean isAutoUpload() { return autoUpload; }
        public void setAutoUpload(boolean autoUpload) { this.autoUpload = autoUpload; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApiConfig {
        private String baseUrl;
        private String appKey;
        private String appSecret;
        private String syncCodeAndVirtualRelationPath;
        private String getSyncResultPath;
        private String codePackageQueryCompletedPath;
        private String productsListPath;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getAppKey() { return appKey; }
        public void setAppKey(String appKey) { this.appKey = appKey; }
        public String getAppSecret() { return appSecret; }
        public void setAppSecret(String appSecret) { this.appSecret = appSecret; }
        public String getSyncCodeAndVirtualRelationPath() { return syncCodeAndVirtualRelationPath; }
        public void setSyncCodeAndVirtualRelationPath(String syncCodeAndVirtualRelationPath) { this.syncCodeAndVirtualRelationPath = syncCodeAndVirtualRelationPath; }
        public String getGetSyncResultPath() { return getSyncResultPath; }
        public void setGetSyncResultPath(String getSyncResultPath) { this.getSyncResultPath = getSyncResultPath; }
        public String getCodePackageQueryCompletedPath() { return codePackageQueryCompletedPath; }
        public void setCodePackageQueryCompletedPath(String codePackageQueryCompletedPath) { this.codePackageQueryCompletedPath = codePackageQueryCompletedPath; }
        public String getProductsListPath() { return productsListPath; }
        public void setProductsListPath(String productsListPath) { this.productsListPath = productsListPath; }
    }
}
