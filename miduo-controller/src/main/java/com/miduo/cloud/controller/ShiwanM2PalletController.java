package com.miduo.cloud.controller;

import com.miduo.cloud.application.shiwan.VirtualPalletSequenceService;
import com.miduo.cloud.common.dto.ApiResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 石湾 2 号机成垛相关接口：生成虚拟垛标等。
 */
@RestController
@RequestMapping("/api/shiwan-m2/pallet")
@CrossOrigin
public class ShiwanM2PalletController {

    @Autowired
    private VirtualPalletSequenceService virtualPalletSequenceService;

    /**
     * 获取下一个虚拟垛标（用于成垛/强制满垛时生成 VirtualSerialNumber）。
     * 从 VirtualPalletSequence 表按日期+产线号取序号，结合配置的前缀、产线号生成。
     * GET /api/shiwan-m2/pallet/next-virtual-serial-number
     */
    @GetMapping("/next-virtual-serial-number")
    public ApiResult<String> getNextVirtualSerialNumber() {
        try {
            String code = virtualPalletSequenceService.generateNextVirtualSerialNumber();
            return ApiResult.success("生成成功", code);
        } catch (Exception e) {
            return ApiResult.error("生成虚拟垛标失败：" + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
    }
}
