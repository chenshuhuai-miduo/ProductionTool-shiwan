package com.miduo.cloud.infrastructure.persistence.mybatis.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * IO设备表实体类
 */
@Data
@TableName("IoConfig")
public class IoDevicePO {
    
    /**
     * 主键
     */
    @TableId(value = "ID", type = IdType.AUTO)
    private Integer id;
    
    /**
     * 设备序号
     */
    @TableField("DeviceIndex")
    private Integer deviceIndex;
    
    /**
     * 设备名称
     */
    @TableField("DeviceName")
    private String deviceName;
    
    /**
     * 设备类型
     */
    @TableField("DeviceType")
    private String deviceType;
    
    /**
     * 连接参数
     */
    @TableField("ConnectionParam")
    private String connectionParam;
    
    /**
     * 状态 (0:未连接, 1:已连接)
     */
    @TableField("Status")
    private Integer status;

}

