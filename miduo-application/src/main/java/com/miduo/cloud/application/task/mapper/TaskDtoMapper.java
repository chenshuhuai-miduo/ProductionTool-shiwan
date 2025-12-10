package com.miduo.cloud.application.task.mapper;

import com.miduo.cloud.domain.task.model.Task;
import com.miduo.cloud.entity.dto.task.TaskAddRequest;
import com.miduo.cloud.entity.dto.task.TaskUpdateRequest;
import com.miduo.cloud.entity.dto.task.TaskVO;

import java.time.LocalDateTime;

/**
 * Task DTO映射器
 * 负责DTO与Domain Model之间的转换
 */
public class TaskDtoMapper {
    
    /**
     * TaskAddRequest → Task Domain Model
     */
    public static Task toDomain(TaskAddRequest request) {
        if (request == null) {
            return null;
        }
        
        Task task = new Task();
        // 基本信息
        task.setType(request.getType() != null ? request.getType() : 1);
        task.setOrderSumCount(request.getOrderSumCount() != null ? request.getOrderSumCount() : 0);
        task.setProductSumCount(request.getProductSumCount() != null ? request.getProductSumCount() : 0);
        task.setDmakeDate(request.getDmakeDate() != null ? request.getDmakeDate() : LocalDateTime.now());
        task.setOrderStatus(0); // 默认状态
        
        // 产品信息
        task.setProductId(request.getProductId());
        task.setProductName(request.getProductName());
        task.setProductNo(request.getProductNo());
        task.setProductFormatId(request.getProductFormatId());
        task.setProductFormatName(request.getProductFormatName());
        task.setProductCount(request.getProductCount() != null ? request.getProductCount() : 0);
        
        // 生产信息
        task.setSyBatchNo(request.getSyBatchNo() != null ? request.getSyBatchNo() : "");
        task.setRatio(request.getRatio() != null ? request.getRatio() : 0);
        task.setProductTime(request.getProductTime() != null ? request.getProductTime() : LocalDateTime.now());
        task.setTwillendTime(request.getTwillendTime() != null ? request.getTwillendTime() : LocalDateTime.now());
        
        // 系统信息
        task.setOrderCount(0); // 初始已生产数量为0
        task.setCreateTime(LocalDateTime.now());
        task.setIsDel(0);
        
        return task;
    }
    
    /**
     * TaskUpdateRequest → Task Domain Model（部分更新）
     */
    public static void updateDomain(Task task, TaskUpdateRequest request) {
        if (task == null || request == null) {
            return;
        }
        
        if (request.getProductName() != null) {
            task.setProductName(request.getProductName());
        }
        if (request.getProductNo() != null) {
            task.setProductNo(request.getProductNo());
        }
        if (request.getProductFormatId() != null) {
            task.setProductFormatId(request.getProductFormatId());
        }
        if (request.getProductFormatName() != null) {
            task.setProductFormatName(request.getProductFormatName());
        }
        if (request.getProductCount() != null) {
            task.setProductCount(request.getProductCount());
        }
        if (request.getSyBatchNo() != null) {
            task.setSyBatchNo(request.getSyBatchNo());
        }
        if (request.getRatio() != null) {
            task.setRatio(request.getRatio());
        }
        if (request.getProductTime() != null) {
            task.setProductTime(request.getProductTime());
        }
        if (request.getTwillendTime() != null) {
            task.setTwillendTime(request.getTwillendTime());
        }
    }
    
    /**
     * Task Domain Model → TaskVO
     */
    public static TaskVO toVO(Task task) {
        if (task == null) {
            return null;
        }
        
        TaskVO vo = new TaskVO();
        vo.setId(task.getId());
        vo.setOrderNo(task.getOrderNo());
        vo.setType(task.getType());
        vo.setProductName(task.getProductName());
        vo.setProductNo(task.getProductNo());
        vo.setProductFormatName(task.getProductFormatName());
        vo.setProductCount(task.getProductCount());
        vo.setOrderCount(task.getOrderCount());
        vo.setSyBatchNo(task.getSyBatchNo());
        vo.setRatio(task.getRatio());
        vo.setProductTime(task.getProductTime());
        vo.setTwillendTime(task.getTwillendTime());
        vo.setCreateTime(task.getCreateTime());
        vo.setOrderStatus(task.getOrderStatus());
        
        // 设置状态文字
        vo.setOrderStatusText(getOrderStatusText(task.getOrderStatus()));
        vo.setTypeText(getTypeText(task.getType()));
        
        return vo;
    }
    
    /**
     * 获取订单状态文字
     * 状态值说明：
     * 0: 待生产
     * 1: 生产中（已启用）
     * 2: 已完成
     * 3: 生产中（未启用但有采集数据，显示为"生产中"但需要点击启用任务）
     * 5: 提前结单
     */
    private static String getOrderStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0: return "待生产";
            case 1: return "生产中";
            case 2: return "已完成";
            case 3: return "生产中"; // 未启用但有采集数据，显示为"生产中"但需要点击启用任务
            case 5: return "提前结单";
            default: return "未知状态";
        }
    }
    
    /**
     * 获取类型文字
     */
    private static String getTypeText(Integer type) {
        if (type == null) {
            return "未知";
        }
        return type == 1 ? "有箱码" : "无箱码";
    }
}

