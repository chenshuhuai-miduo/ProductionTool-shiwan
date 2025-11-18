package com.miduo.cloud.application.log;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.common.dto.PageOutput;
import com.miduo.cloud.entity.dto.operatelog.OperateLogQueryDTO;
import com.miduo.cloud.entity.dto.operatelog.OperateLogVO;
import com.miduo.cloud.entity.po.OperateLog;
import com.miduo.cloud.infrastructure.mapper.OperateLogMapper;
import com.miduo.cloud.infrastructure.persistence.mybatis.po.OperateLogPO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 操作日志应用服务
 */
@Service
public class OperateLogApplicationService {
    
    @Autowired
    private OperateLogMapper operateLogMapper;
    
    /**
     * 记录操作日志
     * 
     * @param operateLog 操作日志对象
     */
    public void saveLog(OperateLog operateLog) {
        try {
            // 设置操作时间
            if (operateLog.getOperateTime() == null) {
                operateLog.setOperateTime(LocalDateTime.now());
            }
            
            // 设置默认值
            if (operateLog.getOperatorName() == null) {
                operateLog.setOperatorName("系统");
            }
            if (operateLog.getType() == null) {
                operateLog.setType(1); // 默认操作日志
            }
            if (operateLog.getOperateResult() == null) {
                operateLog.setOperateResult("成功");
            }
            if (operateLog.getOperatorIp() == null) {
                operateLog.setOperatorIp("");
            }
            if (operateLog.getTargetId() == null) {
                operateLog.setTargetId("");
            }
            if (operateLog.getTargetName() == null) {
                operateLog.setTargetName("");
            }
            if (operateLog.getDeviceInfo() == null) {
                operateLog.setDeviceInfo("JavaFX桌面应用");
            }
            
            operateLogMapper.insert(operateLog);
        } catch (Exception e) {
            // 记录日志失败不影响业务流程
            System.err.println("[操作日志] 保存失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 异步记录操作日志（不阻塞主线程）
     * 
     * @param operateLog 操作日志对象
     */
    public void saveLogAsync(OperateLog operateLog) {
        new Thread(() -> saveLog(operateLog)).start();
    }
    
    /**
     * 分页查询操作日志
     *
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    public ApiResult<PageOutput<OperateLogVO>> queryOperateLogs(OperateLogQueryDTO queryDTO) {
        try {
            // 构建查询条件
            LambdaQueryWrapper<OperateLogPO> queryWrapper = new LambdaQueryWrapper<>();
            
            // 日期范围筛选
            if (queryDTO.getStartTime() != null) {
                queryWrapper.ge(OperateLogPO::getOperateTime, queryDTO.getStartTime());
            }
            if (queryDTO.getEndTime() != null) {
                queryWrapper.le(OperateLogPO::getOperateTime, queryDTO.getEndTime());
            }
            
            // 操作人姓名模糊查询
            if (StringUtils.hasText(queryDTO.getOperatorName())) {
                queryWrapper.like(OperateLogPO::getOperatorName, queryDTO.getOperatorName());
            }
            
            // 操作模块模糊查询
            if (StringUtils.hasText(queryDTO.getModuleName())) {
                queryWrapper.like(OperateLogPO::getModuleName, queryDTO.getModuleName());
            }
            
            // 操作类型精确查询
            if (StringUtils.hasText(queryDTO.getOperateType())) {
                queryWrapper.eq(OperateLogPO::getOperateType, queryDTO.getOperateType());
            }
            
            // 按操作时间倒序排列
            queryWrapper.orderByDesc(OperateLogPO::getOperateTime);
            
            // 分页查询
            Page<OperateLogPO> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
            IPage<OperateLogPO> pageResult = operateLogMapper.selectPage(page, queryWrapper);
            
            // PO转VO
            List<OperateLogVO> voList = pageResult.getRecords().stream().map(po -> {
                OperateLogVO vo = new OperateLogVO();
                BeanUtils.copyProperties(po, vo);
                return vo;
            }).collect(Collectors.toList());
            
            // 构建分页输出
            PageOutput<OperateLogVO> pageOutput = new PageOutput<>();
            pageOutput.setCurrent(queryDTO.getCurrent());
            pageOutput.setSize(queryDTO.getSize());
            pageOutput.setTotal(pageResult.getTotal());
            pageOutput.setPages(pageResult.getPages());
            pageOutput.setRecords(voList);
            
            return ApiResult.success("查询成功", pageOutput);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("查询失败：" + e.getMessage());
        }
    }
}

