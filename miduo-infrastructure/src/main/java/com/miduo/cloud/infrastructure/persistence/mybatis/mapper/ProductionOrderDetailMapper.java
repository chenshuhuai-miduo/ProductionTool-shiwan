package com.miduo.cloud.infrastructure.persistence.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miduo.cloud.infrastructure.persistence.mybatis.po.ProductionOrderDetailPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 生产订单明细表Mapper接口
 */
@Mapper
public interface ProductionOrderDetailMapper extends BaseMapper<ProductionOrderDetailPO> {
    
    /**
     * 分页查询已完成状态的生产订单明细（关联ProductionOrder表）
     * @param page 分页参数
     * @param orderNo 订单编号（可选）
     * @param productName 产品名称（可选）
     * @return 分页结果
     */
    @Select("<script>" +
            "SELECT pod.* FROM ProductionOrderDetail pod " +
            "INNER JOIN ProductionOrder po ON pod.OrderNo = po.OrderNo " +
            "WHERE pod.IsDel = 0 AND po.OrderStatus = 2 " +
            "<if test='orderNo != null and orderNo != \"\"'>" +
            "  AND pod.OrderNo LIKE '%' + #{orderNo} + '%' " +
            "</if>" +
            "<if test='productName != null and productName != \"\"'>" +
            "  AND pod.ProductName LIKE '%' + #{productName} + '%' " +
            "</if>" +
            "ORDER BY pod.CreateTime DESC" +
            "</script>")
    IPage<ProductionOrderDetailPO> selectCompletedOrdersPage(
            Page<ProductionOrderDetailPO> page,
            @Param("orderNo") String orderNo,
            @Param("productName") String productName
    );
    
    /**
     * 分页查询指定状态的生产订单明细（关联ProductionOrder表）
     * 用于选择生产订单页面，只查询待生产(0)和生产中(1)状态的订单
     * @param page 分页参数
     * @param orderNo 订单编号（可选，模糊查询）
     * @param syBatchNo 生产批次（可选，模糊查询）
     * @param productName 产品名称（可选，模糊查询）
     * @param orderStatuses 订单状态列表（支持多个状态，如：0待生产，1生产中）
     * @param productTimeStart 预计生产时间范围 - 开始时间（可选）
     * @param productTimeEnd 预计生产时间范围 - 结束时间（可选）
     * @return 分页结果
     */
    @Select("<script>" +
            "SELECT pod.* FROM ProductionOrderDetail pod " +
            "INNER JOIN ProductionOrder po ON pod.OrderNo = po.OrderNo " +
            "WHERE pod.IsDel = 0 " +
            "<if test='orderStatuses != null and orderStatuses.size() > 0'>" +
            "  AND po.OrderStatus IN " +
            "  <foreach collection='orderStatuses' item='status' open='(' separator=',' close=')'>" +
            "    #{status}" +
            "  </foreach>" +
            "</if>" +
            "<if test='orderNo != null and orderNo != \"\"'>" +
            "  AND pod.OrderNo LIKE '%' + #{orderNo} + '%' " +
            "</if>" +
            "<if test='syBatchNo != null and syBatchNo != \"\"'>" +
            "  AND pod.SyBatchNo LIKE '%' + #{syBatchNo} + '%' " +
            "</if>" +
            "<if test='productName != null and productName != \"\"'>" +
            "  AND pod.ProductName LIKE '%' + #{productName} + '%' " +
            "</if>" +
            "<if test='productTimeStart != null'>" +
            "  AND pod.ProductTime &gt;= #{productTimeStart} " +
            "</if>" +
            "<if test='productTimeEnd != null'>" +
            "  AND pod.ProductTime &lt;= #{productTimeEnd} " +
            "</if>" +
            "ORDER BY pod.Id DESC" +
            "</script>")
    IPage<ProductionOrderDetailPO> selectOrdersByStatusesPage(
            Page<ProductionOrderDetailPO> page,
            @Param("orderNo") String orderNo,
            @Param("syBatchNo") String syBatchNo,
            @Param("productName") String productName,
            @Param("orderStatuses") List<Integer> orderStatuses,
            @Param("productTimeStart") java.time.LocalDateTime productTimeStart,
            @Param("productTimeEnd") java.time.LocalDateTime productTimeEnd
    );
    
}

