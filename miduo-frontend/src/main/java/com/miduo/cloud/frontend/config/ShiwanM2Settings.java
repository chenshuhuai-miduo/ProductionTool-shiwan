package com.miduo.cloud.frontend.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 石湾 2 号机系统设置 JSON 配置模型。
 * 对应 P03-02-系统设置，持久化到 shiwan-m2-settings.json。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShiwanM2Settings {

    /** 系统设置访问密码（默认 123456） */
    private String systemSettingsPassword = "123456";

    /** 业务：码位数配置 */
    private CodeDigitsConfig codeDigits = new CodeDigitsConfig();

    /** 业务：虚拟垛标规则（前缀、产线号） */
    private PalletRuleConfig palletRule = new PalletRuleConfig();

    /** 业务：上传配置 */
    private UploadConfig upload = new UploadConfig();

    /** 页面配置：8 个 Tab 的显示开关（key 为页面 id，value 为是否显示） */
    private Map<String, Boolean> pageVisible = defaultPageVisible();

    /** 页面配置：Tab 显示顺序（仅包含启用的页面 id，第一个为数据采集） */
    private List<String> pageTabOrder = defaultPageTabOrder();

    /** 连接：数据库配置 */
    private DbConnectionConfig dbConnection = new DbConnectionConfig();

    /** 设备：打印机配置 */
    private PrinterConfig printer = new PrinterConfig();

    /** 设备：报警配置 */
    private AlarmConfig alarm = new AlarmConfig();

    /** 接口：API base URL 等 */
    private ApiConfig api = new ApiConfig();

    public static Map<String, Boolean> defaultPageVisible() {
        Map<String, Boolean> m = new LinkedHashMap<>();
        m.put("dataCollection", true);  // 数据采集（盒箱垛）必选
        m.put("manual", true);
        m.put("query", true);
        m.put("replace", true);
        m.put("stats", true);
        m.put("package", true);
        m.put("cancel", true);
        m.put("upload", true);
        return m;
    }

    public static List<String> defaultPageTabOrder() {
        List<String> order = new ArrayList<>();
        order.add("dataCollection");
        order.add("manual");
        order.add("query");
        order.add("replace");
        order.add("stats");
        order.add("package");
        order.add("cancel");
        order.add("upload");
        return order;
    }

    // ---------- getters/setters ----------

    public String getSystemSettingsPassword() {
        return systemSettingsPassword;
    }

    public void setSystemSettingsPassword(String systemSettingsPassword) {
        this.systemSettingsPassword = systemSettingsPassword;
    }

    public CodeDigitsConfig getCodeDigits() {
        return codeDigits;
    }

    public void setCodeDigits(CodeDigitsConfig codeDigits) {
        this.codeDigits = codeDigits;
    }

    public PalletRuleConfig getPalletRule() {
        return palletRule;
    }

    public void setPalletRule(PalletRuleConfig palletRule) {
        this.palletRule = palletRule;
    }

    public UploadConfig getUpload() {
        return upload;
    }

    public void setUpload(UploadConfig upload) {
        this.upload = upload;
    }

    public Map<String, Boolean> getPageVisible() {
        return pageVisible;
    }

    public void setPageVisible(Map<String, Boolean> pageVisible) {
        this.pageVisible = pageVisible;
    }

    public List<String> getPageTabOrder() {
        return pageTabOrder;
    }

    public void setPageTabOrder(List<String> pageTabOrder) {
        this.pageTabOrder = pageTabOrder;
    }

    public DbConnectionConfig getDbConnection() {
        return dbConnection;
    }

    public void setDbConnection(DbConnectionConfig dbConnection) {
        this.dbConnection = dbConnection;
    }

    public PrinterConfig getPrinter() {
        return printer;
    }

    public void setPrinter(PrinterConfig printer) {
        this.printer = printer;
    }

    public AlarmConfig getAlarm() {
        return alarm;
    }

    public void setAlarm(AlarmConfig alarm) {
        this.alarm = alarm;
    }

    public ApiConfig getApi() {
        return api;
    }

    public void setApi(ApiConfig api) {
        this.api = api;
    }

    // ---------- 嵌套配置类 ----------

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CodeDigitsConfig {
        private int smallCodeDigits = 14;
        private int mediumCodeDigits = 14;
        private int largeCodeDigits = -1;

        public int getSmallCodeDigits() { return smallCodeDigits; }
        public void setSmallCodeDigits(int smallCodeDigits) { this.smallCodeDigits = smallCodeDigits; }
        public int getMediumCodeDigits() { return mediumCodeDigits; }
        public void setMediumCodeDigits(int mediumCodeDigits) { this.mediumCodeDigits = mediumCodeDigits; }
        public int getLargeCodeDigits() { return largeCodeDigits; }
        public void setLargeCodeDigits(int largeCodeDigits) { this.largeCodeDigits = largeCodeDigits; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PalletRuleConfig {
        private String prefix = "V";
        private String lineCode = "A";

        public String getPrefix() { return prefix; }
        public void setPrefix(String prefix) { this.prefix = prefix; }
        public String getLineCode() { return lineCode; }
        public void setLineCode(String lineCode) { this.lineCode = lineCode; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UploadConfig {
        private boolean autoUpload = true;

        public boolean isAutoUpload() { return autoUpload; }
        public void setAutoUpload(boolean autoUpload) { this.autoUpload = autoUpload; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DbConnectionConfig {
        private String host = "127.0.0.1";
        private String port = "3306";
        private String database = "production_db";
        private String username = "root";
        private String password = "";

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
    public static class PrinterConfig {
        private String printerName = "Canon LBP2900";
        private String printerIp = "192.168.1.201";
        private String printerPort = "9100";
        private String paperSize = "A4";

        public String getPrinterName() { return printerName; }
        public void setPrinterName(String printerName) { this.printerName = printerName; }
        public String getPrinterIp() { return printerIp; }
        public void setPrinterIp(String printerIp) { this.printerIp = printerIp; }
        public String getPrinterPort() { return printerPort; }
        public void setPrinterPort(String printerPort) { this.printerPort = printerPort; }
        public String getPaperSize() { return paperSize; }
        public void setPaperSize(String paperSize) { this.paperSize = paperSize; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AlarmConfig {
        private boolean soundAlarmEnabled = true;
        private int alarmDelayMs = 500;
        private int alarmIntervalMs = 2000;

        public boolean isSoundAlarmEnabled() { return soundAlarmEnabled; }
        public void setSoundAlarmEnabled(boolean soundAlarmEnabled) { this.soundAlarmEnabled = soundAlarmEnabled; }
        public int getAlarmDelayMs() { return alarmDelayMs; }
        public void setAlarmDelayMs(int alarmDelayMs) { this.alarmDelayMs = alarmDelayMs; }
        public int getAlarmIntervalMs() { return alarmIntervalMs; }
        public void setAlarmIntervalMs(int alarmIntervalMs) { this.alarmIntervalMs = alarmIntervalMs; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApiConfig {
        private String baseUrl = "https://openapi.weixin12315.com";
        private String syncCodeAndVirtualRelationPath = "/api/sign/md.fc.Store/v1/SyncCodeAndVirtualRelation";
        private String getSyncResultPath = "/api/sign/md.fc.Store/v1/GetSyncCodeAndVirtualRelationResult";
        private String codePackageQueryCompletedPath = "/api/v1/code/package/query-completed";

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getSyncCodeAndVirtualRelationPath() { return syncCodeAndVirtualRelationPath; }
        public void setSyncCodeAndVirtualRelationPath(String path) { this.syncCodeAndVirtualRelationPath = path; }
        public String getGetSyncResultPath() { return getSyncResultPath; }
        public void setGetSyncResultPath(String path) { this.getSyncResultPath = path; }
        public String getCodePackageQueryCompletedPath() { return codePackageQueryCompletedPath; }
        public void setCodePackageQueryCompletedPath(String path) { this.codePackageQueryCompletedPath = path; }
    }
}
