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
     * 本地模糊查询产品
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
}
