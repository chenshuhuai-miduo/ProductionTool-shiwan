package com.miduo.cloud.infrastructure.openplatform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miduo.cloud.common.config.ShiwanM2SettingsDto;
import com.miduo.cloud.common.config.ShiwanM2SettingsFileLoader;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 开放平台码包客户端 —— 签名方式与 ProductSyncService 保持一致。
 * 调用地址：api/sign/md.fc.LevelPackage/v1/querycompleted（开放平台签名接口）。
 * 配置来源：ShiwanM2SettingsFileLoader（shiwan-m2-settings.json）。
 */
@Component
public class CodePackageOpenPlatformClient {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 查询开放平台上已完成的码包任务。
     *
     * @param startTime 增量起始时间（取两种类型中更早的那个）
     * @param endTime   截止时间
     */
    public QueryCompletedResponse queryCompleted(LocalDateTime startTime, LocalDateTime endTime) throws Exception {
        ShiwanM2SettingsDto cfg = ShiwanM2SettingsFileLoader.load();
        if (cfg == null || cfg.getApi() == null) {
            throw new IllegalStateException("未加载到开放平台配置（api 节点为空）");
        }
        String baseUrl = trim(cfg.getApi().getBaseUrl());
        String path = trim(cfg.getApi().getCodePackageQueryPath());
        String appId = trim(cfg.getApi().getAppId());
        String appSecret = trim(cfg.getApi().getAppSecret());
        if (baseUrl == null || path == null) {
            throw new IllegalStateException("缺少开放平台配置：baseUrl 或 codePackageQueryPath");
        }
        if (appId == null || appSecret == null) {
            throw new IllegalStateException("缺少开放平台配置：appId 或 appSecret");
        }

        // 组装请求体参数（用于签名和序列化）
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("start_time", TIME_FMT.format(startTime));
        if (endTime != null) {
            requestData.put("end_time", TIME_FMT.format(endTime));
        }

        // 生成签名（与 ProductSyncService 逻辑完全一致）
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String timestamp = sdf.format(new Date());
        String nonce = "123";
        String sign = getSign(timestamp, nonce, appSecret, requestData);

        Map<String, String> headers = new HashMap<>();
        headers.put("timestamp", timestamp);
        headers.put("appid", appId);
        headers.put("nonce", nonce);
        headers.put("sign", sign);
        headers.put("Content-type", "application/json");

        String requestUrl = baseUrl + path;
        String body = objectMapper.writeValueAsString(requestData);
        String responseText = sendPostRequest(requestUrl, headers, body);

        JsonNode root = objectMapper.readTree(responseText);
        String returnCode = root.path("return_code").asText();
        String returnMsg = root.path("return_msg").asText();
        if (!"0".equals(returnCode)) {
            throw new IllegalStateException("开放平台返回失败: " + returnMsg);
        }

        QueryCompletedResponse result = new QueryCompletedResponse();
        JsonNode dataNode = root.path("return_data").path("data");
        if (dataNode.isArray()) {
            Iterator<JsonNode> iterator = dataNode.elements();
            while (iterator.hasNext()) {
                JsonNode item = iterator.next();
                QueryCompletedItem completedItem = new QueryCompletedItem();
                completedItem.setId(item.path("id").asText(null));
                completedItem.setFileName(item.path("file_name").asText(null));
                completedItem.setRelationshipType(item.path("relationship_type").asInt());
                completedItem.setUploadTime(item.path("upload_time").asText(null));
                completedItem.setEndProcessTime(item.path("end_process_time").asText(null));
                completedItem.setFileDownloadAddress(item.path("file_download_address").asText(null));
                result.getItems().add(completedItem);
            }
        }
        return result;
    }

    /**
     * 下载码包文件，按行返回码值列表。
     */
    public List<String> downloadCodeLines(String fileDownloadAddress) throws Exception {
        if (fileDownloadAddress == null || fileDownloadAddress.trim().isEmpty()) {
            return new ArrayList<>();
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(fileDownloadAddress).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                List<String> lines = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
                return lines;
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // ─── 签名与 HTTP 工具（与 ProductSyncService 保持一致） ───────────────────

    private static String getSign(String time, String nonce, String secret, Map<String, Object> data) throws Exception {
        TreeMap<String, Object> sortedData = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        sortedData.putAll(data);
        StringBuilder params = new StringBuilder();
        for (Map.Entry<String, Object> entry : sortedData.entrySet()) {
            String key = entry.getKey();
            String value = String.valueOf(entry.getValue());
            if (key != null && !key.isEmpty()) {
                params.append(key).append(value);
            }
        }
        String signStr = time + nonce + secret + params;
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] bytes = md.digest(signStr.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }

    private static String sendPostRequest(String urlStr, Map<String, String> headers, String data) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
            conn.setDoOutput(true);
            conn.setDoInput(true);
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = data.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            int responseCode = conn.getResponseCode();
            InputStream inputStream = (responseCode >= 200 && responseCode < 300)
                    ? conn.getInputStream() : conn.getErrorStream();
            return readAll(inputStream);
        } finally {
            conn.disconnect();
        }
    }

    private static String readAll(InputStream inputStream) throws Exception {
        if (inputStream == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    // ─── 响应 DTO ─────────────────────────────────────────────────────────────

    @Data
    public static class QueryCompletedResponse {
        private List<QueryCompletedItem> items = new ArrayList<>();
    }

    @Data
    public static class QueryCompletedItem {
        private String id;
        private String fileName;
        private Integer relationshipType;
        private String uploadTime;
        private String endProcessTime;
        private String fileDownloadAddress;
    }
}
