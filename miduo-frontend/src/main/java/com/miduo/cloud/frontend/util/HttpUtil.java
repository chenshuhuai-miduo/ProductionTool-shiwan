package com.miduo.cloud.frontend.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.application.Platform;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * HTTP请求工具类
 * 用于JavaFX前端调用Spring Boot后端接口
 * <p>
 * 重要：doGet / doPost 等同步方法禁止在 JavaFX 应用线程上调用，
 * 否则会阻塞 UI。请改用 asyncGet / asyncPost 异步方法。
 */
public class HttpUtil {

    /** 共享后台线程池，供 asyncGet / asyncPost 使用 */
    private static final ExecutorService ASYNC_POOL = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "http-async");
        t.setDaemon(true);
        return t;
    });
    
    /** 默认后端地址；石湾2号机启动时会从 shiwan-m2-settings 覆盖为配置的 backendBaseUrl */
    private static volatile String baseUrl = "http://localhost:8080";
    
    /**
     * 设置后端 API 根地址（如 http://localhost:8080 或 http://192.168.1.100:8080）
     */
    public static void setBaseUrl(String url) {
        if (url != null && !url.isEmpty()) {
            baseUrl = url.trim().replaceAll("/+$", "");
            if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
                baseUrl = "http://" + baseUrl;
            }
        }
    }
    
    /**
     * 获取当前后端 API 根地址
     */
    public static String getBaseUrl() {
        return baseUrl;
    }
    
    /**
     * Jackson对象映射器
     */
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    static {
        // 注册Java 8时间模块
        objectMapper.registerModule(new JavaTimeModule());
        // 禁用将日期写为时间戳的功能
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    /**
     * 请求配置（常规接口）
     */
    private static final RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(5000)        // 连接超时5秒
            .setSocketTimeout(30000)        // 读取超时30秒（生产统计等聚合查询耗时较长）
            .setConnectionRequestTimeout(3000)  // 从连接池获取连接超时3秒
            .build();

    /**
     * 长时操作请求配置：码包本地导入 / 在线更新等耗时较长的接口使用。
     * socketTimeout = 10 分钟，避免大文件处理时 ReadTimeout 中断。
     */
    private static final RequestConfig longRequestConfig = RequestConfig.custom()
            .setConnectTimeout(10000)
            .setSocketTimeout(600000)       // 读取超时10分钟
            .setConnectionRequestTimeout(5000)
            .build();
    
    /**
     * GET请求
     * @param url 请求URL（相对路径）
     * @return 响应字符串
     */
    public static String doGet(String url) throws IOException {
        warnIfFxThread("doGet", url);
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(getBaseUrl() + url);
        httpGet.setConfig(requestConfig);
        httpGet.setHeader("Content-Type", "application/json;charset=UTF-8");
        
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        } finally {
            httpClient.close();
        }
    }
    
    /**
     * POST请求
     * @param url 请求URL（相对路径）
     * @param jsonBody JSON请求体
     * @return 响应字符串
     */
    public static String doPost(String url, String jsonBody) throws IOException {
        warnIfFxThread("doPost", url);
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(getBaseUrl() + url);
        httpPost.setConfig(requestConfig);
        httpPost.setHeader("Content-Type", "application/json;charset=UTF-8");
        
        if (jsonBody != null && !jsonBody.isEmpty()) {
            StringEntity entity = new StringEntity(jsonBody, StandardCharsets.UTF_8);
            httpPost.setEntity(entity);
        }
        
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        } finally {
            httpClient.close();
        }
    }
    
    /**
     * POST请求（对象自动转JSON）
     * @param url 请求URL（相对路径）
     * @param requestBody 请求对象
     * @return 响应字符串
     */
    public static String doPost(String url, Object requestBody) throws IOException {
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        return doPost(url, jsonBody);
    }
    
    /**
     * PUT请求
     * @param url 请求URL（相对路径）
     * @param jsonBody JSON请求体
     * @return 响应字符串
     */
    public static String doPut(String url, String jsonBody) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPut httpPut = new HttpPut(getBaseUrl() + url);
        httpPut.setConfig(requestConfig);
        httpPut.setHeader("Content-Type", "application/json;charset=UTF-8");
        
        if (jsonBody != null && !jsonBody.isEmpty()) {
            StringEntity entity = new StringEntity(jsonBody, StandardCharsets.UTF_8);
            httpPut.setEntity(entity);
        }
        
        try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
            return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        } finally {
            httpClient.close();
        }
    }
    
    /**
     * PUT请求（对象自动转JSON）
     * @param url 请求URL（相对路径）
     * @param requestBody 请求对象
     * @return 响应字符串
     */
    public static String doPut(String url, Object requestBody) throws IOException {
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        return doPut(url, jsonBody);
    }
    
    /**
     * POST请求（长时操作，10分钟超时）。
     * 适用于码包本地导入、在线更新等耗时接口，避免 ReadTimeout 中断。
     *
     * @param url      请求URL（相对路径）
     * @param jsonBody JSON请求体
     * @return 响应字符串
     */
    public static String doPostLong(String url, String jsonBody) throws IOException {
        warnIfFxThread("doPostLong", url);
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(getBaseUrl() + url);
        httpPost.setConfig(longRequestConfig);
        httpPost.setHeader("Content-Type", "application/json;charset=UTF-8");
        if (jsonBody != null && !jsonBody.isEmpty()) {
            StringEntity entity = new StringEntity(jsonBody, StandardCharsets.UTF_8);
            httpPost.setEntity(entity);
        }
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        } finally {
            httpClient.close();
        }
    }

    /**
     * DELETE请求
     * @param url 请求URL（相对路径）
     * @return 响应字符串
     */
    public static String doDelete(String url) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpDelete httpDelete = new HttpDelete(getBaseUrl() + url);
        httpDelete.setConfig(requestConfig);
        httpDelete.setHeader("Content-Type", "application/json;charset=UTF-8");
        
        try (CloseableHttpResponse response = httpClient.execute(httpDelete)) {
            return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        } finally {
            httpClient.close();
        }
    }
    
    /**
     * 将JSON字符串转换为对象
     * @param json JSON字符串
     * @param clazz 目标类型
     * @return 转换后的对象
     */
    public static <T> T parseJson(String json, Class<T> clazz) throws IOException {
        return objectMapper.readValue(json, clazz);
    }
    
    /**
     * 将JSON字符串转换为对象（泛型支持）
     * @param json JSON字符串
     * @param typeReference 类型引用
     * @return 转换后的对象
     */
    public static <T> T parseJson(String json, com.fasterxml.jackson.core.type.TypeReference<T> typeReference) throws IOException {
        return objectMapper.readValue(json, typeReference);
    }
    
    /**
     * 获取ObjectMapper实例
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    // ================================================================
    //  异步 API：在后台线程执行 HTTP 调用，结果回调在 FX 线程
    // ================================================================

    /**
     * 异步 GET。HTTP 在后台线程执行，onSuccess/onError 在 FX 线程回调。
     * 这是 FX 控制器调用 HTTP 的推荐方式。
     */
    public static void asyncGet(String url, Consumer<String> onSuccess, Consumer<Exception> onError) {
        ASYNC_POOL.submit(() -> {
            try {
                String result = doGet(url);
                if (onSuccess != null) Platform.runLater(() -> onSuccess.accept(result));
            } catch (Exception e) {
                if (onError != null) Platform.runLater(() -> onError.accept(e));
            }
        });
    }

    /**
     * 异步 POST。HTTP 在后台线程执行，onSuccess/onError 在 FX 线程回调。
     */
    public static void asyncPost(String url, String jsonBody, Consumer<String> onSuccess, Consumer<Exception> onError) {
        ASYNC_POOL.submit(() -> {
            try {
                String result = doPost(url, jsonBody);
                if (onSuccess != null) Platform.runLater(() -> onSuccess.accept(result));
            } catch (Exception e) {
                if (onError != null) Platform.runLater(() -> onError.accept(e));
            }
        });
    }

    /**
     * FX 线程检测：如果在 JavaFX 应用线程上调用同步 HTTP 方法，打印堆栈警告。
     * 不抛异常（避免破坏现有流程），但日志中会醒目提示以便开发时发现并修复。
     */
    private static void warnIfFxThread(String method, String url) {
        try {
            if (Platform.isFxApplicationThread()) {
                System.err.println("[HttpUtil] WARNING: " + method + "(" + url
                        + ") called on FX Application Thread! This blocks UI.");
                new Exception("FX-thread HTTP call stack trace").printStackTrace(System.err);
            }
        } catch (Exception ignored) {}
    }
}

