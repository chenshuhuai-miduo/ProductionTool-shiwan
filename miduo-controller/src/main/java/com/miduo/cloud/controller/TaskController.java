package com.miduo.cloud.controller;

import com.miduo.cloud.application.task.TaskApplicationService;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.common.dto.PageOutput;
import com.miduo.cloud.entity.dto.task.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 任务管理控制器
 * 完全按照原始业务逻辑实现（TaskController.java）
 */
@RestController
@RequestMapping("/api/task")
@CrossOrigin
public class TaskController {
    
    @Autowired
    private TaskApplicationService taskApplicationService;
    
    /**
     * 接口一：增加任务
     * 先操作ProductionOrder表，OrderStatus字段设为1，ProductSourceType字段设为2，所属生产线路先暂时设为001
     * 后操作ProductionOrderDetail表
     */
    @PostMapping("/add")
    public ApiResult<String> addTask(@RequestBody TaskAddRequest request) {
        return taskApplicationService.addTask(request);
    }
    
    /**
     * 接口二：根据ID修改任务
     * 操作的表为ProductionOrderDetail表
     */
    @PutMapping("/update/{id}")
    public ApiResult<Boolean> updateTaskById(@PathVariable("id") Integer id, @RequestBody TaskUpdateRequest request) {
        return taskApplicationService.updateTask(id, request);
    }
    
    /**
     * 接口三：根据ID删除任务
     * 操作的表为ProductionOrderDetail表，IsDel置为1
     */
    @DeleteMapping("/delete/{id}")
    public ApiResult<Boolean> deleteTaskById(@PathVariable("id") Integer id) {
        return taskApplicationService.deleteTask(id);
    }
    
    /**
     * 接口六：修改任务状态（启用/停用）
     * @param id 任务ID
     * @param status 状态：0-待生产，1-生产中，2-已完成，5-提前结单
     * @param type 采集类型（可选）：1-有箱码，2-无箱码（启用任务时需要传递）
     */
    @PutMapping("/updateStatus/{id}/{status}")
    public ApiResult<Boolean> updateTaskStatus(
            @PathVariable("id") Integer id, 
            @PathVariable("status") Integer status,
            @RequestParam(value = "type", required = false) Integer type) {
        return taskApplicationService.updateTaskStatus(id, status, type);
    }
    
    /**
     * 接口四：分页获取任务
     * 支持对生产订单、生产批次、产品名称的模糊查询
     */
    @PostMapping("/page")
    public ApiResult<PageOutput<TaskVO>> queryTaskPage(@RequestBody TaskQueryRequest request) {
        return taskApplicationService.queryTaskPage(request);
    }
    
    /**
     * 接口五：根据生产单号获取任务信息
     * 操作的表为ProductionOrderDetail表
     * 筛选条件：未删除的任务（IsDel=0）
     * 用于主界面选择订单后自动更新当前生产信息
     */
    @GetMapping("/get/{orderNo}")
    public ApiResult<TaskVO> getTaskByOrderNo(@PathVariable("orderNo") String orderNo) {
        return taskApplicationService.getTaskByOrderNo(orderNo);
    }
    
    /**
     * 接口七：分页获取可选择的任务（排除已完成状态）
     * 专门用于主界面的"选择生产订单"对话框
     * 支持对生产订单、生产批次、产品名称的模糊查询
     * 自动过滤状态为"已完成"(OrderStatus=2)的任务
     * 
     * 注意：此接口先查询所有数据，然后在内存中过滤已完成状态，性能较差
     * 推荐使用 /page/selectable/v2 接口，在数据库层面直接过滤
     */
    @PostMapping("/page/selectable")
    public ApiResult<PageOutput<TaskVO>> querySelectableTaskPage(@RequestBody TaskQueryRequest request) {
        return taskApplicationService.querySelectableTaskPage(request);
    }
    
    /**
     * 接口八：分页获取可选择的任务（在数据库层面直接过滤状态）
     * 专门用于主界面的"选择生产订单"对话框
     * 支持对生产订单、生产批次、产品名称的模糊查询
     * 只查询状态为"待生产"(0)和"生产中"(1)的任务，在数据库层面直接过滤，不查询已完成状态
     * 性能优于 /page/selectable 接口
     */
    @PostMapping("/page/selectable/v2")
    public ApiResult<PageOutput<TaskVO>> querySelectableTaskPageV2(@RequestBody TaskQueryRequest request) {
        return taskApplicationService.querySelectableTaskPageV2(request);
    }
}



