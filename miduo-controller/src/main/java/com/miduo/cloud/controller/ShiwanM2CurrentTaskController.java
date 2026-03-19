package com.miduo.cloud.controller;

import com.miduo.cloud.application.shiwan.ShiwanM2CurrentTaskService;
import com.miduo.cloud.common.dto.ApiResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 石湾 2 号机当前任务接口（门禁通过后写当前任务、查询当前任务）。
 */
@RestController
@RequestMapping("/api/shiwan-m2/current-task")
@CrossOrigin
public class ShiwanM2CurrentTaskController {

    @Autowired
    private ShiwanM2CurrentTaskService shiwanM2CurrentTaskService;

    /**
     * 开始采集：写当前任务（ProductionOrder + ProductionOrderDetail），返回任务标识。
     * POST /api/shiwan-m2/current-task/start
     * Body: orderNo, productNo, productName, boxesPerPallet, boxesPerCase
     */
    @PostMapping("/start")
    public ApiResult<Map<String, Object>> start(@RequestBody Map<String, Object> body) {
        String orderNo = body != null && body.get("orderNo") != null ? body.get("orderNo").toString().trim() : null;
        String productNo = body != null && body.get("productNo") != null ? body.get("productNo").toString().trim() : null;
        String productName = body != null && body.get("productName") != null ? body.get("productName").toString() : "";
        Integer boxesPerPallet = body != null && body.get("boxesPerPallet") != null ? toInt(body.get("boxesPerPallet"), 70) : 70;
        Integer boxesPerCase = body != null && body.get("boxesPerCase") != null ? toInt(body.get("boxesPerCase"), 4) : 4;
        return shiwanM2CurrentTaskService.startCurrentTask(orderNo, productNo, productName, boxesPerPallet, boxesPerCase);
    }

    /**
     * 查询当前任务（OrderStatus=1 的最新订单）。
     * GET /api/shiwan-m2/current-task
     */
    @GetMapping
    public ApiResult<Map<String, Object>> getCurrentTask() {
        return shiwanM2CurrentTaskService.getCurrentTask();
    }

    /**
     * 按箱码查任务：返回订单号、产品编号、未成垛箱数。
     * GET /api/shiwan-m2/current-task/by-case-code?caseCode=xxx
     */
    @GetMapping("/by-case-code")
    public ApiResult<Map<String, Object>> getTaskByCaseCode(@RequestParam String caseCode) {
        return shiwanM2CurrentTaskService.getTaskByCaseCode(caseCode);
    }

    /**
     * 暂存：将当前任务下未成垛数据标记为 Status=3（未成垛）。
     * POST /api/shiwan-m2/current-task/mark-unfinished
     * Body: orderNo
     */
    @PostMapping("/mark-unfinished")
    public ApiResult<String> markUnfinished(@RequestBody Map<String, Object> body) {
        String orderNo = body != null && body.get("orderNo") != null ? body.get("orderNo").toString().trim() : null;
        return shiwanM2CurrentTaskService.markUnfinished(orderNo);
    }

    /**
     * 启动恢复：查询存在 Status=3（未成垛）数据的订单列表。
     * GET /api/shiwan-m2/current-task/unfinished
     */
    @GetMapping("/unfinished")
    public ApiResult<List<Map<String, Object>>> getUnfinishedOrders() {
        return shiwanM2CurrentTaskService.getUnfinishedOrders();
    }

    /**
     * 提取工单未成垛：按箱码查询未成垛记录，返回订单号、产品编号、当前垛已有箱数。
     * GET /api/shiwan-m2/current-task/unfinished-by-box-code?boxCode=xxx
     */
    @GetMapping("/unfinished-by-box-code")
    public ApiResult<Map<String, Object>> getUnfinishedByBoxCode(@RequestParam String boxCode) {
        return shiwanM2CurrentTaskService.queryUnfinishedByBoxCode(boxCode);
    }

    /**
     * 建议生产单号：prefix+001~999 中第一个未在 ProductionOrder 使用的单号。
     * GET /api/shiwan-m2/current-task/suggest-order-no?prefix=YYYYMMDD
     */
    @GetMapping("/suggest-order-no")
    public ApiResult<String> suggestOrderNo(@RequestParam String prefix) {
        return ApiResult.success("建议单号", shiwanM2CurrentTaskService.suggestOrderNo(prefix));
    }

    /**
     * 检查生产单号是否在 ProductionOrder 中已有记录。
     * GET /api/shiwan-m2/current-task/exists?orderNo=xxx
     */
    @GetMapping("/exists")
    public ApiResult<Boolean> existsOrderNo(@RequestParam String orderNo) {
        return ApiResult.success("查询成功", shiwanM2CurrentTaskService.existsOrderNo(orderNo));
    }

    private static Integer toInt(Object o, int defaultVal) {
        if (o == null) return defaultVal;
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}
