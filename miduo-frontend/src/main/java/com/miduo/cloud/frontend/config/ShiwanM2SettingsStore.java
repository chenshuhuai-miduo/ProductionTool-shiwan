package com.miduo.cloud.frontend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 石湾 2 号机系统设置 JSON 持久化。
 * 文件路径：user.dir / shiwan-m2-settings.json
 */
public final class ShiwanM2SettingsStore {

    private static final String SETTINGS_FILE_NAME = "shiwan-m2-settings.json";
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /** 内存缓存，启动时加载，保存后更新 */
    private static volatile ShiwanM2Settings cached;

    private ShiwanM2SettingsStore() {}

    /** 配置文件完整路径（user.dir 下） */
    public static String getConfigPath() {
        String userDir = System.getProperty("user.dir");
        return userDir + File.separator + SETTINGS_FILE_NAME;
    }

    /**
     * 加载配置；若文件不存在或解析失败则返回默认配置（不写回文件）。
     */
    public static ShiwanM2Settings load() {
        Path path = Paths.get(getConfigPath());
        if (!Files.isRegularFile(path)) {
            return new ShiwanM2Settings();
        }
        try {
            String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            ShiwanM2Settings s = MAPPER.readValue(json, ShiwanM2Settings.class);
            ensureDefaults(s);
            return s;
        } catch (IOException e) {
            System.err.println("[ShiwanM2SettingsStore] 解析配置文件失败，使用默认值：" + e.getMessage());
            return new ShiwanM2Settings();
        }
    }

    /**
     * 保存配置到文件，并更新内存缓存。
     */
    public static void save(ShiwanM2Settings settings) throws IOException {
        if (settings == null) {
            return;
        }
        Path path = Paths.get(getConfigPath());
        File parent = path.toFile().getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        String json = MAPPER.writeValueAsString(settings);
        Files.write(path, json.getBytes(StandardCharsets.UTF_8));
        cached = settings;
    }

    /**
     * 获取当前配置（若未加载则从文件加载并缓存；启动时首次调用即完成接口等配置加载到内存）。
     */
    public static ShiwanM2Settings get() {
        if (cached == null) {
            synchronized (ShiwanM2SettingsStore.class) {
                if (cached == null) {
                    cached = load();
                }
            }
        }
        return cached;
    }

    /**
     * 更新内存缓存（例如设置对话框保存后调用，供主界面等读取最新值）。
     */
    public static void setCached(ShiwanM2Settings settings) {
        cached = settings;
    }

    /** 供码包、上传等模块使用：获取接口 base URL（已随 get() 加载到内存）。 */
    public static String getApiBaseUrl() {
        ShiwanM2Settings.ApiConfig api = get().getApi();
        return api != null && api.getBaseUrl() != null ? api.getBaseUrl() : "https://openapi.weixin12315.com";
    }

    /** 供前端 HttpUtil 使用：获取本地后端 REST API 根地址（如 http://localhost:8080）。 */
    public static String getBackendBaseUrl() {
        ShiwanM2Settings.ApiConfig api = get().getApi();
        String url = api != null && api.getBackendBaseUrl() != null ? api.getBackendBaseUrl().trim() : null;
        if (url == null || url.isEmpty()) return "http://localhost:8080";
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "http://" + url;
        return url.replaceAll("/+$", "");
    }

    /** 确保嵌套对象与页面配置默认值存在 */
    private static void ensureDefaults(ShiwanM2Settings s) {
        if (s.getCodeDigits() == null) s.setCodeDigits(new ShiwanM2Settings.CodeDigitsConfig());
        if (s.getPalletRule() == null) s.setPalletRule(new ShiwanM2Settings.PalletRuleConfig());
        if (s.getUpload() == null) s.setUpload(new ShiwanM2Settings.UploadConfig());
        if (s.getPageVisible() == null) s.setPageVisible(ShiwanM2Settings.defaultPageVisible());
        if (s.getPageTabOrder() == null) s.setPageTabOrder(ShiwanM2Settings.defaultPageTabOrder());
        if (s.getDbConnection() == null) s.setDbConnection(new ShiwanM2Settings.DbConnectionConfig());
        if (s.getM1DbConnection() == null) s.setM1DbConnection(new ShiwanM2Settings.M1DbConnectionConfig());
        if (s.getPrinter() == null) s.setPrinter(new ShiwanM2Settings.PrinterConfig());
        if (s.getAlarm() == null) s.setAlarm(new ShiwanM2Settings.AlarmConfig());
        if (s.getBoxCaseCameraCapture() == null) s.setBoxCaseCameraCapture(new ShiwanM2Settings.BoxCaseCameraCaptureConfig());
        if (s.getApi() == null) s.setApi(new ShiwanM2Settings.ApiConfig());
        if (s.getSystemSettingsPassword() == null) s.setSystemSettingsPassword("123456");
    }
}
