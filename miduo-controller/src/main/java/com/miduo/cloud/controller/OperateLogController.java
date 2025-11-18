package com.miduo.cloud.controller;

import com.miduo.cloud.application.log.OperateLogApplicationService;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.common.dto.PageOutput;
import com.miduo.cloud.entity.dto.operatelog.OperateLogQueryDTO;
import com.miduo.cloud.entity.dto.operatelog.OperateLogVO;
import com.miduo.cloud.entity.po.OperateLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 操作日志Controller
 */
@RestController
@CrossOrigin(originPatterns = "*", allowCredentials = "false")
public class OperateLogController {
    
    @Autowired
    private OperateLogApplicationService operateLogApplicationService;
    
    /**
     * 保存操作日志（前端调用）
     * POST /api/log/operate
     *
     * @param operateLog 操作日志对象
     * @return 操作结果
     */
    @PostMapping("/api/log/operate")
    public ApiResult<String> saveOperateLog(@RequestBody OperateLog operateLog) {
        try {
            operateLogApplicationService.saveLog(operateLog);
            return ApiResult.success("日志记录成功");
        } catch (Exception e) {
            System.err.println("[操作日志Controller] 保存失败: " + e.getMessage());
            e.printStackTrace();
            return ApiResult.error("日志记录失败：" + e.getMessage());
        }
    }
    
    /**
     * 分页查询操作日志
     * POST /api/operateLog/query
     *
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    @PostMapping("/api/operateLog/query")
    public ApiResult<PageOutput<OperateLogVO>> queryOperateLogs(@RequestBody OperateLogQueryDTO queryDTO) {
        return operateLogApplicationService.queryOperateLogs(queryDTO);
    }
}
