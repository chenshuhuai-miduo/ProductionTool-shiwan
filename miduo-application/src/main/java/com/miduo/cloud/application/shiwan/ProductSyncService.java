package com.miduo.cloud.application.shiwan;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miduo.cloud.common.config.ShiwanM2SettingsDto;
import com.miduo.cloud.common.config.ShiwanM2SettingsFileLoader;
import com.miduo.cloud.infrastructure.mapper.ProductInfoMapper;
import com.miduo.cloud.infrastructure.persistence.mybatis.po.ProductInfoPO;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 拉取产品列表并落库。
 */
@Service
public class ProductSyncService {

    @Resource
    private ProductInfoMapper productInfoMapper;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    //private static final String APPID = "您的APPID";
    //private static final String SECRET = "您的SECRET";

    private static String getSign(String time, String nonce, String secret, Map<String, Object> data) throws Exception {
        // 对数据进行排序
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

        // 组合签名字符串
        String signStr = time + nonce + secret + params;

        // MD5加密
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] bytes = md.digest(signStr.getBytes());

        // 转换为大写的十六进制字符串
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }

        return result.toString();
    }

    private static String sendPostRequest(String urlStr, Map<String, String> headers, String data) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        StringBuilder response = new StringBuilder();

        try {
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            // 设置请求头
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }

            conn.setDoOutput(true);
            conn.setDoInput(true);

            // 写入请求体
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = data.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // 读取响应内容
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return response.toString();
    }

    public int syncProducts() throws Exception {
        ShiwanM2SettingsDto cfg = ShiwanM2SettingsFileLoader.load();
        String baseUrl = trim(cfg.getApi().getBaseUrl());
        String path = trim(cfg.getApi().getProductsListPath());
        if (baseUrl == null || path == null) {
            throw new RuntimeException("缺少开放平台配置：baseUrl 或 productsListPath");
        }
        String appId = trim(cfg.getApi().getAppId());
        String appSecret = trim(cfg.getApi().getAppSecret());
        if (appId == null || appSecret == null) {
            throw new RuntimeException("缺少开放平台配置：appId 或 appSecret");
        }
        String url = baseUrl + path;
        String queryJson = "{\"prostate\":\"0\",\"noprotype\":\"0\"}";
        int pageSize = 2000;
        String sort = "";

        // 分页循环拉取
        List<ProductInfoPO> allItems = new ArrayList<>();
        int currentPage = 1;
        while (true) {
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("page", currentPage);
            requestData.put("pagesize", pageSize);
            requestData.put("query", queryJson);
            requestData.put("sort", sort);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String time = sdf.format(new Date());
            String nonce = "123";
            String sign = getSign(time, nonce, appSecret, requestData);

            Map<String, String> headers = new HashMap<>();
            headers.put("timestamp", time);
            headers.put("appid", appId);
            headers.put("nonce", nonce);
            headers.put("sign", sign);
            headers.put("Content-type", "application/json");

            String jsonStr = MAPPER.writeValueAsString(requestData);
            String response = sendPostRequest(url, headers, jsonStr);
            System.out.println("[产品同步] 第" + currentPage + "页响应: " + response);

            List<ProductInfoPO> pageItems = parseItems(response);
            allItems.addAll(pageItems);

            if (pageItems.size() < pageSize) {
                break;
            }
            currentPage++;
        }

        if (allItems.isEmpty()) return 0;

        // 全量覆盖：先清空本地产品表，再写入所有页汇总数据
        productInfoMapper.delete(new QueryWrapper<>());
        int total = 0;
        int batchSize = 500;
        for (int i = 0; i < allItems.size(); i += batchSize) {
            List<ProductInfoPO> sub = allItems.subList(i, Math.min(i + batchSize, allItems.size()));
            productInfoMapper.batchUpsert(sub);
            total += sub.size();
        }
        System.out.println("[产品同步] 共同步 " + total + " 条产品数据（共" + currentPage + "页）");
        return total;
    }

    /**
     * 本地模糊搜索产品（不分页，向下兼容）
     */
    public List<ProductInfoPO> search(String keyword, int size) {
        QueryWrapper<ProductInfoPO> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = "%" + keyword.trim() + "%";
            qw.lambda().like(ProductInfoPO::getProductName, kw).or().like(ProductInfoPO::getProductNo, kw);
        }
        qw.last("LIMIT " + Math.max(size, 1));
        return productInfoMapper.selectList(qw);
    }

    /**
     * 本地模糊搜索产品（支持分页），返回 { list, total, page, pageSize }
     */
    public java.util.Map<String, Object> searchPage(String keyword, int page, int pageSize) {
        int realPage = Math.max(page, 1);
        int realSize = Math.max(pageSize, 1);
        int offset   = (realPage - 1) * realSize;

        QueryWrapper<ProductInfoPO> countQw = new QueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = "%" + keyword.trim() + "%";
            countQw.lambda().like(ProductInfoPO::getProductName, kw).or().like(ProductInfoPO::getProductNo, kw);
        }
        Long total = productInfoMapper.selectCount(countQw);

        QueryWrapper<ProductInfoPO> listQw = new QueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = "%" + keyword.trim() + "%";
            listQw.lambda().like(ProductInfoPO::getProductName, kw).or().like(ProductInfoPO::getProductNo, kw);
        }
        listQw.last("LIMIT " + realSize + " OFFSET " + offset);
        List<ProductInfoPO> list = productInfoMapper.selectList(listQw);

        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("list",     list);
        result.put("total",    total != null ? total : 0);
        result.put("page",     realPage);
        result.put("pageSize", realSize);
        return result;
    }

    /** 解析单页接口响应，返回产品列表（不落库），供分页循环调用 */
    private List<ProductInfoPO> parseItems(String json) throws Exception {
        JsonNode root = MAPPER.readTree(json);
        if (root == null) return Collections.emptyList();
        if (root.has("return_code")) {
            String code = root.get("return_code").asText();
            if (!"0".equals(code)) {
                String msg = root.has("return_msg") ? root.get("return_msg").asText() : "接口返回错误";
                throw new RuntimeException(msg);
            }
        } else if (root.has("error_code") && root.get("error_code").asInt() != 0) {
            String msg = root.has("error_msg") ? root.get("error_msg").asText() : "接口返回错误";
            throw new RuntimeException(msg);
        }
        JsonNode data = root.has("return_data") ? root.get("return_data") : null;
        JsonNode list = data != null ? data.get("data_list") : null;
        if (list == null && data != null) {
            list = data.get("datalist");
        }
        if (list == null) return Collections.emptyList();
        List<JsonNode> nodes = new ArrayList<>();
        if (list.isArray()) {
            list.forEach(nodes::add);
        } else if (list.isObject()) {
            nodes.add(list);
        }
        List<ProductInfoPO> batch = new ArrayList<>(nodes.size());
        LocalDateTime now = LocalDateTime.now();
        for (JsonNode node : nodes) {
            String productNo = getText(node, "pronumber", getText(node, "productno", null));
            if (productNo == null || productNo.isEmpty()) continue;
            ProductInfoPO po = new ProductInfoPO();
            po.setProductNo(productNo);
            po.setProductName(getText(node, "name", getText(node, "productname", "")));
            po.setBarcode(getText(node, "barcode", ""));
            po.setSpec(getText(node, "proggstr1", ""));
            po.setProductId(node.has("productid") ? node.get("productid").asInt() : null);
            po.setCreatedAt(now);
            po.setUpdatedAt(now);
            batch.add(po);
        }
        return batch;
    }

    private String getText(JsonNode n, String field, String def) {
        if (n != null && n.has(field) && !n.get(field).isNull()) {
            return n.get(field).asText();
        }
        return def;
    }

    private String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String buildDataForSign(Map<String, Object> data) {
        // 对齐示例：TreeMap(CASE_INSENSITIVE_ORDER) 按 key 排序，拼接 key + value
        TreeMap<String, Object> sortedData = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        sortedData.putAll(data);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> e : sortedData.entrySet()) {
            String key = e.getKey();
            if (key == null || key.isEmpty()) continue;
            if ("sign".equalsIgnoreCase(key)) continue;
            Object val = e.getValue();
            sb.append(key).append(val == null ? "" : String.valueOf(val));
        }
        return sb.toString();
    }

    private String buildSign(String timestamp, String nonce, String secret, String data) {
        return md5Upper(timestamp + nonce + secret + data);
    }

    private String md5Upper(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] dig = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(dig.length * 2);
            for (byte b : dig) {
                String hex = Integer.toHexString(b & 0xff).toUpperCase();
                if (hex.length() < 2) sb.append('0');
                sb.append(hex);
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("生成签名失败", e);
        }
    }
}
