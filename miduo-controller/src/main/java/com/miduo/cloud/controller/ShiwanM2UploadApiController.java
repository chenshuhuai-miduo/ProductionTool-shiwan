package com.miduo.cloud.controller;

import com.miduo.cloud.application.shiwan.ShiwanM2BoxCaseService;
import com.miduo.cloud.common.dto.ApiResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 石湾 2 号机数据上传管理接口（P02-08）。
 * <p>
 * 提供：垛码上传状态查询、设为未上传/已上传、手动触发批量上传。
 * </p>
 */
@RestController
@RequestMapping("/api/shiwan-m2/upload")
@CrossOrigin
public class ShiwanM2UploadApiController {

    @Autowired
    private ShiwanM2BoxCaseService boxCaseService;

    /**
     * 按垛码查询上传状态。
     * GET /api/shiwan-m2/upload/pallet-status?palletCode=xxx
     *
     * 返回 data 字段：
     *   isUpload   : 0=未上传, 1=已上传, 2=上传失败, 3=上传中
     *   uploadTime : 上传时间（已上传时有值）
     *   boxCount   : 本垛箱数
     *   msg        : 失败原因（失败时有值）
     */
    @GetMapping("/pallet-status")
    public ApiResult<Map<String, Object>> palletStatus(@RequestParam String palletCode) {
        if (palletCode == null || palletCode.trim().isEmpty()) {
            return ApiResult.error(400, "垛码不能为空");
        }
        Map<String, Object> status = boxCaseService.queryPalletStatus(palletCode.trim());
        if (status == null) {
            return ApiResult.error(404, "未找到该垛码的上传记录");
        }
        return ApiResult.success("查询成功", status);
    }

    /**
     * 将指定垛设为未上传（IsUpload=0）。
     * POST /api/shiwan-m2/upload/set-not-uploaded
     * Body: { "palletCode": "xxx" }
     */
    @PostMapping("/set-not-uploaded")
    public ApiResult<Void> setNotUploaded(@RequestBody Map<String, Object> body) {
        String palletCode = getStr(body, "palletCode");
        if (palletCode == null) return ApiResult.error(400, "垛码不能为空");
        int rows = boxCaseService.setPalletNotUploaded(palletCode);
        if (rows == 0) return ApiResult.error(404, "未找到该垛码的上传记录");
        return ApiResult.success("已设为未上传", null);
    }

    /**
     * 将指定垛手动标记为已上传（IsUpload=1）。
     * POST /api/shiwan-m2/upload/set-uploaded
     * Body: { "palletCode": "xxx" }
     */
    @PostMapping("/set-uploaded")
    public ApiResult<Void> setUploaded(@RequestBody Map<String, Object> body) {
        String palletCode = getStr(body, "palletCode");
        if (palletCode == null) return ApiResult.error(400, "垛码不能为空");
        int rows = boxCaseService.setPalletUploaded(palletCode);
        if (rows == 0) return ApiResult.error(404, "未找到该垛码的上传记录");
        return ApiResult.success("已标记为已上传", null);
    }

    /**
     * 手动触发批量上传：查找所有 IsUpload=0（待上传）的垛，逐垛串行调用虚拟垛标入库接口。
     * POST /api/shiwan-m2/upload/manual-upload
     * 方法立即返回，上传在后台异步执行，进度通过实时上传日志反馈。
     * 返回 pendingCount：本次找到的待上传垛数。
     */
    @PostMapping("/manual-upload")
    public ApiResult<Map<String, Object>> manualUpload() {
        int count = boxCaseService.startManualUpload();
        Map<String, Object> data = new HashMap<>();
        data.put("pendingCount", count);
        String msg = count == 0 ? "无待上传的垛数据" : "手动上传已启动，共 " + count + " 个待上传垛";
        return ApiResult.success(msg, data);
    }

    /**
     * 查询所有待上传垛列表（IsUpload=0）。
     * GET /api/shiwan-m2/upload/pending-list
     */
    @GetMapping("/pending-list")
    public ApiResult<List<Map<String, Object>>> pendingList() {
        List<Map<String, Object>> list = boxCaseService.getPendingUploadPallets();
        return ApiResult.success("查询成功", list);
    }

    private static String getStr(Map<String, Object> body, String key) {
        if (body == null) return null;
        Object v = body.get(key);
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }
}
