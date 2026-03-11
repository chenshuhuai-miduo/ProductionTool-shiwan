package com.miduo.cloud.infrastructure.persistence.mybatis.po;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 码关联时间查询结果
 */
@Data
public class CodeAssociationTimePO {
    private String codeValue;
    private LocalDateTime associatedAt;
}
