package com.miduo.cloud.infrastructure.openplatform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 开放平台码包客户端
 */
@Component
public class CodePackageOpenPlatformClient {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${code.package.openplatform.url:}")
    private String openPlatformBaseUrl;

    public QueryCompletedResponse queryCompleted(LocalDateTime startTime, LocalDateTime endTime) throws Exception {
        if (!StringUtils.hasText(openPlatformBaseUrl)) {
            throw new IllegalStateException("未配置 code.package.openplatform.url");
        }
        String endpoint = normalizeBaseUrl(openPlatformBaseUrl) + "/api/v1/code/package/query-completed";
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(endpoint).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(20000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");

            String body = buildQueryBody(startTime, endTime);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = connection.getResponseCode();
            InputStream inputStream = responseCode >= 200 && responseCode < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            String responseText = readAll(inputStream);
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
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public List<String> downloadCodeLines(String fileDownloadAddress) throws Exception {
        if (!StringUtils.hasText(fileDownloadAddress)) {
            return new ArrayList<>();
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(fileDownloadAddress).openConnection();
            connection.setRequestMethod("GET");
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

    private String buildQueryBody(LocalDateTime startTime, LocalDateTime endTime) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"start_time\":\"").append(TIME_FMT.format(startTime)).append("\"");
        if (endTime != null) {
            builder.append(",\"end_time\":\"").append(TIME_FMT.format(endTime)).append("\"");
        }
        builder.append("}");
        return builder.toString();
    }

    private String normalizeBaseUrl(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    private String readAll(InputStream inputStream) throws Exception {
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
