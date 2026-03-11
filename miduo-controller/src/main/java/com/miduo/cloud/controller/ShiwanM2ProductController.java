package com.miduo.cloud.controller;

import com.miduo.cloud.common.dto.ApiResult;
import org.springframework.web.bind.annotation.*;

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

    /**
     * 产品列表（分页、关键词）。
     * GET /api/shiwan-m2/products?page=1&pagesize=10&keyword=
     * 返回：{ code, message, data: { total, list: [ { name, pronumber } ] } }
     */
    @GetMapping
    public ApiResult<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pagesize,
            @RequestParam(required = false) String keyword) {
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
        return ApiResult.success("查询成功", data);
    }
}
