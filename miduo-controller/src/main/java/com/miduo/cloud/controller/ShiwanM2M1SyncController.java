package com.miduo.cloud.controller;

import com.miduo.cloud.application.shiwan.M1TCodeSyncService;
import com.miduo.cloud.common.dto.ApiResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 石湾 2 号机：1 号机 T_Code 同步控制接口。
 * 前端开始采集时调用 /start，停止采集时调用 /stop。
 * 依赖 shiwan.m2.m1-sync.enabled=true 才有 M1TCodeSyncService Bean。
 */
@RestController
@RequestMapping("/api/shiwan-m2/m1-sync")
@CrossOrigin
public class ShiwanM2M1SyncController {

    /**
     * 允许 M1TCodeSyncService 不存在（未启用 shiwan.m2.m1-sync.enabled=true 时），
     * 接口仍可正常响应而不会 500。
     */
    @Autowired(required = false)
    private M1TCodeSyncService m1TCodeSyncService;

    /**
     * 启动 1 号机同步：前端开始采集后调用。
     * POST /api/shiwan-m2/m1-sync/start
     */
    @PostMapping("/start")
    public ApiResult<Map<String, Object>> start() {
        if (m1TCodeSyncService == null) {
            return ApiResult.error("1号机同步未启用，请在后端配置 shiwan.m2.m1-sync.enabled=true");
        }
        m1TCodeSyncService.startSync();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("active", true);
        return ApiResult.success("1号机同步已启动", data);
    }

    /**
     * 停止 1 号机同步：前端停止采集后调用。
     * POST /api/shiwan-m2/m1-sync/stop
     */
    @PostMapping("/stop")
    public ApiResult<Map<String, Object>> stop() {
        if (m1TCodeSyncService == null) {
            return ApiResult.error("1号机同步未启用");
        }
        m1TCodeSyncService.stopSync();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("active", false);
        return ApiResult.success("1号机同步已停止", data);
    }

    /**
     * 查询同步状态。
     * GET /api/shiwan-m2/m1-sync/status
     */
    @GetMapping("/status")
    public ApiResult<Map<String, Object>> status() {
        Map<String, Object> data = new LinkedHashMap<>();
        if (m1TCodeSyncService == null) {
            data.put("enabled", false);
            data.put("active", false);
        } else {
            data.put("enabled", true);
            data.put("active", m1TCodeSyncService.isSyncActive());
        }
        return ApiResult.success(data);
    }
}
