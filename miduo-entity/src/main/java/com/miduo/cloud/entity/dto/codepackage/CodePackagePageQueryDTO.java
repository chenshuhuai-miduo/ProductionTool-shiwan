package com.miduo.cloud.entity.dto.codepackage;

import com.miduo.cloud.common.dto.PageInput;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 码包分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CodePackagePageQueryDTO extends PageInput {

    /**
     * 兼容前端 pageNum 字段
     */
    private Long pageNum;

    /**
     * 兼容前端 pageSize 字段
     */
    private Long pageSize;

    private String keyword;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer importSource;
    private Integer packageType;
    private Integer status;

    public long resolveCurrent() {
        return pageNum != null ? pageNum : getCurrent();
    }

    public long resolveSize() {
        return pageSize != null ? pageSize : getSize();
    }
}
