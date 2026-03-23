package com.miduo.cloud.application.shiwan;

import com.miduo.cloud.common.dto.ApiResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

            // 检查是否已有相同订单号的进行中任务（OrderStatus=1）
            List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                    "SELECT po.Id AS orderId FROM ProductionOrder po " +
                    "WHERE po.OrderNo = ? AND po.OrderStatus = 1 ORDER BY po.Id DESC LIMIT 1",
                    orderNo.trim());

            int orderId;
            if (existing != null && !existing.isEmpty()) {
                // 已有进行中的任务：更新产品信息及采集规格
                orderId = ((Number) existing.get(0).get("orderId")).intValue();
                jdbcTemplate.update(
                        "UPDATE ProductionOrder SET CasesPerPallet = ?, BoxesPerCase = ? WHERE Id = ?",
                        m, n, orderId);
                jdbcTemplate.update(
                        "UPDATE ProductionOrderDetail SET ProductName = ?, ProductNO = ?, Ratio = ?, ProductForMatID = ? " +
                        "WHERE OrderId = ? AND (IsDel = 0 OR IsDel IS NULL)",
                        productName.trim(), productNo.trim(), m, n, orderId);
            } else {
                // 无进行中任务：新建 ProductionOrder + ProductionOrderDetail
                String orderSql = "INSERT INTO ProductionOrder (SrcId, Type, OrderNo, OrderSumCoun, ProductSumCount, DmakeDate, OrderStatus, ProductSourceType, CreateTime, ProductionLine, CasesPerPallet, BoxesPerCase) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                KeyHolder keyHolder = new GeneratedKeyHolder();
                jdbcTemplate.update(conn -> {
                    PreparedStatement ps = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS);
                    ps.setInt(1, 0);
                    ps.setInt(2, 1);   // 1 有箱码
                    ps.setString(3, orderNo.trim());
                    ps.setInt(4, 0);
                    ps.setInt(5, 0);
                    ps.setObject(6, now);
                    ps.setInt(7, 1);   // 1 入库中
                    ps.setInt(8, 2);   // 2 产线
                    ps.setObject(9, now);
                    ps.setInt(10, 1);  // ProductionLine
                    ps.setInt(11, m);  // CasesPerPallet
                    ps.setInt(12, n);  // BoxesPerCase
                    return ps;
                }, keyHolder);

                Number orderIdNum = keyHolder.getKey();
                if (orderIdNum == null) {
                    return ApiResult.error("写入生产订单失败，未返回订单ID");
                }
                orderId = orderIdNum.intValue();

                String detailSql = "INSERT INTO ProductionOrderDetail (OrderCount, ProductCount, ProductID, ProductName, ProductNO, ProductForMatID, ProductForMatName, OrderNo, OrderId, SyBatchNo, Type, Ratio, IsDel, AddTime, ProductTime, TwillendTime, CreateTime) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                jdbcTemplate.update(detailSql,
                        0, 0, 0, productName.trim(), productNo.trim(), n, "",
                        orderNo.trim(), orderId, "", 1, m, 0, now, now, now, now);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("taskId", orderId);
            data.put("orderId", orderId);
            data.put("orderNo", orderNo.trim());
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
     * 建议生产单号：从 prefix+001 开始递增，返回第一个在 ProductionOrder 中不存在的单号。
     */
    public String suggestOrderNo(String prefix) {
        if (prefix == null || prefix.isEmpty()) return "001";
        try {
            int prefixLen = prefix.length();
            // 查找当天格式（prefix + 纯数字后缀）中已存在的最大序号
            Integer maxSuffix = jdbcTemplate.queryForObject(
                    "SELECT MAX(CAST(SUBSTRING(OrderNo, ?) AS UNSIGNED)) " +
                    "FROM ProductionOrder " +
                    "WHERE OrderNo LIKE ? " +
                    "AND SUBSTRING(OrderNo, ?) REGEXP '^[0-9]+$'",
                    Integer.class,
                    prefixLen + 1,    // SUBSTRING 起始位（1-indexed）
                    prefix + "%",     // LIKE 过滤前缀
                    prefixLen + 1);   // 同上，用于 REGEXP 纯数字校验
            if (maxSuffix == null || maxSuffix <= 0) {
                return prefix + "001";
            }
            int next = maxSuffix + 1;
            return prefix + String.format("%03d", Math.min(next, 999));
        } catch (Exception e) {
            System.err.println("[suggestOrderNo] 查询失败 prefix=" + prefix + ": " + e.getMessage());
            return prefix + "001";
        }
    }

    /**
     * 检查生产单号是否已有历史生产数据。
     * 触发条件：
     * 1) ProductionOrder 存在该 OrderNo；
     * 2) CodeRelationUpload 存在该 OrderNo 且 IsDel=0 的记录。
     */
    public boolean existsOrderNo(String orderNo) {
        if (orderNo == null || orderNo.trim().isEmpty()) return false;
        try {
            String orderNoTrim = orderNo.trim();
            Long poCnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM ProductionOrder WHERE OrderNo = ?",
                    Long.class, orderNoTrim);
            if (poCnt == null || poCnt <= 0) return false;

            Long cruCnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM CodeRelationUpload WHERE OrderNo = ? AND IsDel = 0",
                    Long.class, orderNoTrim);
            return cruCnt != null && cruCnt > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 暂存：将当前任务下未成垛的 CodeRelationUpload 记录的 Status 设为 3（未成垛），
     * 并更新该垛 TagNo 对应所有记录的 Qty（= COUNT(*) WHERE TagNo）。
     */
    public ApiResult<String> markUnfinished(String orderNo) {
        if (orderNo == null || orderNo.trim().isEmpty()) {
            return ApiResult.error(400, "订单号不能为空");
        }
        try {
            // 先找到当前未成垛记录的 TagNo（用于后续更新 Qty）
            List<String> tagNos = jdbcTemplate.queryForList(
                    "SELECT DISTINCT TagNo FROM CodeRelationUpload " +
                    "WHERE OrderNo = ? AND IsDel = 0 " +
                    "AND (VirtualSerialNumber IS NULL OR VirtualSerialNumber = '') " +
                    "AND BigSerialNumber IS NOT NULL AND BigSerialNumber != '' " +
                    "AND TagNo IS NOT NULL AND TagNo != ''",
                    String.class, orderNo.trim());

            // 将未成垛记录标记为 Status=3
            jdbcTemplate.update(
                    "UPDATE CodeRelationUpload SET Status = 3 WHERE OrderNo = ? AND IsDel = 0 " +
                            "AND (VirtualSerialNumber IS NULL OR VirtualSerialNumber = '') " +
                            "AND BigSerialNumber IS NOT NULL AND BigSerialNumber != ''",
                    orderNo.trim());

            // 更新每个 TagNo 的 Qty = 该 TagNo 下所有未删除记录数
            for (String tagNo : tagNos) {
                try {
                    Integer count = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM CodeRelationUpload WHERE TagNo = ? AND IsDel = 0",
                            Integer.class, tagNo);
                    if (count != null && count > 0) {
                        jdbcTemplate.update(
                                "UPDATE CodeRelationUpload SET Qty = ? WHERE TagNo = ? AND IsDel = 0",
                                count, tagNo);
                    }
                } catch (Exception e) {
                    // Qty 更新失败不影响暂存主流程
                }
            }

            return ApiResult.success("已标记未成垛数据为未成垛", "OK");
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("暂存失败：" + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
    }

    /**
     * 提取工单未成垛：两步查询。
     * 第一步：根据输入箱码找到其 TagNo（条件：BigSerialNumber=箱码、VirtualSerialNumber=''、Status=3、IsDel=0、TagNo非空）。
     * 第二步：用 TagNo 拉取该垛所有记录（条件：TagNo=?、Status=3、VirtualSerialNumber=''、IsDel=0），
     *         从这些记录派生 currentCaseCount，同时关联 ProductionOrder 取采集规格。
     */
    public ApiResult<Map<String, Object>> queryUnfinishedByBoxCode(String boxCode) {
        if (boxCode == null || boxCode.trim().isEmpty()) {
            return ApiResult.error(400, "箱码不能为空");
        }
        try {
            // ── 第一步：根据箱码找到 TagNo 及基本信息 ─────────────────────────────
            List<Map<String, Object>> anchor = jdbcTemplate.queryForList(
                    "SELECT cru.TagNo, cru.OrderNo, cru.ProductNO, " +
                    "po.CasesPerPallet AS casesPerPallet, po.BoxesPerCase AS boxesPerCase " +
                    "FROM CodeRelationUpload cru " +
                    "LEFT JOIN ProductionOrder po ON po.OrderNo = cru.OrderNo " +
                    "WHERE cru.BigSerialNumber = ? " +
                    "AND (cru.VirtualSerialNumber IS NULL OR cru.VirtualSerialNumber = '') " +
                    "AND cru.Status = 3 AND cru.IsDel = 0 " +
                    "AND cru.TagNo IS NOT NULL AND cru.TagNo != '' LIMIT 1",
                    boxCode.trim());
            if (anchor == null || anchor.isEmpty()) {
                return ApiResult.error(404, "未找到对应的未成垛记录");
            }
            String tagNo     = anchor.get(0).get("TagNo")    != null ? anchor.get(0).get("TagNo").toString()    : "";
            String orderNo   = anchor.get(0).get("OrderNo")  != null ? anchor.get(0).get("OrderNo").toString()  : "";
            String productNo = anchor.get(0).get("ProductNO") != null ? anchor.get(0).get("ProductNO").toString() : "";
            int casesPerPallet = anchor.get(0).get("casesPerPallet") instanceof Number
                    ? ((Number) anchor.get(0).get("casesPerPallet")).intValue() : 70;
            int boxesPerCase   = anchor.get(0).get("boxesPerCase") instanceof Number
                    ? ((Number) anchor.get(0).get("boxesPerCase")).intValue() : 4;

            if (tagNo.isEmpty()) {
                return ApiResult.error(404, "未找到对应的未成垛记录（TagNo 为空）");
            }

            // ── 第二步：按 TagNo 拉取该垛所有未成垛记录，派生 currentCaseCount ───────
            List<Map<String, Object>> palletRecords = jdbcTemplate.queryForList(
                    "SELECT BigSerialNumber, MediumSerialNumber FROM CodeRelationUpload " +
                    "WHERE TagNo = ? AND Status = 3 AND IsDel = 0 " +
                    "AND (VirtualSerialNumber IS NULL OR VirtualSerialNumber = '')",
                    tagNo);

            // 从内存数据派生已关联箱数
            Set<String> distinctCaseCodes = new java.util.HashSet<>();
            for (Map<String, Object> row : palletRecords) {
                Object bsn = row.get("BigSerialNumber");
                if (bsn != null && !bsn.toString().isEmpty()) {
                    distinctCaseCodes.add(bsn.toString());
                }
            }
            int currentCaseCount = distinctCaseCodes.size();

            // ── 产品名称 ──────────────────────────────────────────────────────────
            String productName = "";
            if (!productNo.isEmpty()) {
                try {
                    List<Map<String, Object>> nameRows = jdbcTemplate.queryForList(
                            "SELECT ProductName FROM ProductInfo WHERE ProductNo = ? LIMIT 1", productNo);
                    if (nameRows != null && !nameRows.isEmpty() && nameRows.get(0).get("ProductName") != null) {
                        productName = nameRows.get(0).get("ProductName").toString();
                    }
                } catch (Exception ignored) {}
            }

            // ── 待入队盒码数（Status=0，尚未关联到箱）────────────────────────────
            Integer pendingBoxCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT MediumSerialNumber) FROM CodeRelationUpload " +
                    "WHERE OrderNo = ? AND IsDel = 0 AND Status = 0 " +
                    "AND (VirtualSerialNumber IS NULL OR VirtualSerialNumber = '') " +
                    "AND (BigSerialNumber IS NULL OR BigSerialNumber = '')",
                    Integer.class, orderNo);

            Map<String, Object> data = new HashMap<>();
            data.put("tagNo", tagNo);
            data.put("orderNo", orderNo);
            data.put("productNo", productNo);
            data.put("productName", productName);
            data.put("casesPerPallet", casesPerPallet);
            data.put("boxesPerCase", boxesPerCase);
            data.put("currentCaseCount", currentCaseCount);
            data.put("pendingBoxCount", pendingBoxCount != null ? pendingBoxCount : 0);
            return ApiResult.success("查询成功", data);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("查询失败：" + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
    }

    /**
     * 启动恢复：查询存在 Status=3（未成垛）数据的订单，返回订单号及未成垛箱数等。
     */
    public ApiResult<List<Map<String, Object>>> getUnfinishedOrders() {
        try {
            // 查询未成垛的已关联箱数（Status=3）及产品编号
            List<Map<String, Object>> list = jdbcTemplate.queryForList(
                    "SELECT OrderNo, MAX(ProductNO) AS productNo, " +
                            "COUNT(DISTINCT BigSerialNumber) AS currentCaseCount " +
                            "FROM CodeRelationUpload WHERE Status = 3 AND IsDel = 0 " +
                            "AND BigSerialNumber IS NOT NULL AND BigSerialNumber != '' " +
                            "GROUP BY OrderNo");
            if (list == null || list.isEmpty()) {
                return ApiResult.success("无未成垛数据", Collections.emptyList());
            }
            // 补充：每个订单下等待入队的盒码数（Status=0, BigSerialNumber='', VirtualSerialNumber=''）和产品名称
            for (Map<String, Object> row : list) {
                String orderNo = row.get("OrderNo") != null ? row.get("OrderNo").toString() : "";
                Integer pendingBoxCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(DISTINCT MediumSerialNumber) FROM CodeRelationUpload " +
                        "WHERE OrderNo = ? AND IsDel = 0 AND Status = 0 " +
                        "AND (VirtualSerialNumber IS NULL OR VirtualSerialNumber = '') " +
                        "AND (BigSerialNumber IS NULL OR BigSerialNumber = '')",
                        Integer.class, orderNo);
                row.put("pendingBoxCount", pendingBoxCount != null ? pendingBoxCount : 0);
                // 查询产品名称
                String productNo = row.get("productNo") != null ? row.get("productNo").toString() : "";
                String productName = "";
                if (!productNo.isEmpty()) {
                    try {
                        List<Map<String, Object>> nameRows = jdbcTemplate.queryForList(
                                "SELECT ProductName FROM ProductInfo WHERE ProductNo = ? LIMIT 1", productNo);
                        if (nameRows != null && !nameRows.isEmpty() && nameRows.get(0).get("ProductName") != null) {
                            productName = nameRows.get(0).get("ProductName").toString();
                        }
                    } catch (Exception ignored) {}
                }
                row.put("productName", productName);
            }
            return ApiResult.success("查询成功", list);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("查询未成垛数据失败：" + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
    }
}
