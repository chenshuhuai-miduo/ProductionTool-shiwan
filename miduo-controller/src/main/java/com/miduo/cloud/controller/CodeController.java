package com.miduo.cloud.controller;

import com.miduo.cloud.application.code.CodeApplicationService;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.entity.dto.code.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 码管理控制器
 * 完全按照原始业务逻辑实现所有接口（CodeController.java）
 */
@RestController
@RequestMapping("/api/code")
@CrossOrigin
public class CodeController {
    
    @Autowired
    private CodeApplicationService codeApplicationService;
    
    /**
     * 添加码
     */
    @PostMapping("/add")
    public ApiResult<GenerateSystemCodeResult> addCode(@RequestBody CodeAddRequest request) {
        return codeApplicationService.addCode(request);
    }
    
    /**
     * 删除码（清除上一个采集的箱码）
     */
    @DeleteMapping("/delete/{code}/{orderNo}/{productNo}")
    public ApiResult<Boolean> deleteCode(@PathVariable("code") String code, 
                                         @PathVariable("orderNo") String orderNo,
                                         @PathVariable("productNo") String productNo) {
        return codeApplicationService.deleteCode(code, orderNo, productNo);
    }
    
    /**
     * 根据箱码删除（数据上传页面使用）
     * 删除CodeRelationUpload表中的数据，并更新相关Qty和OrderCount
     */
    @DeleteMapping("/delete-by-box-code/{boxCode}")
    public ApiResult<Boolean> deleteByBoxCode(@PathVariable("boxCode") String boxCode) {
        return codeApplicationService.deleteByBoxCode(boxCode);
    }
    
    /**
     * 替换码
     */
    @PostMapping("/replace")
    public ApiResult<Boolean> replaceCode(@RequestBody CodeReplaceRequest request) {
        return codeApplicationService.replaceCode(request);
    }
    
    /**
     * 查询码信息（联表查询）
     * 支持查询箱码（SmallSerialNumber）或托盘码（BigSerialNumber）
     * 返回同一VirtualSerialNumber的所有未删除记录
     */
    @GetMapping("/query/{code}")
    public ApiResult<List<CodeQueryVO>> queryCode(@PathVariable("code") String code) {
        return codeApplicationService.queryCode(code);
    }
    
    /**
     * 码校验（第一台设备）
     */
    @PostMapping("/validate")
    public ApiResult<String> validateCodeSimple(@RequestBody CodeValidateRequest request) {
        return codeApplicationService.validateCode(request.getCode());
    }
    
    /**
     * 读码剔除校验（第一台设备-读码剔除模式）
     */
    @PostMapping("/validate-reject")
    public ApiResult<CodeRejectResult> validateCodeReject(@RequestBody CodeRejectRequest request) {
        return codeApplicationService.validateCodeReject(request);
    }
    
    /**
     * 箱码采集（第二台设备）
     */
    @PostMapping("/collect")
    public ApiResult<CodeCollectResult> collectBoxCode(@RequestBody CodeCollectRequest request) {
        return codeApplicationService.collectBoxCode(request);
    }
    
    /**
     * 托盘码关联（第三、四台设备）
     */
    @PostMapping("/associate-pallet")
    public ApiResult<PalletAssociateResult> associatePalletCode(@RequestBody PalletAssociateRequest request) {
        return codeApplicationService.associatePalletCode(request);
    }
    
    /**
     * 获取当前垛信息
     */
    @GetMapping("/current-pallet-info/by-task/{taskId}")
    public ApiResult<CurrentPalletInfoVO> getCurrentPalletInfoByTask(@PathVariable("taskId") Integer taskId) {
        return codeApplicationService.getCurrentPalletInfoByTaskId(taskId);
    }
    
    @GetMapping("/current-pallet-info/{orderNo}")
    @Deprecated
    public ApiResult<CurrentPalletInfoVO> getCurrentPalletInfo(@PathVariable("orderNo") String orderNo) {
        return codeApplicationService.getCurrentPalletInfo(orderNo);
    }
    
    /**
     * 调整当前箱数
     */
    @PostMapping("/adjust-box-count")
    public ApiResult<AdjustBoxCountResult> adjustBoxCount(@RequestBody AdjustBoxCountRequest request) {
        return codeApplicationService.adjustBoxCount(request);
    }
    
    /**
     * 强制满垛
     */
    @PostMapping("/force-pallet")
    public ApiResult<ForcePalletResult> forcePallet(@RequestBody ForcePalletRequest request) {
        return codeApplicationService.forcePallet(request);
    }
    
    /**
     * 删除本垛无码
     */
    @PostMapping("/delete-empty-codes")
    public ApiResult<DeleteEmptyCodesResult> deleteEmptyCodes(@RequestBody DeleteEmptyCodesRequest request) {
        return codeApplicationService.deleteEmptyCodes(request);
    }
    
    /**
     * 生成系统箱码
     */
    @PostMapping("/generate-system-code")
    public ApiResult<GenerateSystemCodeResult> generateSystemCode(@RequestBody GenerateSystemCodeRequest request) {
        return codeApplicationService.generateSystemCode(request);
    }
    
    /**
     * 无箱码采集
     */
    @PostMapping("/collect-no-box")
    public ApiResult<NoBoxCollectResult> collectNoBox(@RequestBody NoBoxCollectRequest request) {
        return codeApplicationService.collectNoBox(request);
    }
    
    /**
     * 获取订单已采集箱数
     */
    @GetMapping("/collected-count/{orderNo}")
    public ApiResult<Integer> getCollectedCount(@PathVariable("orderNo") String orderNo) {
        return codeApplicationService.getCollectedCount(orderNo);
    }
    
    /**
     * 获取订单已生产垛数
     * 统计该订单BigSerialNumber有值的记录中不同TagNo的数量
     */
    @GetMapping("/produced-pallet-count/{orderNo}")
    public ApiResult<Integer> getProducedPalletCount(@PathVariable("orderNo") String orderNo) {
        return codeApplicationService.getProducedPalletCount(orderNo);
    }
    
    /**
     * 获取产品已采集箱数（按OrderNo和ProductNo统计）
     * 用于主界面单位实时统计，因为一个订单可能包含多个产品
     */
    @GetMapping("/collected-count-by-product")
    public ApiResult<Integer> getCollectedCountByProduct(
            @RequestParam("orderNo") String orderNo,
            @RequestParam("productNo") String productNo) {
        return codeApplicationService.getCollectedCountByProduct(orderNo, productNo);
    }
    
    /**
     * 获取产品已生产垛数（按OrderNo和ProductNo统计）
     * 用于主界面单位实时统计，因为一个订单可能包含多个产品
     */
    @GetMapping("/produced-pallet-count-by-product")
    public ApiResult<Integer> getProducedPalletCountByProduct(
            @RequestParam("orderNo") String orderNo,
            @RequestParam("productNo") String productNo) {
        return codeApplicationService.getProducedPalletCountByProduct(orderNo, productNo);
    }
    
    /**
     * 获取产品已生产垛数（优化版）
     * 使用COUNT(DISTINCT TagNo)在数据库层面直接计数，性能更优
     */
    @GetMapping("/produced-pallet-count-by-product-optimized")
    public ApiResult<Integer> getProducedPalletCountByProductOptimized(
            @RequestParam("orderNo") String orderNo,
            @RequestParam("productNo") String productNo) {
        return codeApplicationService.getProducedPalletCountByProductOptimized(orderNo, productNo);
    }
}

