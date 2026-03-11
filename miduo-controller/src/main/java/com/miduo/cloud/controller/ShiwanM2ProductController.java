package com.miduo.cloud.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miduo.cloud.common.config.ShiwanM2SettingsDto;
import com.miduo.cloud.common.config.ShiwanM2SettingsFileLoader;
import com.miduo.cloud.common.dto.ApiResult;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 石湾 2 号机产品列表接口（代理开放平台或返回示例数据）。
 * 完整实现时从 2 号机系统配置读取 base URL 与签名，请求开放平台 /api/sign/md.shop.products/v1/list。
 */
@RestController
@RequestMapping("/api/shiwan-m2/products")
@CrossOrigin
public class ShiwanM2ProductController {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int DEFAULT_PAGE_SIZE = 10;

    private static final List<Map<String, String>> MOCK_PRODUCTS = new ArrayList<>();
    static {
        addMock("石湾酒 52度 500ml", "P001");
        addMock("石湾酒 38度 500ml", "P002");
        addMock("石湾酒 42度 250ml", "P003");
        addMock("石湾酒 52度 250ml", "P004");
    }
    private static void addMock(String name, String pronumber) {
        Map<String, String> m = new HashMap<>();
        m.put("name", name);
        m.put("pronumber", pronumber);
        m.put("productid", pronumber);
        MOCK_PRODUCTS.add(m);
    }

    @GetMapping
    public ApiResult<Map<String, Object>> list(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int pagesize,
                                               @RequestParam(required = false) String keyword) {
        Map<String, Object> body = new HashMap<>();
        body.put("page", page);
        body.put("pagesize", pagesize);
        Map<String, Object> query = new HashMap<>();
        query.put("name", keyword != null ? keyword : "");
        query.put("pronumber", keyword != null ? keyword : "");
        body.put("query", query);
        return query(body);
    }

    /**
     * 产品列表（分页、查询条件），优先代理开放平台，失败时降级到本地 mock。
     * POST /api/shiwan-m2/products/query
     */
    @PostMapping("/query")
    public ApiResult<Map<String, Object>> query(@RequestBody(required = false) Map<String, Object> body) {
        int page = toInt(body != null ? body.get("page") : null, 1);
        int pagesize = toInt(body != null ? body.get("pagesize") : null, DEFAULT_PAGE_SIZE);
        if (page <= 0) page = 1;
        if (pagesize <= 0) pagesize = DEFAULT_PAGE_SIZE;

        String keyword = "";
        if (body != null && body.get("keyword") != null) {
            keyword = String.valueOf(body.get("keyword")).trim();
        }
        Object queryObj = body != null ? body.get("query") : null;
        if (keyword.isEmpty() && queryObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<Object, Object> qm = (Map<Object, Object>) queryObj;
            Object byName = qm.get("name");
            if (byName == null || String.valueOf(byName).trim().isEmpty()) {
                byName = qm.get("pronumber");
            }
            if (byName != null && !String.valueOf(byName).trim().isEmpty()) {
                keyword = String.valueOf(byName).trim();
            }
        }

        ApiResult<Map<String, Object>> remoteResult = queryFromOpenPlatform(page, pagesize, body, keyword);
        if (remoteResult != null && remoteResult.getCode() != null && remoteResult.getCode() == 200) {
            return remoteResult;
        }

        // 代理失败时降级到 mock，保证主流程可用
        List<Map<String, String>> filtered = MOCK_PRODUCTS;
        if (keyword != null && !keyword.trim().isEmpty()) {
            String k = keyword.trim().toLowerCase();
            filtered = MOCK_PRODUCTS.stream()
                    .filter(p -> (p.get("name") != null && p.get("name").toLowerCase().contains(k))
                            || (p.get("pronumber") != null && p.get("pronumber").toLowerCase().contains(k)))
                    .collect(Collectors.toList());
        }
        int total = filtered.size();
        int from = (page - 1) * pagesize;
        int to = Math.min(from + pagesize, total);
        List<Map<String, String>> list = from < total ? filtered.subList(from, to) : new ArrayList<>();
        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("list", list);
        data.put("source", "mock");
        return ApiResult.success("查询成功", data);
    }

    private ApiResult<Map<String, Object>> queryFromOpenPlatform(int page, int pagesize, Map<String, Object> requestBody, String keyword) {
        ShiwanM2SettingsDto settings = ShiwanM2SettingsFileLoader.load();
        ShiwanM2SettingsDto.ApiConfig api = settings != null ? settings.getApi() : null;
        String baseUrl = api != null ? trimToEmpty(api.getBaseUrl()) : "";
        String productsPath = api != null ? trimToEmpty(api.getProductsListPath()) : "";
        if (productsPath.isEmpty()) {
            productsPath = "/api/sign/md.shop.products/v1/list";
        }
        if (baseUrl.isEmpty()) {
            return null;
        }
        String endpoint = joinUrl(baseUrl, productsPath);

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("page", page);
            payload.put("pagesize", pagesize);

            Object query = requestBody != null ? requestBody.get("query") : null;
            if (query instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<Object, Object> queryMap = (Map<Object, Object>) query;
                Map<String, Object> q = new HashMap<>();
                for (Map.Entry<Object, Object> entry : queryMap.entrySet()) {
                    if (entry.getKey() != null) {
                        q.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                }
                payload.put("query", q);
            } else {
                Map<String, Object> q = new HashMap<>();
                q.put("name", keyword != null ? keyword : "");
                q.put("pronumber", keyword != null ? keyword : "");
                payload.put("query", q);
            }
            if (requestBody != null && requestBody.containsKey("sort")) {
                payload.put("sort", requestBody.get("sort"));
            }

            String requestJson = JSON.writeValueAsString(payload);
            HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            if (api != null) {
                if (!trimToEmpty(api.getAppKey()).isEmpty()) {
                    conn.setRequestProperty("appKey", api.getAppKey());
                    conn.setRequestProperty("AppKey", api.getAppKey());
                }
                if (!trimToEmpty(api.getAppSecret()).isEmpty()) {
                    conn.setRequestProperty("appSecret", api.getAppSecret());
                    conn.setRequestProperty("AppSecret", api.getAppSecret());
                }
            }
            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestJson.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            String text = readBody(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
            JsonNode root = JSON.readTree(text);
            if (root == null) {
                return ApiResult.error("开放平台产品接口返回为空");
            }

            String errorCode = root.has("error_code") ? root.get("error_code").asText() : "";
            if (!"0".equals(errorCode) && !"200".equals(errorCode) && !errorCode.isEmpty()) {
                String msg = root.has("error_msg") ? root.get("error_msg").asText() : "开放平台返回失败";
                return ApiResult.error(msg);
            }

            JsonNode dataNode = root.path("return_data");
            int total = dataNode.has("total") ? dataNode.get("total").asInt() : 0;
            List<Map<String, String>> list = new ArrayList<>();
            JsonNode datalist = dataNode.path("datalist");
            if (datalist.isArray()) {
                for (JsonNode item : datalist) {
                    Map<String, String> row = new HashMap<>();
                    row.put("name", item.has("name") ? item.get("name").asText("") : "");
                    row.put("pronumber", item.has("pronumber") ? item.get("pronumber").asText("") : "");
                    row.put("productid", item.has("productid") ? item.get("productid").asText("") : "");
                    list.add(row);
                }
            }
            Map<String, Object> data = new HashMap<>();
            data.put("total", total);
            data.put("list", list);
            data.put("source", "openPlatform");
            return ApiResult.success("查询成功", data);
        } catch (Exception e) {
            return ApiResult.error("开放平台产品接口调用失败：" + e.getMessage());
        }
    }

    private static String joinUrl(String baseUrl, String path) {
        String b = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String p = path.startsWith("/") ? path : "/" + path;
        return b + p;
    }

    private static String trimToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    private static int toInt(Object val, int defaultVal) {
        if (val == null) return defaultVal;
        if (val instanceof Number) return ((Number) val).intValue();
        try {
            return Integer.parseInt(String.valueOf(val));
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private static String readBody(InputStream is) throws Exception {
        if (is == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
}
