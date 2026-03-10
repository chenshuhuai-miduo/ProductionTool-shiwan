package com.miduo.cloud.variant.zhimeizhai.application.task.mapper;

import com.miduo.cloud.domain.task.model.Task;
import com.miduo.cloud.entity.dto.task.TaskAddRequest;
import com.miduo.cloud.entity.dto.task.TaskUpdateRequest;
import com.miduo.cloud.entity.dto.task.TaskVO;

import java.time.LocalDateTime;

/**
 * Task DTO映射器（致美斋产线）
 */
public class TaskDtoMapper {

    public static Task toDomain(TaskAddRequest request) {
        if (request == null) return null;
        Task task = new Task();
        task.setType(request.getType() != null ? request.getType() : 1);
        task.setOrderSumCount(request.getOrderSumCount() != null ? request.getOrderSumCount() : 0);
        task.setProductSumCount(request.getProductSumCount() != null ? request.getProductSumCount() : 0);
        task.setDmakeDate(request.getDmakeDate() != null ? request.getDmakeDate() : LocalDateTime.now());
        task.setOrderStatus(0);
        task.setProductId(request.getProductId());
        task.setProductName(request.getProductName());
        task.setProductNo(request.getProductNo());
        task.setProductFormatId(request.getProductFormatId());
        task.setProductFormatName(request.getProductFormatName());
        task.setProductCount(request.getProductCount() != null ? request.getProductCount() : 0);
        task.setSyBatchNo(request.getSyBatchNo() != null ? request.getSyBatchNo() : "");
        task.setRatio(request.getRatio() != null ? request.getRatio() : 0);
        task.setProductTime(request.getProductTime() != null ? request.getProductTime() : LocalDateTime.now());
        task.setTwillendTime(request.getTwillendTime() != null ? request.getTwillendTime() : LocalDateTime.now());
        task.setOrderCount(0);
        task.setCreateTime(LocalDateTime.now());
        task.setIsDel(0);
        return task;
    }

    public static void updateDomain(Task task, TaskUpdateRequest request) {
        if (task == null || request == null) return;
        if (request.getProductName() != null) task.setProductName(request.getProductName());
        if (request.getProductNo() != null) task.setProductNo(request.getProductNo());
        if (request.getProductFormatId() != null) task.setProductFormatId(request.getProductFormatId());
        if (request.getProductFormatName() != null) task.setProductFormatName(request.getProductFormatName());
        if (request.getProductCount() != null) task.setProductCount(request.getProductCount());
        if (request.getSyBatchNo() != null) task.setSyBatchNo(request.getSyBatchNo());
        if (request.getRatio() != null) task.setRatio(request.getRatio());
        if (request.getProductTime() != null) task.setProductTime(request.getProductTime());
        if (request.getTwillendTime() != null) task.setTwillendTime(request.getTwillendTime());
    }

    public static TaskVO toVO(Task task) {
        if (task == null) return null;
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
        vo.setOrderStatusText(getOrderStatusText(task.getOrderStatus()));
        vo.setTypeText(getTypeText(task.getType()));
        return vo;
    }

    private static String getOrderStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待生产";
            case 1: return "生产中";
            case 2: return "已完成";
            case 3: return "生产中";
            case 5: return "提前结单";
            default: return "未知状态";
        }
    }

    private static String getTypeText(Integer type) {
        return type == null ? "未知" : (type == 1 ? "有箱码" : "无箱码");
    }
}
