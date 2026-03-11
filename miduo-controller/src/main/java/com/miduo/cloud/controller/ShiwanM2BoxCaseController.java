package com.miduo.cloud.controller;

import com.miduo.cloud.application.shiwan.ShiwanM2BoxCaseService;
import com.miduo.cloud.common.dto.ApiResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 石湾 2 号机盒箱关联、成垛、统计接口。
 */
@RestController
@RequestMapping("/api/shiwan-m2")
@CrossOrigin
public class ShiwanM2BoxCaseController {

    @Autowired
    private ShiwanM2BoxCaseService shiwanM2BoxCaseService;

    /**
     * 盒箱关联：N 个盒码 + 1 个箱码，校验后写入；满 M 箱则成垛。
     * POST /api/shiwan-m2/box-case/associate
     * Body: orderNo, productNo, boxCodes[], caseCode, boxesPerCase, boxesPerPallet
     */
    @PostMapping("/box-case/associate")
    public ApiResult<Map<String, Object>> associate(@RequestBody Map<String, Object> body) {
        String orderNo = getStr(body, "orderNo");
        String productNo = getStr(body, "productNo");
        String caseCode = getStr(body, "caseCode");
        @SuppressWarnings("unchecked")
        List<String> boxCodes = body != null && body.get("boxCodes") instanceof List
                ? ((List<?>) body.get("boxCodes")).stream().map(o -> o != null ? o.toString().trim() : "").collect(Collectors.toList())
                : null;
        int boxesPerCase = toInt(body != null ? body.get("boxesPerCase") : null, 4);
        int boxesPerPallet = toInt(body != null ? body.get("boxesPerPallet") : null, 70);
        return shiwanM2BoxCaseService.associate(orderNo, productNo, boxCodes, caseCode, boxesPerCase, boxesPerPallet);
    }

    /**
     * 强制满垛：将当前未成垛的箱绑定到新虚拟垛标。
     * POST /api/shiwan-m2/box-case/force-full-pallet
     * Body: orderNo, currentCaseCount（可选，不传则按当前未成垛箱数）
     */
    @PostMapping("/box-case/force-full-pallet")
    public ApiResult<Map<String, Object>> forceFullPallet(@RequestBody Map<String, Object> body) {
        String orderNo = getStr(body, "orderNo");
        Integer currentCaseCount = body != null && body.get("currentCaseCount") != null
                ? toInt(body.get("currentCaseCount"), null) : null;
        return shiwanM2BoxCaseService.forceFullPallet(orderNo, currentCaseCount);
    }

    /**
     * 已生产垛数：按订单号统计成垛数。
     * GET /api/shiwan-m2/stats/produced-pallet-count?orderNo=xxx
     */
    @GetMapping("/stats/produced-pallet-count")
    public ApiResult<Integer> producedPalletCount(@RequestParam String orderNo) {
        int count = shiwanM2BoxCaseService.getProducedPalletCount(orderNo);
        return ApiResult.success("查询成功", count);
    }

    /**
     * 总剔除数：按订单号统计 RejectRecord 条数。
     * GET /api/shiwan-m2/stats/reject-count?orderNo=xxx
     */
    @GetMapping("/stats/reject-count")
    public ApiResult<Integer> rejectCount(@RequestParam String orderNo) {
        int count = shiwanM2BoxCaseService.getRejectCount(orderNo);
        return ApiResult.success("查询成功", count);
    }

    /**
     * 当前垛内箱数：未成垛的箱数（用于前端展示）。
     * GET /api/shiwan-m2/box-case/current-cases?orderNo=xxx
     */
    @GetMapping("/box-case/current-cases")
    public ApiResult<Integer> currentCases(@RequestParam String orderNo) {
        int count = shiwanM2BoxCaseService.countCurrentCasesInPallet(orderNo);
        return ApiResult.success("查询成功", count);
    }

    private static String getStr(Map<String, Object> body, String key) {
        if (body == null) return null;
        Object v = body.get(key);
        return v != null ? v.toString().trim() : null;
    }

    private static int toInt(Object o, int defaultVal) {
        if (o == null) return defaultVal;
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private static Integer toInt(Object o, Integer defaultVal) {
        if (o == null) return defaultVal;
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}
