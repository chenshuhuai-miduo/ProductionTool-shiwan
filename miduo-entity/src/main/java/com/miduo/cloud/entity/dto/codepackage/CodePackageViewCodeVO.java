package com.miduo.cloud.entity.dto.codepackage;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 码包查看码列表 VO
 */
@Data
public class CodePackageViewCodeVO {

    private String codeValue;
    private Boolean associated;
    private LocalDateTime associatedAt;
}
