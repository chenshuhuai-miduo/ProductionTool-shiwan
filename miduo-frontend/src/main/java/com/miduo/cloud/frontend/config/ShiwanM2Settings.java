package com.miduo.cloud.frontend.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    /** 连接：2 号机数据库配置（MySQL） */
    private DbConnectionConfig dbConnection = new DbConnectionConfig();

    /** 连接：1 号机数据库配置（SQL Server，用于 T_Code 同步） */
    private M1DbConnectionConfig m1DbConnection = new M1DbConnectionConfig();

    /** 1 号机 T_Code 同步：上次已同步的最大 SerialNo（持久化到配置或由后端维护） */
    private Long lastSyncedM1SerialNo;

    /** 设备：打印机配置 */
    private PrinterConfig printer = new PrinterConfig();

    /** 设备：报警配置 */
    private AlarmConfig alarm = new AlarmConfig();

    /** 设备：盒箱相机 TCP 采集批次匹配等待超时 */
    private BoxCaseCameraCaptureConfig boxCaseCameraCapture = new BoxCaseCameraCaptureConfig();

    /** 设备：自定义控制信号（16进制，用于主界面手动控制按钮） */
    private DeviceSignalConfig deviceSignal = new DeviceSignalConfig();

    /** 接口：API base URL 等 */
    private ApiConfig api = new ApiConfig();

    /** 入库仓库编号（上传垛码关联数据时作为 warehouseno 入参，默认 001） */
    private String warehouseNo = "001";

    /** 采集规格：每垛箱数、每箱盒数（用于持久化上次采集规格） */
    private Integer boxesPerPallet = 70;
    private Integer boxesPerCase = 4;

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

    public M1DbConnectionConfig getM1DbConnection() {
        return m1DbConnection;
    }

    public void setM1DbConnection(M1DbConnectionConfig m1DbConnection) {
        this.m1DbConnection = m1DbConnection;
    }

    public Long getLastSyncedM1SerialNo() {
        return lastSyncedM1SerialNo;
    }

    public void setLastSyncedM1SerialNo(Long lastSyncedM1SerialNo) {
        this.lastSyncedM1SerialNo = lastSyncedM1SerialNo;
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

    public BoxCaseCameraCaptureConfig getBoxCaseCameraCapture() {
        return boxCaseCameraCapture;
    }

    public void setBoxCaseCameraCapture(BoxCaseCameraCaptureConfig boxCaseCameraCapture) {
        this.boxCaseCameraCapture = boxCaseCameraCapture;
    }

    public DeviceSignalConfig getDeviceSignal() {
        return deviceSignal;
    }

    public void setDeviceSignal(DeviceSignalConfig deviceSignal) {
        this.deviceSignal = deviceSignal;
    }

    public ApiConfig getApi() {
        return api;
    }

    public void setApi(ApiConfig api) {
        this.api = api;
    }

    public String getWarehouseNo() {
        return warehouseNo;
    }

    public void setWarehouseNo(String warehouseNo) {
        this.warehouseNo = warehouseNo;
    }

    public Integer getBoxesPerPallet() {
        return boxesPerPallet;
    }

    public void setBoxesPerPallet(Integer boxesPerPallet) {
        this.boxesPerPallet = boxesPerPallet;
    }

    public Integer getBoxesPerCase() {
        return boxesPerCase;
    }

    public void setBoxesPerCase(Integer boxesPerCase) {
        this.boxesPerCase = boxesPerCase;
    }

    // ---------- 嵌套配置类 ----------

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
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
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PalletRuleConfig {
        private String prefix = "V";
        private String lineCode = "A";

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

    /** 1 号机 SQL Server 连接配置（T_Code 表同步） */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class M1DbConnectionConfig {
        private String host = "192.168.1.100";
        private String port = "1433";
        private String database = "";
        private String tableName = "T_Code";
        private String username = "sa";
        private String password = "";
        /**
         * 首次同步起始游标（SerialNo）。仅在游标文件不存在时生效（即第一次同步），
         * 同步将从该 SerialNo 之后的数据开始拉取；若为 null 则从 0 开始（拉取全部数据）。
         */
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

    /**
     * 设备控制信号配置（16进制字符串，通过串口发送至报警器/指示灯控制器）。
     * <p>业务状态指令（7个）：编码灯光状态+剔除状态，由系统自动发送。
     * <p>手动按钮信号（6个）：供主界面手动按钮使用，由用户根据实际协议填写。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DeviceSignalConfig {

        // -------- 业务状态指令（系统自动发送，默认已预置协议值）--------

        /** 全关：退出软件/停止采集时发送，关闭所有灯光和剔除 */
        private String cmdAllOff                  = "21 0F 00 00 00 08 01 00 FC 8D";
        /** 正常运行：工控机灯+龙门架灯亮（开始采集、收回剔除后） */
        private String cmdNormalOn                = "21 0F 00 00 00 08 01 04 FD 4E";
        /** 正常运行+剔除中：遇到问题触发剔除装置 */
        private String cmdNormalWithReject        = "21 0F 00 00 00 08 01 05 3C 8E";
        /** 上传成功：三灯亮（工控机+龙门架+上传成功灯），1分钟后自动切回正常 */
        private String cmdUploadSuccess           = "21 0F 00 00 00 08 01 06 7C 8F";
        /** 上传成功期间发生剔除：三灯亮+剔除 */
        private String cmdUploadSuccessWithReject = "21 0F 00 00 00 08 01 07 BD 4F";
        /** 上传失败报警：工控机灯+龙门架灯+红灯蜂鸣，持续报警直到手动关闭 */
        private String cmdUploadFail              = "21 0F 00 00 00 08 01 0C FC 88";
        /** 上传失败报警期间发生剔除：红灯蜂鸣+剔除 */
        private String cmdUploadFailWithReject    = "21 0F 00 00 00 08 01 0D 3D 48";

        // -------- 手动按钮信号（主界面按钮使用，用户自定义）--------

        /** 主界面"打开报警"按钮信号 */
        private String alarmOpenHex = "";
        /** 主界面"关闭报警"按钮信号 */
        private String alarmCloseHex = "";
        /** 主界面"测试报警灯亮"按钮信号 */
        private String alarmLightOnHex = "";
        /** 主界面"测试报警灯灭"按钮信号 */
        private String alarmLightOffHex = "";
        /** 主界面"触发剔除"按钮信号 */
        private String rejectTriggerHex = "";
        /** 主界面"收回剔除"按钮信号 */
        private String rejectRetractHex = "";

        public String getCmdAllOff() { return cmdAllOff; }
        public void setCmdAllOff(String v) { this.cmdAllOff = v; }
        public String getCmdNormalOn() { return cmdNormalOn; }
        public void setCmdNormalOn(String v) { this.cmdNormalOn = v; }
        public String getCmdNormalWithReject() { return cmdNormalWithReject; }
        public void setCmdNormalWithReject(String v) { this.cmdNormalWithReject = v; }
        public String getCmdUploadSuccess() { return cmdUploadSuccess; }
        public void setCmdUploadSuccess(String v) { this.cmdUploadSuccess = v; }
        public String getCmdUploadSuccessWithReject() { return cmdUploadSuccessWithReject; }
        public void setCmdUploadSuccessWithReject(String v) { this.cmdUploadSuccessWithReject = v; }
        public String getCmdUploadFail() { return cmdUploadFail; }
        public void setCmdUploadFail(String v) { this.cmdUploadFail = v; }
        public String getCmdUploadFailWithReject() { return cmdUploadFailWithReject; }
        public void setCmdUploadFailWithReject(String v) { this.cmdUploadFailWithReject = v; }

        public String getAlarmOpenHex() { return alarmOpenHex; }
        public void setAlarmOpenHex(String v) { this.alarmOpenHex = v; }
        public String getAlarmCloseHex() { return alarmCloseHex; }
        public void setAlarmCloseHex(String v) { this.alarmCloseHex = v; }
        public String getAlarmLightOnHex() { return alarmLightOnHex; }
        public void setAlarmLightOnHex(String v) { this.alarmLightOnHex = v; }
        public String getAlarmLightOffHex() { return alarmLightOffHex; }
        public void setAlarmLightOffHex(String v) { this.alarmLightOffHex = v; }
        public String getRejectTriggerHex() { return rejectTriggerHex; }
        public void setRejectTriggerHex(String v) { this.rejectTriggerHex = v; }
        public String getRejectRetractHex() { return rejectRetractHex; }
        public void setRejectRetractHex(String v) { this.rejectRetractHex = v; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AlarmConfig {
        private boolean soundAlarmEnabled = true;
        private int alarmDelayMs = 500;
        private int alarmIntervalMs = 2000;
        /**
         * 触发剔除延时（ms）：检测到需要剔除后，等待该时间再发送剔除信号，
         * 用于让物品移动到剔除装置正上方。0 表示立即发送。
         */
        private int rejectTriggerDelayMs = 0;
        /**
         * 剔除收回时间（ms）：发送剔除信号后，等待该时间自动发送收回（恢复正常状态）信号。
         */
        private int rejectRetractTimeMs = 2000;

        public boolean isSoundAlarmEnabled() { return soundAlarmEnabled; }
        public void setSoundAlarmEnabled(boolean soundAlarmEnabled) { this.soundAlarmEnabled = soundAlarmEnabled; }
        public int getAlarmDelayMs() { return alarmDelayMs; }
        public void setAlarmDelayMs(int alarmDelayMs) { this.alarmDelayMs = alarmDelayMs; }
        public int getAlarmIntervalMs() { return alarmIntervalMs; }
        public void setAlarmIntervalMs(int alarmIntervalMs) { this.alarmIntervalMs = alarmIntervalMs; }
        public int getRejectTriggerDelayMs() { return rejectTriggerDelayMs; }
        public void setRejectTriggerDelayMs(int v) { this.rejectTriggerDelayMs = v; }
        public int getRejectRetractTimeMs() { return rejectRetractTimeMs; }
        public void setRejectRetractTimeMs(int v) { this.rejectRetractTimeMs = v; }
    }

    /**
     * 盒箱双相机采集：一侧已收到某批号后，等待另一侧同批号数据的最长时间。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BoxCaseCameraCaptureConfig {
        /**
         * 双相机等待超时（ms）。一侧入队后超过此时间仍未收到同批号另一侧数据则整批剔除。0 表示不启用。
         */
        private int matchWaitTimeoutMs = 3000;

        public int getMatchWaitTimeoutMs() { return matchWaitTimeoutMs; }
        public void setMatchWaitTimeoutMs(int matchWaitTimeoutMs) { this.matchWaitTimeoutMs = matchWaitTimeoutMs; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApiConfig {
        private String baseUrl = "https://openapi.weixin12315.com";
        /** 本地后端 REST API 地址（前端调用 /api/shiwan-m2/* 等接口的根地址，默认 8080 端口） */
        private String backendBaseUrl = "http://localhost:8080";
        /** JSON 中的 key 为 appId，与后端 ShiwanM2SettingsDto 保持一致 */
        @JsonProperty("appId")
        private String appId;
        private String appSecret;
        /** 开放平台登录账号（Memberlogin 字段值，用于码替换等接口请求体） */
        private String memberlogin;
        private String syncCodeAndVirtualRelationPath = "/api/sign/md.fc.Store/v1/SyncCodeAndVirtualRelation";
        private String getSyncResultPath = "/api/sign/md.fc.Store/v1/GetSyncCodeAndVirtualRelationResult";
        @com.fasterxml.jackson.annotation.JsonAlias("codePackageQueryCompletedPath")
        private String codePackageQueryPath = "/api/sign/md.fc.LevelPackage/v1/querycompleted";
        private String productsListPath = "/api/sign/md.shop.products/v1/list";
        /** 码替换接口路径 */
        private String codeSubstitutionPath;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getBackendBaseUrl() { return backendBaseUrl; }
        public void setBackendBaseUrl(String backendBaseUrl) { this.backendBaseUrl = backendBaseUrl; }
        public String getAppId() { return appId; }
        public void setAppId(String appId) { this.appId = appId; }
        public String getAppSecret() { return appSecret; }
        public void setAppSecret(String appSecret) { this.appSecret = appSecret; }
        public String getMemberlogin() { return memberlogin; }
        public void setMemberlogin(String memberlogin) { this.memberlogin = memberlogin; }
        public String getSyncCodeAndVirtualRelationPath() { return syncCodeAndVirtualRelationPath; }
        public void setSyncCodeAndVirtualRelationPath(String path) { this.syncCodeAndVirtualRelationPath = path; }
        public String getGetSyncResultPath() { return getSyncResultPath; }
        public void setGetSyncResultPath(String path) { this.getSyncResultPath = path; }
        public String getCodePackageQueryPath() { return codePackageQueryPath; }
        public void setCodePackageQueryPath(String path) { this.codePackageQueryPath = path; }
        public String getProductsListPath() { return productsListPath; }
        public void setProductsListPath(String path) { this.productsListPath = path; }
        public String getCodeSubstitutionPath() { return codeSubstitutionPath; }
        public void setCodeSubstitutionPath(String codeSubstitutionPath) { this.codeSubstitutionPath = codeSubstitutionPath; }
    }
}
