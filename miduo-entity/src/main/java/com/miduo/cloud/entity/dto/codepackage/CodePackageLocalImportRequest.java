package com.miduo.cloud.entity.dto.codepackage;

import lombok.Data;

import java.util.List;

/**
 * 本地导入请求
 */
@Data
public class CodePackageLocalImportRequest {

    private Integer packageType;
    private String packageName;
    private String fileName;
    private String password;
    private String remark;
    /**
     * 本地 TXT 绝对路径（与 {@link #codes} 二选一；优先使用路径，避免大批量 JSON 传输）
     */
    private String localFilePath;
    /**
     * 码行列表；未传 {@link #localFilePath} 时使用
     */
    private List<String> codes;
}
