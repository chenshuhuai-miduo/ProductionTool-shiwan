package com.miduo.cloud.application.shiwan;

import com.miduo.cloud.common.dto.ApiResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 石湾 2 号机盒箱关联与成垛服务。
 * 盒箱关联：校验 N 盒+1 箱码，写入 CodeRelationUpload；满 M 箱时成垛（写虚拟垛标）。
 * 待剔除 Status=3 与剔除记录表本期不实现。
 */
@Service
public class ShiwanM2BoxCaseService {

    private static final String TAG_M2_BOX = "M2-BOX";
    private static final String EMPTY = "";

    private final JdbcTemplate jdbcTemplate;
    private final VirtualPalletSequenceService virtualPalletSequenceService;

    public ShiwanM2BoxCaseService(JdbcTemplate jdbcTemplate,
                                  VirtualPalletSequenceService virtualPalletSequenceService) {
        this.jdbcTemplate = jdbcTemplate;
        this.virtualPalletSequenceService = virtualPalletSequenceService;
    }

    /**
     * 盒箱关联：N 个盒码 + 1 个箱码，校验后写入 CodeRelationUpload；若当前垛箱数达 M 则成垛。
     * 入参：orderNo, productNo, boxCodes, caseCode, boxesPerCase(N), boxesPerPallet(M)。
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResult<Map<String, Object>> associate(String orderNo, String productNo, List<String> boxCodes,
                                                    String caseCode, int boxesPerCase, int boxesPerPallet) {
        if (orderNo == null || orderNo.trim().isEmpty()) {
            return ApiResult.error(400, "订单号不能为空");
        }
        if (productNo == null) productNo = "";
        if (boxCodes == null || boxCodes.size() != boxesPerCase) {
            return ApiResult.error(400, "盒码数量必须等于每箱盒数 N(" + boxesPerCase + ")");
        }
        if (caseCode == null || caseCode.trim().isEmpty()) {
            return ApiResult.error(400, "箱码不能为空");
        }
        caseCode = caseCode.trim();
        List<String> trimmedBoxCodes = new ArrayList<>();
        Set<String> boxSet = new HashSet<>();
        for (String b : boxCodes) {
            if (b == null || b.trim().isEmpty()) {
                return ApiResult.error(400, "盒码不能为空");
            }
            String t = b.trim();
            if (boxSet.contains(t)) {
                return ApiResult.error(400, "盒码重复：" + t);
            }
            boxSet.add(t);
            trimmedBoxCodes.add(t);
        }

        try {
            // 1. 当前任务存在且 OrderStatus=1
            Map<String, Object> task = getCurrentTaskByOrderNo(orderNo);
            if (task == null) {
                return ApiResult.error(400, "未找到进行中的任务或订单号不匹配");
            }
            String useProductNo = (productNo != null && !productNo.isEmpty()) ? productNo : (String) task.get("productNo");
            if (useProductNo == null) useProductNo = "";

            // 2. 箱码在大标码包(PackageType=3)内
            if (!isCodeInPackage(caseCode, 3)) {
                return ApiResult.error(400, "箱码不在大标码包内：" + caseCode);
            }
            // 3. 每个盒码在中标码包(PackageType=2)内
            for (String box : trimmedBoxCodes) {
                if (!isCodeInPackage(box, 2)) {
                    return ApiResult.error(400, "盒码不在中标码包内：" + box);
                }
            }
            // 4. 箱码未重复（该订单下未使用过）
            if (isCaseCodeAlreadyUsed(orderNo, caseCode)) {
                return ApiResult.error(400, "箱码已使用（重码）：" + caseCode);
            }
            // 5. 盒码未重复使用（该订单下未出现）
            for (String box : trimmedBoxCodes) {
                if (isBoxCodeAlreadyUsed(orderNo, box)) {
                    return ApiResult.error(400, "盒码已使用（重码）：" + box);
                }
            }

            LocalDateTime now = LocalDateTime.now();
            String insertSql = "INSERT INTO CodeRelationUpload (" +
                    "BiggerSerialNumber, BigSerialNumber, MediumSerialNumber, SmallSerialNumber, " +
                    "DxCode, SalesCode, VirtualSerialNumber, IsVirtual, ProductNO, OrderNo, Status, TagNo, Qty, Type, " +
                    "WarehouseNo, BatchNo, AddTime, ErrCount, Msg, IsUpload, IsDel, TeamName) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            for (String boxCode : trimmedBoxCodes) {
                jdbcTemplate.update(insertSql,
                        EMPTY, caseCode, boxCode, EMPTY,
                        EMPTY, EMPTY, EMPTY, 0, useProductNo, orderNo.trim(), 1, TAG_M2_BOX, 0, 1,
                        EMPTY, EMPTY, now, 0, EMPTY, 1, 0, EMPTY);
            }

            // 当前垛内箱数（VirtualSerialNumber 为空的不同 BigSerialNumber 数）
            int currentCaseCount = countCurrentCasesInPallet(orderNo);
            boolean fullPallet = currentCaseCount >= boxesPerPallet;
            String palletCode = null;
            if (fullPallet) {
                palletCode = completeCurrentPallet(orderNo, boxesPerPallet);
                currentCaseCount = 0;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("success", true);
            data.put("caseCode", caseCode);
            data.put("currentCaseCount", currentCaseCount);
            data.put("currentBoxInCase", 0);
            data.put("boxesPerCase", boxesPerCase);
            data.put("boxesPerPallet", boxesPerPallet);
            data.put("fullCase", true);
            data.put("fullPallet", fullPallet);
            if (fullPallet && palletCode != null) {
                data.put("palletCode", palletCode);
            }
            return ApiResult.success("盒箱关联成功", data);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("盒箱关联失败：" + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
    }

    /**
     * 强制满垛：将当前未成垛的箱绑定到新虚拟垛标，重置当前垛计数。
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResult<Map<String, Object>> forceFullPallet(String orderNo, Integer currentCaseCount) {
        if (orderNo == null || orderNo.trim().isEmpty()) {
            return ApiResult.error(400, "订单号不能为空");
        }
        try {
            int casesToClose = currentCaseCount != null && currentCaseCount > 0 ? currentCaseCount : countCurrentCasesInPallet(orderNo);
            if (casesToClose <= 0) {
                Map<String, Object> data = new HashMap<>();
                data.put("palletCode", null);
                data.put("currentCaseCount", 0);
                return ApiResult.success("当前无未成垛箱", data);
            }
            String palletCode = completeCurrentPallet(orderNo, casesToClose);
            Map<String, Object> data = new HashMap<>();
            data.put("palletCode", palletCode);
            data.put("currentCaseCount", 0);
            data.put("closedCaseCount", casesToClose);
            return ApiResult.success("强制满垛成功", data);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("强制满垛失败：" + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
    }

    /** 当前垛内未成垛的箱数（不同 BigSerialNumber 且 VirtualSerialNumber 为空） */
    public int countCurrentCasesInPallet(String orderNo) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT BigSerialNumber) FROM CodeRelationUpload " +
                        "WHERE OrderNo = ? AND IsDel = 0 AND (VirtualSerialNumber IS NULL OR VirtualSerialNumber = '') " +
                        "AND BigSerialNumber IS NOT NULL AND BigSerialNumber != ''",
                Integer.class, orderNo.trim());
        return n != null ? n : 0;
    }

    /** 成垛：取当前未成垛的前 limit 个箱（按该箱最小 ID 排序），生成虚拟垛标并更新这些记录的 VirtualSerialNumber */
    private String completeCurrentPallet(String orderNo, int limit) throws Exception {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT BigSerialNumber, MIN(ID) AS minId FROM CodeRelationUpload " +
                        "WHERE OrderNo = ? AND IsDel = 0 AND (VirtualSerialNumber IS NULL OR VirtualSerialNumber = '') " +
                        "AND BigSerialNumber IS NOT NULL AND BigSerialNumber != '' " +
                        "GROUP BY BigSerialNumber ORDER BY minId ASC LIMIT " + limit,
                orderNo.trim());
        if (rows == null || rows.isEmpty()) return null;
        List<String> toClose = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object v = row.get("BigSerialNumber");
            if (v != null && !v.toString().isEmpty()) toClose.add(v.toString());
        }
        if (toClose.isEmpty()) return null;

        String palletCode = virtualPalletSequenceService.generateNextVirtualSerialNumber();
        String placeholders = String.join(",", Collections.nCopies(toClose.size(), "?"));
        List<Object> args = new ArrayList<>();
        args.add(palletCode);
        args.add(palletCode);
        args.add(orderNo.trim());
        args.addAll(toClose);
        jdbcTemplate.update(
                "UPDATE CodeRelationUpload SET VirtualSerialNumber = ?, BiggerSerialNumber = ? " +
                        "WHERE OrderNo = ? AND IsDel = 0 AND BigSerialNumber IN (" + placeholders + ")",
                args.toArray());
        return palletCode;
    }

    private Map<String, Object> getCurrentTaskByOrderNo(String orderNo) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT po.OrderNo AS orderNo, pod.ProductNO AS productNo FROM ProductionOrder po " +
                        "LEFT JOIN ProductionOrderDetail pod ON po.Id = pod.OrderId AND (pod.IsDel = 0 OR pod.IsDel IS NULL) " +
                        "WHERE po.OrderNo = ? AND po.OrderStatus = 1 ORDER BY po.Id DESC LIMIT 1",
                orderNo.trim());
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    private boolean isCodeInPackage(String codeValue, int packageType) {
        Long c = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM CodePackageItemHot h INNER JOIN CodePackageImport i ON h.ImportId = i.Id " +
                        "WHERE i.PackageType = ? AND i.Status = 1 AND h.CodeValue = ?",
                Long.class, packageType, codeValue);
        return c != null && c > 0;
    }

    private boolean isCaseCodeAlreadyUsed(String orderNo, String caseCode) {
        Long c = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM CodeRelationUpload WHERE OrderNo = ? AND IsDel = 0 AND BigSerialNumber = ?",
                Long.class, orderNo.trim(), caseCode);
        return c != null && c > 0;
    }

    private boolean isBoxCodeAlreadyUsed(String orderNo, String boxCode) {
        Long c = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM CodeRelationUpload WHERE OrderNo = ? AND IsDel = 0 AND MediumSerialNumber = ?",
                Long.class, orderNo.trim(), boxCode);
        return c != null && c > 0;
    }

    /** 已生产垛数：该订单下不同 VirtualSerialNumber 的数量（非空且 IsDel=0）。 */
    public int getProducedPalletCount(String orderNo) {
        if (orderNo == null || orderNo.trim().isEmpty()) return 0;
        try {
            Long n = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT VirtualSerialNumber) FROM CodeRelationUpload " +
                            "WHERE OrderNo = ? AND IsDel = 0 AND VirtualSerialNumber IS NOT NULL AND VirtualSerialNumber != ''",
                    Long.class, orderNo.trim());
            return n != null ? n.intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /** 总剔除数：RejectRecord 表按 OrderNo 统计条数。 */
    public int getRejectCount(String orderNo) {
        if (orderNo == null || orderNo.trim().isEmpty()) return 0;
        try {
            Long n = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM RejectRecord WHERE OrderNo = ?",
                    Long.class, orderNo.trim());
            return n != null ? n.intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
