package com.miduo.cloud.variant.zhimeizhai.controller;

import com.miduo.cloud.variant.zhimeizhai.application.dataupload.DataUploadApplicationService;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.common.dto.PageOutput;
import com.miduo.cloud.entity.dto.dataupload.DataUploadOrderVO;
import com.miduo.cloud.entity.dto.dataupload.DataUploadTaskVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据上传Controller
 */
@RestController
@RequestMapping("/api/dataupload")
public class DataUploadController {
    
    @Autowired
    private DataUploadApplicationService dataUploadService;
    
    /**
     * 查询所有生产订单
     * GET /api/dataupload/orders
     */
    @GetMapping("/orders")
    public ApiResult<List<DataUploadOrderVO>> queryOrders() {
        return dataUploadService.queryProductionOrders();
    }
    
    /**
     * 根据订单编号查询生产任务数据
     * GET /api/dataupload/tasks/{orderNo}
     */
    @GetMapping("/tasks/{orderNo}")
    public ApiResult<List<DataUploadTaskVO>> queryTasks(@PathVariable String orderNo) {
        return dataUploadService.queryProductionTasks(orderNo);
    }
    
    /**
     * 分页查询生产订单
     * GET /api/dataupload/orders/page
     */
    @GetMapping("/orders/page")
    public ApiResult<PageOutput<DataUploadOrderVO>> queryOrdersByPage(
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String productName) {
        return dataUploadService.queryProductionOrdersByPage(pageNum, pageSize, orderNo, productName);
    }
    
    /**
     * 分页查询生产任务数据（支持按ProductNO和VirtualSerialNumber筛选）
     * GET /api/dataupload/tasks/{orderNo}/page?productNo=xxx&virtualSerialNumber=xxx&pageNum=1&pageSize=20
     */
    @GetMapping("/tasks/{orderNo}/page")
    public ApiResult<PageOutput<DataUploadTaskVO>> queryTasksByPage(
            @PathVariable String orderNo,
            @RequestParam(required = false) String productNo,
            @RequestParam(required = false) String virtualSerialNumber,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        return dataUploadService.queryProductionTasksByPage(orderNo, productNo, virtualSerialNumber, pageNum, pageSize);
    }
}

