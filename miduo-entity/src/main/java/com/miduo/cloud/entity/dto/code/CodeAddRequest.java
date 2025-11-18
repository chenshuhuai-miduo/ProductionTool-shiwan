package com.miduo.cloud.entity.dto.code;

import lombok.Data;

/**
 * 添加码请求DTO
 */
@Data
public class CodeAddRequest {

    /**
     * 小标（箱码/瓶码）
     */
    private String smallSerialNumber;

    /**
     * 产品编号
     */
    private String productNo;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 批次号
     */
    private String batchNo;

    /**
     * 垛数量（每个托盘多少垛）
     */
    private Integer stackCount;

    /**
     * 箱数量（每垛多少箱）
     */
    private Integer boxCount;

    /**
     * 类型：1-有箱码，2-无箱码
     */
    private Integer type;
}

