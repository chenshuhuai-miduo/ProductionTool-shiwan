package com.miduo.cloud.controller;

import com.miduo.cloud.application.shiwan.ShiwanM2TcpCaptureService;
import com.miduo.cloud.common.dto.ApiResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 石湾 2 号机 TCP 采集控制接口。
 * 前端开始采集后调用 /start；停止采集时调用 /stop；
 * 前端定时轮询 /status 更新计数，轮询 /events 更新数据日志。
 * 依赖 shiwan.m2.tcp-capture.enabled=true 才会注入 ShiwanM2TcpCaptureService。
 */
@RestController
@RequestMapping("/api/shiwan-m2/capture")
@CrossOrigin
public class ShiwanM2CaptureController {

    /** required=false：未启用时接口仍可正常响应而不报 500 */
    @Autowired(required = false)
    private ShiwanM2TcpCaptureService captureService;

    /**
     * 启动 TCP 采集。
     * POST /api/shiwan-m2/capture/start
     * Body: { orderNo, productNo, boxesPerCase, boxesPerPallet }
     */
    @PostMapping("/start")
    public ApiResult<Map<String, Object>> start(@RequestBody Map<String, Object> body) {
        if (captureService == null) {
            return ApiResult.error("TCP采集服务未启用，请配置 shiwan.m2.tcp-capture.enabled=true");
        }
        String  orderNo       = str(body, "orderNo");
        String  productNo     = str(body, "productNo");
        int     boxesPerCase  = toInt(body.get("boxesPerCase"), 4);
        int     boxesPerPallet = toInt(body.get("boxesPerPallet"), 70);

        if (orderNo == null || orderNo.isEmpty()) {
            return ApiResult.error("orderNo 不能为空");
        }

        String err = captureService.start(orderNo, productNo, boxesPerCase, boxesPerPallet);
        if (err != null) {
            return ApiResult.error(err);
        }
        return ApiResult.success("TCP采集已启动", captureService.getStatus());
    }

    /**
     * 停止 TCP 采集。
     * POST /api/shiwan-m2/capture/stop
     */
    @PostMapping("/stop")
    public ApiResult<Map<String, Object>> stop() {
        if (captureService == null) {
            return ApiResult.error("TCP采集服务未启用");
        }
        captureService.stop();
        return ApiResult.success("TCP采集已停止", captureService.getStatus());
    }

    /**
     * 查询采集状态（计数用）。
     * GET /api/shiwan-m2/capture/status
     * 返回: active, pendingBoxCodes, currentCasesInPallet, totalAssociations, boxesPerCase, boxesPerPallet
     */
    @GetMapping("/status")
    public ApiResult<Map<String, Object>> status() {
        if (captureService == null) {
            return ApiResult.error("TCP采集服务未启用");
        }
        return ApiResult.success(captureService.getStatus());
    }

    /**
     * 增量获取采集事件（数据日志用）。
     * GET /api/shiwan-m2/capture/events?lastSeq=0
     * 返回 seq > lastSeq 的事件列表，前端每次用最大 seq 做下次请求参数。
     * 事件 type: STARTED / STOPPED / BOX_CONNECTED / BOX_DISCONNECTED /
     *             CASE_CONNECTED / CASE_DISCONNECTED / BOX_CODE / CASE_CODE /
     *             ASSOCIATED / ASSOC_FAIL / BOX_ERROR / CASE_ERROR
     */
    @GetMapping("/events")
    public ApiResult<List<Map<String, Object>>> events(
            @RequestParam(value = "lastSeq", defaultValue = "0") long lastSeq) {
        if (captureService == null) {
            return ApiResult.error("TCP采集服务未启用");
        }
        return ApiResult.success(captureService.getEvents(lastSeq));
    }

    /**
     * 从数据库恢复未关联盒码到内存队列（软件重启后用于继续未完成采集）。
     * POST /api/shiwan-m2/capture/restore-queue
     * Body: { orderNo }
     */
    @PostMapping("/restore-queue")
    public ApiResult<Map<String, Object>> restoreQueue(@RequestBody Map<String, Object> body) {
        if (captureService == null) {
            return ApiResult.error("TCP采集服务未启用");
        }
        String orderNo = body != null && body.get("orderNo") != null ? body.get("orderNo").toString().trim() : null;
        if (orderNo == null || orderNo.isEmpty()) {
            return ApiResult.error("orderNo 不能为空");
        }
        int count = captureService.restoreBoxQueueFromDb(orderNo);
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("restoredCount", count);
        data.put("orderNo",       orderNo);
        return ApiResult.success("队列恢复完成，共恢复 " + count + " 个盒码", data);
    }

    // ---- helpers ----

    private static String str(Map<String, Object> m, String key) {
        Object v = m != null ? m.get(key) : null;
        return v != null ? v.toString().trim() : null;
    }

    private static int toInt(Object o, int def) {
        if (o == null) return def;
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(o.toString()); } catch (NumberFormatException e) { return def; }
    }
}
