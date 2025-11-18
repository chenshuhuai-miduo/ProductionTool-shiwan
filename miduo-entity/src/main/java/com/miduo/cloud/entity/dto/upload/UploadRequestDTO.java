package com.miduo.cloud.entity.dto.upload;

import lombok.Data;

import java.util.List;

/**
 * WMS上传请求DTO
 */
@Data
public class UploadRequestDTO {
    
    /**
     * 所有者
     */
    private String owner;
    
    /**
     * 运输单元
     */
    private TransportUnit transportUnit;
    
    /**
     * ASN行列表
     */
    private List<AsnLine> asnLine;
    
    @Data
    public static class TransportUnit {
        /**
         * 运输单元ID（托盘码）
         */
        private String tuId;
        
        /**
         * 运输单元类型
         */
        private String tuType;
    }
    
    @Data
    public static class AsnLine {
        /**
         * 行号
         */
        private Integer lineNumber;
        
        /**
         * 产品ID
         */
        private String productId;
        
        /**
         * 预期数量（每垛箱数）
         */
        private Double quantityExpected;
        
        /**
         * 属性值列表
         */
        private List<AttributeValue> attributeValue;
    }
    
    @Data
    public static class AttributeValue {
        /**
         * 属性名称
         */
        private String name;
        
        /**
         * 属性值
         */
        private String value;
    }
}

