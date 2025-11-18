package com.miduo.cloud.entity.dto.code;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 码关联信息表格行数据
 */
@Data
public class CodeAssociationRow {
    /**
     * 第一层：SmallSerialNumber（箱码）
     */
    private String layer1;
    
    /**
     * 采集时间
     */
    private LocalDateTime addTime;
    
    /**
     * 第二层：MediumSerialNumber（中标）
     */
    private String layer2;
    
    /**
     * 第三层：BigSerialNumber（托盘码）
     */
    private String layer3;
    
    /**
     * 第四层：BiggerSerialNumber（更大标）
     */
    private String layer4;
    
    /**
     * 完整的查询结果（用于点击后显示详细信息）
     */
    private CodeQueryVO fullData;
    
    public CodeAssociationRow(CodeQueryVO data) {
        this.layer1 = data.getSmallSerialNumber();
        this.addTime = data.getAddTime();
        this.layer2 = data.getMediumSerialNumber();
        this.layer3 = data.getBigSerialNumber();
        this.layer4 = data.getBiggerSerialNumber();
        this.fullData = data;
    }
}

