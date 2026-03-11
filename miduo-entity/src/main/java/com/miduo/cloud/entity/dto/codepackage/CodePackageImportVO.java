package com.miduo.cloud.entity.dto.codepackage;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 码包导入记录 VO
 */
@Data
public class CodePackageImportVO {

    private Long id;
    private Integer packageType;
    private String packageTypeName;
    private String packageName;
    private LocalDateTime importTime;
    private Integer importSource;
    private String importSourceName;
    private Integer codeCount;
    private Integer status;
    private String statusName;
    private String remark;
    private String fileName;
}
