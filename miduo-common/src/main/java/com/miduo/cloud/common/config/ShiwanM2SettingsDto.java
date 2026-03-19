package com.miduo.cloud.common.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonAlias;

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
    /** 接口配置：baseUrl、appId、appSecret、productsListPath 等（与前端 api 同结构） */
    private Api api;

    public DbConnection getDbConnection() { return dbConnection; }
    public void setDbConnection(DbConnection dbConnection) { this.dbConnection = dbConnection; }
    public M1DbConnection getM1DbConnection() { return m1DbConnection; }
    public void setM1DbConnection(M1DbConnection m1DbConnection) { this.m1DbConnection = m1DbConnection; }
    public Long getLastSyncedM1SerialNo() { return lastSyncedM1SerialNo; }
    public void setLastSyncedM1SerialNo(Long lastSyncedM1SerialNo) { this.lastSyncedM1SerialNo = lastSyncedM1SerialNo; }
    public PalletRule getPalletRule() { return palletRule; }
    public void setPalletRule(PalletRule palletRule) { this.palletRule = palletRule; }
    public Api getApi() { return api; }
    public void setApi(Api api) { this.api = api; }

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
        private Long initialSerialNo;
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
        public Long getInitialSerialNo() { return initialSerialNo; }
        public void setInitialSerialNo(Long initialSerialNo) { this.initialSerialNo = initialSerialNo; }
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

    /** 入库仓库编号（上传垛码关联数据时作为 warehouseno 入参，默认 001） */
    private String warehouseNo = "001";
    public String getWarehouseNo() { return warehouseNo; }
    public void setWarehouseNo(String warehouseNo) { this.warehouseNo = warehouseNo; }

    /** 码位数配置（与前端 ShiwanM2Settings.CodeDigitsConfig 同结构） */
    private CodeDigitsConfig codeDigits;
    public CodeDigitsConfig getCodeDigits() { return codeDigits; }
    public void setCodeDigits(CodeDigitsConfig codeDigits) { this.codeDigits = codeDigits; }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CodeDigitsConfig {
        /** 瓶码（小码）位数，-1 表示不限，默认 14 */
        private int smallCodeDigits = 14;
        /** 盒码（中码）位数，-1 表示不限，默认 14 */
        private int mediumCodeDigits = 14;
        /** 箱码（大码）位数，-1 表示不限，默认 -1 */
        private int largeCodeDigits = -1;
        public int getSmallCodeDigits() { return smallCodeDigits; }
        public void setSmallCodeDigits(int smallCodeDigits) { this.smallCodeDigits = smallCodeDigits; }
        public int getMediumCodeDigits() { return mediumCodeDigits; }
        public void setMediumCodeDigits(int mediumCodeDigits) { this.mediumCodeDigits = mediumCodeDigits; }
        public int getLargeCodeDigits() { return largeCodeDigits; }
        public void setLargeCodeDigits(int largeCodeDigits) { this.largeCodeDigits = largeCodeDigits; }
    }

    /** 上传配置（对应前端 ShiwanM2Settings.UploadConfig） */
    private Upload upload;
    public Upload getUpload() { return upload; }
    public void setUpload(Upload upload) { this.upload = upload; }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Upload {
        /** 自动上传开关：默认 true（成垛后自动上传至开放平台） */
        private boolean autoUpload = true;
        public boolean isAutoUpload() { return autoUpload; }
        public void setAutoUpload(boolean autoUpload) { this.autoUpload = autoUpload; }
    }

    /** 接口配置（后端读 shiwan-m2-settings.json 中的 api 节点，用于产品同步等） */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Api {
        private String baseUrl;
        private String backendBaseUrl;
        @JsonAlias("app_id")
        private String appId;
        private String appKey;  // 与前端兼容，部分配置写 appKey
        @JsonAlias({"app_secret", "appSecert"})
        private String appSecret;
        /** 开放平台登录账号（Memberlogin 字段值，用于码替换等接口请求体） */
        private String memberlogin;
        private String syncCodeAndVirtualRelationPath;
        private String getSyncResultPath;
        private String productsListPath;
        private String codePackageQueryPath;
        /** 码替换接口路径 */
        private String codeSubstitutionPath;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getBackendBaseUrl() { return backendBaseUrl; }
        public void setBackendBaseUrl(String backendBaseUrl) { this.backendBaseUrl = backendBaseUrl; }
        /** 优先返回 appId，否则 appKey（与前端配置兼容） */
        public String getAppId() { return appId != null && !appId.isEmpty() ? appId : appKey; }
        public void setAppId(String appId) { this.appId = appId; }
        public String getAppKey() { return appKey; }
        public void setAppKey(String appKey) { this.appKey = appKey; }
        public String getAppSecret() { return appSecret; }
        public void setAppSecret(String appSecret) { this.appSecret = appSecret; }
        public String getMemberlogin() { return memberlogin; }
        public void setMemberlogin(String memberlogin) { this.memberlogin = memberlogin; }
        public String getSyncCodeAndVirtualRelationPath() { return syncCodeAndVirtualRelationPath; }
        public void setSyncCodeAndVirtualRelationPath(String syncCodeAndVirtualRelationPath) {
            this.syncCodeAndVirtualRelationPath = syncCodeAndVirtualRelationPath;
        }
        public String getGetSyncResultPath() { return getSyncResultPath; }
        public void setGetSyncResultPath(String getSyncResultPath) { this.getSyncResultPath = getSyncResultPath; }
        public String getProductsListPath() { return productsListPath; }
        public void setProductsListPath(String productsListPath) { this.productsListPath = productsListPath; }
        public String getCodePackageQueryPath() { return codePackageQueryPath; }
        public void setCodePackageQueryPath(String codePackageQueryPath) { this.codePackageQueryPath = codePackageQueryPath; }
        public String getCodeSubstitutionPath() { return codeSubstitutionPath; }
        public void setCodeSubstitutionPath(String codeSubstitutionPath) { this.codeSubstitutionPath = codeSubstitutionPath; }
    }
}
