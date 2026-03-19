package com.miduo.cloud.controller;

import com.miduo.cloud.application.shiwan.ProductSyncService;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.infrastructure.persistence.mybatis.po.ProductInfoPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 石湾 2 号机产品同步与查询
 */
@RestController
@RequestMapping("/api/shiwan-m2/products")
@CrossOrigin
public class ShiwanM2ProductController {

    @Autowired
    private ProductSyncService productSyncService;

    /**
     * 拉取远端产品并落库
     */
    @PostMapping("/sync")
    public ApiResult<Integer> syncProducts() {
        try {
            int count = productSyncService.syncProducts();
            return ApiResult.success("同步成功", count);
        } catch (Exception e) {
            return ApiResult.error("同步产品失败：" + e.getMessage());
        }
    }

    /**
     * 本地模糊查询产品（不分页，向下兼容）
     */
    @GetMapping("/search")
    public ApiResult<List<ProductInfoPO>> search(@RequestParam(value = "keyword", required = false) String keyword,
                                                 @RequestParam(value = "size", required = false, defaultValue = "20") Integer size) {
        try {
            int realSize = size != null && size > 0 ? size : 20;
            List<ProductInfoPO> list = productSyncService.search(keyword, realSize);
            return ApiResult.success("查询成功", list);
        } catch (Exception e) {
            return ApiResult.error("查询产品失败：" + e.getMessage());
        }
    }

    /**
     * 本地模糊查询产品（分页）
     * GET /api/shiwan-m2/products/search-page?keyword=xxx&page=1&pageSize=10
     */
    @GetMapping("/search-page")
    public ApiResult<java.util.Map<String, Object>> searchPage(
            @RequestParam(value = "keyword",  required = false) String keyword,
            @RequestParam(value = "page",     required = false, defaultValue = "1")  Integer page,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        try {
            int realPage = page     != null && page     > 0 ? page     : 1;
            int realSize = pageSize != null && pageSize > 0 ? pageSize : 10;
            java.util.Map<String, Object> result = productSyncService.searchPage(keyword, realPage, realSize);
            return ApiResult.success("查询成功", result);
        } catch (Exception e) {
            return ApiResult.error("查询产品失败：" + e.getMessage());
        }
    }
}
