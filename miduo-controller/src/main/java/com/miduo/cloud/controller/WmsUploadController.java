package com.miduo.cloud.controller;

import com.miduo.cloud.application.upload.WmsUploadApplicationService;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.entity.dto.upload.RealTimeUploadVO;
import com.miduo.cloud.entity.dto.upload.UploadRequestDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * WMS上传Controller
 */
@RestController
@RequestMapping("/api/wms")
@CrossOrigin(originPatterns = "*", allowCredentials = "false")
public class WmsUploadController {
    
    @Autowired
    private WmsUploadApplicationService wmsUploadApplicationService;
    
    /**
     * 上传数据到WMS（暂未实现完整接口路径）
     *
     * @param request 上传请求
     * @return 上传结果
     */
    @PostMapping("/synq/resources/asns")
    public ApiResult<String> uploadToWms(@RequestBody UploadRequestDTO request) {
        return wmsUploadApplicationService.uploadToWms(request);
    }
    
    /**
     * 获取单个订单的实时上传数据（通过任务ID）
     *
     * @param taskId 任务ID（ProductionOrderDetail.Id）
     * @return 实时上传数据
     */
    @GetMapping("/realtime-upload/single-by-task")
    public ApiResult<RealTimeUploadVO> getSingleOrderUploadDataByTask(@RequestParam Integer taskId) {
        return wmsUploadApplicationService.getSingleOrderUploadDataByTaskId(taskId);
    }
    
    /**
     * 获取单个订单的实时上传数据（通过订单号）
     *
     * @param orderNo 订单号
     * @return 实时上传数据
     * @deprecated 当一个订单有多个产品明细时，应使用getSingleOrderUploadDataByTask
     */
    @GetMapping("/realtime-upload/single")
    @Deprecated
    public ApiResult<RealTimeUploadVO> getSingleOrderUploadData(@RequestParam String orderNo) {
        return wmsUploadApplicationService.getSingleOrderUploadData(orderNo);
    }
}

