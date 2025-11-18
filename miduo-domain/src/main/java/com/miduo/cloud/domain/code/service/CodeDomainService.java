package com.miduo.cloud.domain.code.service;

import com.miduo.cloud.domain.code.model.CodeRelation;
import com.miduo.cloud.domain.code.repository.CodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 码管理领域服务
 * 封装码相关的核心业务逻辑（纯业务逻辑，无事务管理）
 */
@Service
public class CodeDomainService {
    
    @Autowired
    private CodeRepository codeRepository;
    
    private static final DateTimeFormatter CODE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    
    /**
     * 验证码格式
     * @param code 码值
     * @return 验证结果
     */
    public ValidationResult validateCode(String code) {
        if (code == null || code.isEmpty()) {
            return ValidationResult.error("码不能为空");
        }
        
        if ("null".equals(code)) {
            return ValidationResult.error("未检测到码");
        }
        
        // 检查是否全是字母
        if (code.matches("^[a-zA-Z]+$")) {
            return ValidationResult.error("码格式错误：不能全是字母");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * 检查重码
     * @param orderNo 订单号
     * @param boxCode 箱码
     * @return 是否存在重复
     */
    public boolean isDuplicateCode(String orderNo, String boxCode) {
        return codeRepository.findByOrderNoAndBoxCode(orderNo, boxCode).isPresent();
    }
    
    /**
     * 生成系统箱码（22位）
     * @param orderNo 订单号
     * @param sequence 序号
     * @return 系统箱码
     */
    public String generateSystemCode(String orderNo, int sequence) {
        String timestamp = LocalDateTime.now().format(CODE_FORMATTER);
        String seqStr = String.format("%04d", sequence);
        return timestamp + seqStr + "SYS";
    }
    
    /**
     * 生成虚拟箱码
     * @param orderNo 订单号
     * @param tagNo 托盘号
     * @param sequence 序号
     * @return 虚拟箱码
     */
    public String generateVirtualCode(String orderNo, String tagNo, int sequence) {
        String timestamp = LocalDateTime.now().format(CODE_FORMATTER);
        String seqStr = String.format("%04d", sequence);
        return "V" + timestamp + tagNo.substring(tagNo.length() - 2) + seqStr;
    }
    
    /**
     * 批量关联托盘码
     * @param codeRelations 码关联列表
     * @param palletCode 托盘码
     * @return 关联成功的数量
     */
    public int batchAssociatePallet(List<CodeRelation> codeRelations, String palletCode) {
        if (codeRelations == null || codeRelations.isEmpty()) {
            return 0;
        }
        
        List<Integer> ids = codeRelations.stream()
            .map(CodeRelation::getId)
            .collect(Collectors.toList());
        
        return codeRepository.batchUpdatePalletCode(ids, palletCode);
    }
    
    /**
     * 计算托盘完成进度
     * @param orderNo 订单号
     * @param tagNo 托盘号
     * @param targetQty 目标数量
     * @return 完成百分比
     */
    public double calculatePalletProgress(String orderNo, String tagNo, int targetQty) {
        int currentCount = codeRepository.countByTagNo(orderNo, tagNo);
        if (targetQty == 0) {
            return 0.0;
        }
        return (currentCount * 100.0) / targetQty;
    }
    
    /**
     * 验证结果内部类
     */
    public static class ValidationResult {
        private final boolean success;
        private final String message;
        
        private ValidationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
    }
}

