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
    private List<String> codes;
}
