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

    /**
     * P02-04 数据查询：输入任意层级码，查所属垛全部关联链。
     * GET /api/shiwan-m2/code/query?code=xxx
     */
    @GetMapping("/code/query")
    public ApiResult<Map<String, Object>> queryCode(@RequestParam String code) {
        if (code == null || code.trim().isEmpty()) {
            return ApiResult.error(400, "码值不能为空");
        }
        Map<String, Object> data = shiwanM2BoxCaseService.queryCodeAssociation(code);
        if (data == null || data.isEmpty()) {
            return ApiResult.error(404, "未找到该码信息");
        }
        return ApiResult.success("查询成功", data);
    }

    /**
     * P02-05 取消关联（物理删除）。
     * POST /api/shiwan-m2/code/cancel
     * Body: { caseCode: "箱码" }
     */
    @PostMapping("/code/cancel")
    public ApiResult<Boolean> cancelCode(@RequestBody Map<String, Object> body) {
        String caseCode = getStr(body, "caseCode");
        return shiwanM2BoxCaseService.cancelByCaseCode(caseCode);
    }

    /**
     * P02-06 数据替换。
     * POST /api/shiwan-m2/code/replace
     * Body: { oldCode: "", newCode: "", reason: "" }
     */
    @PostMapping("/code/replace")
    public ApiResult<Boolean> replaceCode(@RequestBody Map<String, Object> body) {
        String oldCode = getStr(body, "oldCode");
        String newCode = getStr(body, "newCode");
        String reason = getStr(body, "reason");
        return shiwanM2BoxCaseService.replaceCode(oldCode, newCode, reason);
    }

    /**
     * P02-07 生产统计汇总（垛数/箱数/盒数/剔除数）。
     * GET /api/shiwan-m2/stats/production-summary
     */
    @GetMapping("/stats/production-summary")
    public ApiResult<Map<String, Object>> productionSummary(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String orderNo) {
        return ApiResult.success("查询成功",
                shiwanM2BoxCaseService.getProductionSummary(startDate, endDate, orderNo));
    }

    /**
     * P02-07 垛码列表弹窗数据。
     * GET /api/shiwan-m2/stats/pallet-list
     */
    @GetMapping("/stats/pallet-list")
    public ApiResult<Map<String, Object>> palletList(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String palletCode,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return ApiResult.success("查询成功",
                shiwanM2BoxCaseService.getPalletList(startDate, endDate, orderNo, palletCode, page, pageSize));
    }

    /**
     * P02-07 剔除记录弹窗数据。
     * GET /api/shiwan-m2/stats/reject-records
     */
    @GetMapping("/stats/reject-records")
    public ApiResult<Map<String, Object>> rejectRecords(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String caseCode,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return ApiResult.success("查询成功",
                shiwanM2BoxCaseService.getRejectRecords(startDate, endDate, orderNo, caseCode, page, pageSize));
    }

    /**
     * P02-07 上传统计列表（仅成功/异常）。
     * GET /api/shiwan-m2/stats/upload-records
     */
    @GetMapping("/stats/upload-records")
    public ApiResult<Map<String, Object>> uploadRecords(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return ApiResult.success("查询成功",
                shiwanM2BoxCaseService.getUploadRecords(startDate, endDate, orderNo, status, page, pageSize));
    }

    /**
     * P02-02 手工采集码校验：热表 + CodeRelationUpload 重码检查。
     * POST /api/shiwan-m2/manual/validate-code
     * Body: { code: "码值", packageType: 1（瓶码）或 2（盒码） }
     */
    @PostMapping("/manual/validate-code")
    public ApiResult<Map<String, Object>> validateManualCode(@RequestBody Map<String, Object> body) {
        String code = getStr(body, "code");
        int packageType = toInt(body != null ? body.get("packageType") : null, 0);
        return shiwanM2BoxCaseService.validateManualCode(code, packageType);
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
