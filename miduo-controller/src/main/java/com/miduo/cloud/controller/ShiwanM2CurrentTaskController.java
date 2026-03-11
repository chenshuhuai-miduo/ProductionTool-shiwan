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
     * 暂存：将当前任务下未成垛数据标记为 Status=0。
     * POST /api/shiwan-m2/current-task/mark-unfinished
     * Body: orderNo
     */
    @PostMapping("/mark-unfinished")
    public ApiResult<String> markUnfinished(@RequestBody Map<String, Object> body) {
        String orderNo = body != null && body.get("orderNo") != null ? body.get("orderNo").toString().trim() : null;
        return shiwanM2CurrentTaskService.markUnfinished(orderNo);
    }

    /**
     * 启动恢复：查询存在 Status=0 未成垛数据的订单列表。
     * GET /api/shiwan-m2/current-task/unfinished
     */
    @GetMapping("/unfinished")
    public ApiResult<List<Map<String, Object>>> getUnfinishedOrders() {
        return shiwanM2CurrentTaskService.getUnfinishedOrders();
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
