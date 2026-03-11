package com.miduo.cloud.entity.dto.codepackage;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 在线导入结果
 */
@Data
public class CodePackageOnlineImportResultVO {

    private Integer totalProcessed = 0;
    private Integer successCount = 0;
    private Integer failedCount = 0;
    private List<ItemResult> successItems = new ArrayList<>();
    private List<ItemResult> failedItems = new ArrayList<>();

    @Data
    public static class ItemResult {
        private Integer packageType;
        private String packageTypeName;
        private String fileName;
        private Integer importedCount;
        private String message;
    }
}
