package com.miduo.cloud.frontend.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * HTTP请求工具类
 * 用于JavaFX前端调用Spring Boot后端接口
 */
public class HttpUtil {
    
    /**
     * 默认的后端服务地址
     */
    private static final String BASE_URL = "http://localhost:8080";
    
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
     * 请求配置
     */
    private static final RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(5000)        // 连接超时5秒
            .setSocketTimeout(10000)        // 读取超时10秒
            .setConnectionRequestTimeout(3000)  // 从连接池获取连接超时3秒
            .build();
    
    /**
     * GET请求
     * @param url 请求URL（相对路径）
     * @return 响应字符串
     */
    public static String doGet(String url) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(BASE_URL + url);
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
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(BASE_URL + url);
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
        HttpPut httpPut = new HttpPut(BASE_URL + url);
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
     * DELETE请求
     * @param url 请求URL（相对路径）
     * @return 响应字符串
     */
    public static String doDelete(String url) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpDelete httpDelete = new HttpDelete(BASE_URL + url);
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
}

