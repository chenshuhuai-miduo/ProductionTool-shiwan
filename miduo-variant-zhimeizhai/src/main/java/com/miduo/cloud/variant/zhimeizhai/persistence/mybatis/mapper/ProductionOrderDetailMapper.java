package com.miduo.cloud.variant.zhimeizhai.persistence.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miduo.cloud.variant.zhimeizhai.persistence.mybatis.po.ProductionOrderDetailPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 生产订单明细表 Mapper（致美斋产线）
 */
@Mapper
public interface ProductionOrderDetailMapper extends BaseMapper<ProductionOrderDetailPO> {

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
