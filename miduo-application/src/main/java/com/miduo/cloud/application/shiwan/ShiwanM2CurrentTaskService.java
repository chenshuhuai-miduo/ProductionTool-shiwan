package com.miduo.cloud.application.shiwan;

import com.miduo.cloud.common.dto.ApiResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 石湾 2 号机当前任务服务。
 * 门禁通过后写入 ProductionOrder + ProductionOrderDetail（顺序同致美斋），并支持查询当前任务。
 */
@Service
public class ShiwanM2CurrentTaskService {

    private final JdbcTemplate jdbcTemplate;

    public ShiwanM2CurrentTaskService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 开始采集：写入当前任务（ProductionOrder + ProductionOrderDetail），OrderStatus=1（入库中）。
     * 请求参数：orderNo、productNo、productName、boxesPerPallet(M)、boxesPerCase(N)。
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResult<Map<String, Object>> startCurrentTask(String orderNo, String productNo, String productName,
                                                           Integer boxesPerPallet, Integer boxesPerCase) {
        if (orderNo == null || orderNo.trim().isEmpty()) {
            return ApiResult.error(400, "生产单号不能为空");
        }
        if (productNo == null || productNo.trim().isEmpty()) {
            return ApiResult.error(400, "产品编号不能为空");
        }
        if (productName == null) productName = "";
        int m = boxesPerPallet != null && boxesPerPallet > 0 ? boxesPerPallet : 70;
        int n = boxesPerCase != null && boxesPerCase > 0 ? boxesPerCase : 4;

        try {
            LocalDateTime now = LocalDateTime.now();
            String normalizedOrderNo = orderNo.trim();
            int orderId = upsertProductionOrder(normalizedOrderNo, now);
            upsertProductionOrderDetail(orderId, normalizedOrderNo, productNo.trim(), productName.trim(), m, now);

            Map<String, Object> data = new HashMap<>();
            data.put("taskId", orderId);
            data.put("orderId", orderId);
            data.put("orderNo", normalizedOrderNo);
            data.put("productNo", productNo.trim());
            data.put("productName", productName.trim());
            data.put("boxesPerPallet", m);
            data.put("boxesPerCase", n);
            return ApiResult.success("当前任务已写入，开始采集", data);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("写入当前任务失败：" + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
    }

    /**
     * 查询当前任务：OrderStatus=1（入库中）的最新一条 ProductionOrder 及其明细。
     */
    public ApiResult<Map<String, Object>> getCurrentTask() {
        try {
            List<Map<String, Object>> list = jdbcTemplate.queryForList(
                    "SELECT po.Id AS orderId, po.OrderNo AS orderNo, po.OrderStatus AS orderStatus, " +
                            "pod.ProductNO AS productNo, pod.ProductName AS productName, pod.Ratio AS ratio " +
                            "FROM ProductionOrder po " +
                            "LEFT JOIN ProductionOrderDetail pod ON po.Id = pod.OrderId AND (pod.IsDel = 0 OR pod.IsDel IS NULL) " +
                            "WHERE po.OrderStatus = 1 ORDER BY po.Id DESC LIMIT 1"
            );
            if (list == null || list.isEmpty()) {
                return ApiResult.success("暂无进行中的任务", null);
            }
            Map<String, Object> row = list.get(0);
            Map<String, Object> data = new HashMap<>();
            data.put("taskId", row.get("orderId"));
            data.put("orderId", row.get("orderId"));
            data.put("orderNo", row.get("orderNo"));
            data.put("orderStatus", row.get("orderStatus"));
            data.put("productNo", row.get("productNo"));
            data.put("productName", row.get("productName"));
            data.put("boxesPerPallet", row.get("ratio")); // 致美斋 Ratio 存拖箱比例，石湾此处可表示每垛箱数 M
            return ApiResult.success("查询成功", data);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("查询当前任务失败：" + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
    }

    /**
     * 按箱码查任务：根据箱码（BigSerialNumber）查 CodeRelationUpload 得 OrderNo，再返回该订单及未成垛箱数。
     */
    public ApiResult<Map<String, Object>> getTaskByCaseCode(String caseCode) {
        if (caseCode == null || caseCode.trim().isEmpty()) {
            return ApiResult.error(400, "箱码不能为空");
        }
        try {
            List<Map<String, Object>> list = jdbcTemplate.queryForList(
                    "SELECT OrderNo, ProductNO FROM CodeRelationUpload WHERE BigSerialNumber = ? AND IsDel = 0 LIMIT 1",
                    caseCode.trim());
            if (list == null || list.isEmpty()) {
                return ApiResult.success("未找到该箱码对应任务", null);
            }
            String orderNo = (String) list.get(0).get("OrderNo");
            String productNo = (String) list.get(0).get("ProductNO");
            if (orderNo == null) orderNo = "";
            if (productNo == null) productNo = "";
            Integer currentCaseCountObj = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT BigSerialNumber) FROM CodeRelationUpload " +
                            "WHERE OrderNo = ? AND IsDel = 0 AND (VirtualSerialNumber IS NULL OR VirtualSerialNumber = '') " +
                            "AND BigSerialNumber IS NOT NULL AND BigSerialNumber != ''",
                    Integer.class, orderNo);
            int currentCaseCount = currentCaseCountObj != null ? currentCaseCountObj : 0;
            Map<String, Object> data = new HashMap<>();
            data.put("orderNo", orderNo);
            data.put("productNo", productNo);
            data.put("currentCaseCount", currentCaseCount != 0 ? currentCaseCount : 0);
            return ApiResult.success("查询成功", data);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("按箱码查任务失败：" + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
    }

    /**
     * 暂存：将当前任务下未成垛的 CodeRelationUpload 记录的 Status 设为 0（未完成）。
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResult<String> markUnfinished(String orderNo) {
        if (orderNo == null || orderNo.trim().isEmpty()) {
            return ApiResult.error(400, "订单号不能为空");
        }
        try {
            int updated = jdbcTemplate.update(
                    "UPDATE CodeRelationUpload SET Status = 0 WHERE OrderNo = ? AND IsDel = 0 " +
                            "AND (VirtualSerialNumber IS NULL OR VirtualSerialNumber = '') " +
                            "AND BigSerialNumber IS NOT NULL AND BigSerialNumber != ''",
                    orderNo.trim());
            return ApiResult.success("已标记未成垛数据为未完成，记录数：" + updated, "OK");
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("暂存失败：" + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
    }

    /**
     * 启动恢复：查询存在 Status=0 未完成数据的订单，返回订单号及未成垛箱数等。
     */
    public ApiResult<List<Map<String, Object>>> getUnfinishedOrders() {
        try {
            List<Map<String, Object>> list = jdbcTemplate.queryForList(
                    "SELECT cru.OrderNo, " +
                            "COUNT(DISTINCT cru.BigSerialNumber) AS currentCaseCount, " +
                            "MAX(pod.ProductNO) AS productNo, " +
                            "MAX(pod.ProductName) AS productName " +
                            "FROM CodeRelationUpload cru " +
                            "LEFT JOIN ProductionOrder po ON po.OrderNo = cru.OrderNo " +
                            "LEFT JOIN ProductionOrderDetail pod ON pod.OrderId = po.Id AND (pod.IsDel = 0 OR pod.IsDel IS NULL) " +
                            "WHERE cru.Status = 0 AND cru.IsDel = 0 " +
                            "AND cru.BigSerialNumber IS NOT NULL AND cru.BigSerialNumber != '' " +
                            "GROUP BY cru.OrderNo");
            if (list == null || list.isEmpty()) {
                return ApiResult.success("无未成垛数据", Collections.emptyList());
            }
            return ApiResult.success("查询成功", list);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("查询未成垛数据失败：" + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
    }

    private int upsertProductionOrder(String orderNo, LocalDateTime now) {
        List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                "SELECT Id FROM ProductionOrder WHERE OrderNo = ? ORDER BY Id DESC LIMIT 1",
                orderNo);
        if (existing != null && !existing.isEmpty() && existing.get(0).get("Id") != null) {
            int orderId = ((Number) existing.get(0).get("Id")).intValue();
            jdbcTemplate.update(
                    "UPDATE ProductionOrder SET Type = 1, OrderStatus = 1, ProductSourceType = 2, DmakeDate = ?, CreateTime = ?, ProductionLine = 1 WHERE Id = ?",
                    now, now, orderId);
            return orderId;
        }

        String orderSql = "INSERT INTO ProductionOrder (SrcId, Type, OrderNo, OrderSumCoun, ProductSumCount, DmakeDate, OrderStatus, ProductSourceType, CreateTime, ProductionLine) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, 0);
            ps.setInt(2, 1);
            ps.setString(3, orderNo);
            ps.setInt(4, 0);
            ps.setInt(5, 0);
            ps.setObject(6, now);
            ps.setInt(7, 1);
            ps.setInt(8, 2);
            ps.setObject(9, now);
            ps.setInt(10, 1);
            return ps;
        }, keyHolder);
        Number orderIdNum = keyHolder.getKey();
        if (orderIdNum == null) {
            throw new IllegalStateException("写入生产订单失败，未返回订单ID");
        }
        return orderIdNum.intValue();
    }

    private void upsertProductionOrderDetail(int orderId, String orderNo, String productNo, String productName, int ratio, LocalDateTime now) {
        List<Map<String, Object>> detailRows = jdbcTemplate.queryForList(
                "SELECT Id FROM ProductionOrderDetail WHERE OrderId = ? AND (IsDel = 0 OR IsDel IS NULL) ORDER BY Id DESC LIMIT 1",
                orderId);
        if (detailRows != null && !detailRows.isEmpty() && detailRows.get(0).get("Id") != null) {
            int detailId = ((Number) detailRows.get(0).get("Id")).intValue();
            jdbcTemplate.update(
                    "UPDATE ProductionOrderDetail SET ProductName = ?, ProductNO = ?, Ratio = ?, ProductTime = ?, TwillendTime = ?, CreateTime = ? WHERE Id = ?",
                    productName, productNo, ratio, now, now, now, detailId);
            return;
        }
        String detailSql = "INSERT INTO ProductionOrderDetail (OrderCount, ProductCount, ProductID, ProductName, ProductNO, ProductForMatID, ProductForMatName, OrderNo, OrderId, SyBatchNo, Type, Ratio, IsDel, AddTime, ProductTime, TwillendTime, CreateTime) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(detailSql,
                0, 0, 0, productName, productNo, 0, "",
                orderNo, orderId, "", 1, ratio, 0, now, now, now, now);
    }
}
