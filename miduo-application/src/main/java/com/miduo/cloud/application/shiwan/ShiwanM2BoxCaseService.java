package com.miduo.cloud.application.shiwan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miduo.cloud.common.config.ShiwanM2SettingsDto;
import com.miduo.cloud.common.config.ShiwanM2SettingsFileLoader;
import com.miduo.cloud.common.dto.ApiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 石湾 2 号机盒箱关联与成垛服务。
 * 盒箱关联：校验 N 盒+1 箱码，写入 CodeRelationUpload；满 M 箱时成垛（写虚拟垛标）。
 * 待剔除 Status=3 与剔除记录表本期不实现。
 */
@Service
public class ShiwanM2BoxCaseService {

    private static final Logger log = LoggerFactory.getLogger(ShiwanM2BoxCaseService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter LOG_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String TAG_M2_BOX = "M2-BOX";
    private static final String EMPTY = "";

    /** 单线程定时器，用于成垛 5 分钟后轮询上传结果 */
    private final ScheduledExecutorService pollScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "pallet-upload-poller");
        t.setDaemon(true);
        return t;
    });

    @PreDestroy
    public void shutdownPollScheduler() {
        pollScheduler.shutdownNow();
    }

    private final JdbcTemplate jdbcTemplate;
    private final VirtualPalletSequenceService virtualPalletSequenceService;
    private final TransactionTemplate transactionTemplate;

    public ShiwanM2BoxCaseService(JdbcTemplate jdbcTemplate,
                                  VirtualPalletSequenceService virtualPalletSequenceService,
                                  PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.virtualPalletSequenceService = virtualPalletSequenceService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
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
                        EMPTY, EMPTY, now, 0, EMPTY, 0, 0, EMPTY);
            }

            // 落冷：箱码（大标）和盒码（中标）从热表迁移到冷表
            dropCodeToCold(3, caseCode);
            for (String boxCode : trimmedBoxCodes) {
                dropCodeToCold(2, boxCode);
            }

            // 当前垛内箱数（VirtualSerialNumber 为空的不同 BigSerialNumber 数）
            int currentCaseCount = countCurrentCasesInPallet(orderNo);
            boolean fullPallet = currentCaseCount >= boxesPerPallet;
            String palletCode = null;
            if (fullPallet) {
                palletCode = completeCurrentPallet(orderNo, useProductNo, boxesPerPallet);
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
     * 接收盒码时校验：CodeRelationUpload 中该 MediumSerialNumber 对应 exactly requiredRows 条，
     * 且 SmallSerialNumber 互不相同，且 BigSerialNumber 均为空，则视为正确可用的盒码（不落库，由调用方放入内存队列）。
     */
    public ApiResult<Void> validateBoxCodeForReceive(String orderNo, String productNo, String boxCode, int requiredRows) {
        if (orderNo == null || orderNo.trim().isEmpty()) {
            return ApiResult.error(400, "订单号不能为空");
        }
        if (productNo == null) productNo = "";
        if (boxCode == null || (boxCode = boxCode.trim()).isEmpty()) {
            return ApiResult.error(400, "盒码不能为空");
        }
        try {
            Map<String, Object> task = getCurrentTaskByOrderNo(orderNo);
            if (task == null) {
                return ApiResult.error(400, "未找到进行中的任务或订单号不匹配");
            }
            String useProductNo = (productNo != null && !productNo.isEmpty()) ? productNo : (String) task.get("productNo");
            if (useProductNo == null) useProductNo = "";
            // 中标码包热表校验（错误信息含"中标码包"，供 parseBoxRejectReason 识别为"中标不通过"）
            if (!isCodeInPackage(boxCode, 2)) {
                return ApiResult.error(400, "盒码不在中标码包热表中：" + boxCode);
            }
            Integer total = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM CodeRelationUpload WHERE MediumSerialNumber = ? AND IsDel = 0 " +
                            "AND (BigSerialNumber IS NULL OR BigSerialNumber = '')",
                    Integer.class, boxCode);
            if (total == null || total != requiredRows) {
                return ApiResult.error(400, "盒码对应条数不符（需" + requiredRows + "条）：" + boxCode + "，当前" + (total != null ? total : 0) + "条");
            }
            return ApiResult.success("盒码校验通过", null);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("盒码校验失败：" + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
    }

    /**
     * 箱码与内存队列中的 N 个盒码关联：将 CodeRelationUpload 中 MediumSerialNumber 属于 boxCodes 且 BigSerialNumber 为空的记录更新 BigSerialNumber = caseCode。
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResult<Map<String, Object>> associateCaseCodeWithBoxCodes(String orderNo, String productNo,
                                                                        String caseCode, List<String> boxCodes, int boxesPerPallet) {
        if (orderNo == null || orderNo.trim().isEmpty()) {
            return ApiResult.error(400, "订单号不能为空");
        }
        if (productNo == null) productNo = "";
        if (caseCode == null || (caseCode = caseCode.trim()).isEmpty()) {
            return ApiResult.error(400, "箱码不能为空");
        }
        if (boxCodes == null || boxCodes.isEmpty()) {
            return ApiResult.error(400, "盒码列表不能为空");
        }
        try {
            Map<String, Object> task = getCurrentTaskByOrderNo(orderNo);
            if (task == null) {
                return ApiResult.error(400, "未找到进行中的任务或订单号不匹配");
            }
            String useProductNo = (productNo != null && !productNo.isEmpty()) ? productNo : (String) task.get("productNo");
            if (useProductNo == null) useProductNo = "";
            if (!isCodeInPackage(caseCode, 3)) {
                return ApiResult.error(400, "箱码不在大标码包内：" + caseCode);
            }
            if (isCaseCodeAlreadyUsed(orderNo, caseCode)) {
                return ApiResult.error(400, "箱码已使用（重码）：" + caseCode);
            }
            String placeholders = String.join(",", Collections.nCopies(boxCodes.size(), "?"));
            List<Object> args = new ArrayList<>();
            args.add(caseCode);
            args.add(orderNo.trim());
            args.add(useProductNo);
            args.addAll(boxCodes);
            int updated = jdbcTemplate.update(
                    "UPDATE CodeRelationUpload SET BigSerialNumber = ?, OrderNo = ?, ProductNO = ?, TagNo = '' " +
                            "WHERE IsDel = 0 " +
                            "AND MediumSerialNumber IN (" + placeholders + ") AND (BigSerialNumber IS NULL OR BigSerialNumber = '')",
                    args.toArray());
            if (updated <= 0) {
                return ApiResult.error(400, "未找到可关联的盒码记录，箱码：" + caseCode);
            }
            // 落冷：箱码（大标）和盒码（中标）从热表迁移到冷表
            dropCodeToCold(3, caseCode);
            for (String boxCode : boxCodes) {
                dropCodeToCold(2, boxCode);
            }
            int currentCaseCount = countCurrentCasesInPallet(orderNo);
            boolean fullPallet = currentCaseCount >= boxesPerPallet;
            String palletCode = null;
            if (fullPallet) {
                palletCode = completeCurrentPallet(orderNo, useProductNo, boxesPerPallet);
                currentCaseCount = 0;
            }
            Map<String, Object> data = new HashMap<>();
            data.put("success", true);
            data.put("caseCode", caseCode);
            data.put("boxCodes", boxCodes);
            data.put("currentCaseCount", currentCaseCount);
            data.put("boxesPerPallet", boxesPerPallet);
            data.put("fullPallet", fullPallet);
            if (fullPallet && palletCode != null) data.put("palletCode", palletCode);
            return ApiResult.success("盒箱关联成功", data);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("盒箱关联失败：" + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
    }

    /** 当前订单下未关联箱码的盒码条数（BigSerialNumber 为空） */
    public int getUnassociatedBoxCount(String orderNo, String productNo) {
        if (orderNo == null || orderNo.trim().isEmpty()) return 0;
        try {
            if (productNo != null && !productNo.isEmpty()) {
                Integer n = jdbcTemplate.queryForObject(
                        "SELECT COUNT(1) FROM CodeRelationUpload WHERE OrderNo = ? AND IsDel = 0 AND (ProductNO IS NULL OR ProductNO = '' OR ProductNO = ?) " +
                                "AND (BigSerialNumber IS NULL OR BigSerialNumber = '')",
                        Integer.class, orderNo.trim(), productNo);
                return n != null ? n : 0;
            } else {
                Integer n = jdbcTemplate.queryForObject(
                        "SELECT COUNT(1) FROM CodeRelationUpload WHERE OrderNo = ? AND IsDel = 0 " +
                                "AND (BigSerialNumber IS NULL OR BigSerialNumber = '')",
                        Integer.class, orderNo.trim());
                return n != null ? n : 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 收到箱码时触发关联：查当前未关联的前 N 条盒码，若够 N 条则更新 BigSerialNumber 并成垛逻辑；不足则返回 PENDING 供调用方将箱码存入待处理列表。
     * @return 成功时 code=200；不足 N 条时 code=202、message 含 PENDING，调用方应将箱码存入内存待处理列表。
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResult<Map<String, Object>> associateCaseCodeWithPendingBoxes(String orderNo, String productNo,
                                                                            String caseCode, int boxesPerCase, int boxesPerPallet) {
        if (orderNo == null || orderNo.trim().isEmpty()) {
            return ApiResult.error(400, "订单号不能为空");
        }
        if (productNo == null) productNo = "";
        if (caseCode == null || (caseCode = caseCode.trim()).isEmpty()) {
            return ApiResult.error(400, "箱码不能为空");
        }
        try {
            Map<String, Object> task = getCurrentTaskByOrderNo(orderNo);
            if (task == null) {
                return ApiResult.error(400, "未找到进行中的任务或订单号不匹配");
            }
            String useProductNo = (productNo != null && !productNo.isEmpty()) ? productNo : (String) task.get("productNo");
            if (useProductNo == null) useProductNo = "";
            if (!isCodeInPackage(caseCode, 3)) {
                return ApiResult.error(400, "箱码不在大标码包内：" + caseCode);
            }
            if (isCaseCodeAlreadyUsed(orderNo, caseCode)) {
                return ApiResult.error(400, "箱码已使用（重码）：" + caseCode);
            }
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT ID, MediumSerialNumber FROM CodeRelationUpload WHERE IsDel = 0 " +
                            "AND (OrderNo IS NULL OR OrderNo = '' OR OrderNo = ?) " +
                            "AND (BigSerialNumber IS NULL OR BigSerialNumber = '') ORDER BY ID ASC LIMIT " + boxesPerCase,
                    orderNo.trim());
            if (rows == null || rows.size() < boxesPerCase) {
                ApiResult<Map<String, Object>> pendingResult = new ApiResult<>();
                pendingResult.setCode(202);
                pendingResult.setMessage("未关联盒码不足" + boxesPerCase + "条，请将箱码存入待处理列表");
                Map<String, Object> pending = new HashMap<>();
                pending.put("pending", true);
                pending.put("unassociatedCount", rows != null ? rows.size() : 0);
                pendingResult.setData(pending);
                return pendingResult;
            }
            List<Long> ids = new ArrayList<>();
            List<String> pendingBoxCodes = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                Object id = row.get("ID");
                if (id instanceof Number) ids.add(((Number) id).longValue());
                String med = toStr(row.get("MediumSerialNumber"));
                if (!med.isEmpty()) pendingBoxCodes.add(med);
            }
            if (ids.size() < boxesPerCase) {
                ApiResult<Map<String, Object>> pendingResult = new ApiResult<>();
                pendingResult.setCode(202);
                pendingResult.setMessage("未关联盒码不足" + boxesPerCase + "条");
                Map<String, Object> pending = new HashMap<>();
                pending.put("pending", true);
                pending.put("unassociatedCount", ids.size());
                pendingResult.setData(pending);
                return pendingResult;
            }
            String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
            List<Object> args = new ArrayList<>();
            args.add(caseCode);
            args.add(orderNo.trim());
            args.add(useProductNo);
            args.addAll(ids);
            jdbcTemplate.update(
                    "UPDATE CodeRelationUpload SET BigSerialNumber = ?, OrderNo = ?, ProductNO = ?, TagNo = '' " +
                            "WHERE IsDel = 0 AND ID IN (" + placeholders + ")",
                    args.toArray());
            // 落冷：箱码（大标）和盒码（中标）从热表迁移到冷表
            dropCodeToCold(3, caseCode);
            for (String boxCode : pendingBoxCodes) {
                dropCodeToCold(2, boxCode);
            }
            int currentCaseCount = countCurrentCasesInPallet(orderNo);
            boolean fullPallet = currentCaseCount >= boxesPerPallet;
            String palletCode = null;
            if (fullPallet) {
                palletCode = completeCurrentPallet(orderNo, useProductNo, boxesPerPallet);
                currentCaseCount = 0;
            }
            Map<String, Object> data = new HashMap<>();
            data.put("success", true);
            data.put("caseCode", caseCode);
            data.put("currentCaseCount", currentCaseCount);
            data.put("boxesPerCase", boxesPerCase);
            data.put("boxesPerPallet", boxesPerPallet);
            data.put("fullPallet", fullPallet);
            if (fullPallet && palletCode != null) data.put("palletCode", palletCode);
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
            Map<String, Object> task = getCurrentTaskByOrderNo(orderNo);
            String productNo = task != null ? toStr(task.get("productNo")) : "";
            int casesToClose = currentCaseCount != null && currentCaseCount > 0 ? currentCaseCount : countCurrentCasesInPallet(orderNo);
            if (casesToClose <= 0) {
                Map<String, Object> data = new HashMap<>();
                data.put("palletCode", null);
                data.put("currentCaseCount", 0);
                return ApiResult.success("当前无未成垛箱", data);
            }
            String palletCode = completeCurrentPallet(orderNo, productNo, casesToClose);
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

    /** 当前垛内未成垛的箱数（不同 BigSerialNumber 且 VirtualSerialNumber = ''）
     * 注：VirtualSerialNumber NOT NULL，插入时固定写 ''，无需 IS NULL 判断，便于 idx_order_del_virtual 索引生效。
     */
    public int countCurrentCasesInPallet(String orderNo) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT BigSerialNumber) FROM CodeRelationUpload " +
                        "WHERE OrderNo = ? AND IsDel = 0 AND VirtualSerialNumber = '' " +
                        "AND BigSerialNumber != ''",
                Integer.class, orderNo.trim());
        return n != null ? n : 0;
    }

    /** 成垛：取当前未成垛的前 limit 个箱（按该箱最小 ID 排序），生成虚拟垛标并更新这些记录的 VirtualSerialNumber，完成后异步上传至开放平台。 */
    private String completeCurrentPallet(String orderNo, String productNo, int limit) throws Exception {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT BigSerialNumber, MIN(ID) AS minId FROM CodeRelationUpload " +
                        "WHERE OrderNo = ? AND IsDel = 0 AND VirtualSerialNumber = '' " +
                        "AND BigSerialNumber != '' " +
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
        int    boxCount   = toClose.size();
        String placeholders = String.join(",", Collections.nCopies(boxCount, "?"));
        List<Object> args = new ArrayList<>();
        args.add(palletCode);
        args.add(orderNo.trim());
        args.addAll(toClose);
        jdbcTemplate.update(
                "UPDATE CodeRelationUpload SET VirtualSerialNumber = ?, Status = 1 " +
                        "WHERE OrderNo = ? AND IsDel = 0 AND BigSerialNumber IN (" + placeholders + ")",
                args.toArray());

        // 仅当系统设置→业务→上传配置中「自动上传开关」开启时才自动上传
        ShiwanM2SettingsDto cfg = ShiwanM2SettingsFileLoader.load();
        boolean autoUpload = cfg == null || cfg.getUpload() == null || cfg.getUpload().isAutoUpload();
        if (autoUpload) {
            syncVirtualPalletToCloud(orderNo, productNo, palletCode, boxCount);
        }
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
                "SELECT COUNT(1) FROM CodeRelationUpload WHERE BigSerialNumber = ? AND IsDel = 0",
                Long.class, caseCode);
        return c != null && c > 0;
    }

    /** 盒码已使用：仅当表中存在该盒码且已被箱码关联（BigSerialNumber 有值）时才算已使用 */
    private boolean isBoxCodeAlreadyUsed(String orderNo, String boxCode) {
        Long c = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM CodeRelationUpload WHERE MediumSerialNumber = ? AND IsDel = 0 " +
                        "AND BigSerialNumber != ''",
                Long.class, boxCode);
        return c != null && c > 0;
    }

    /**
     * 手工采集码校验（供前端 P02-02 调用）。
     * packageType: 1=小码(瓶码), 2=中码(盒码)。
     * 检查项：
     * 1. 码是否在热表（CodePackageItemHot JOIN CodePackageImport WHERE PackageType=packageType）
     * 2. CodeRelationUpload 中是否已存在该码的关联关系（重码）
     */
    public ApiResult<Map<String, Object>> validateManualCode(String code, int packageType) {
        if (code == null || code.trim().isEmpty()) {
            return ApiResult.error(400, "码值不能为空");
        }
        code = code.trim();
        if (packageType != 1 && packageType != 2) {
            return ApiResult.error(400, "packageType 仅支持 1（瓶码）或 2（盒码）");
        }
        String codeTypeName = packageType == 1 ? "瓶码" : "盒码";
        try {
            if (!isCodeInPackage(code, packageType)) {
                return ApiResult.error(400, codeTypeName + "不在有效码包热表中：" + code);
            }
            boolean hasRelation;
            if (packageType == 1) {
                Long c = jdbcTemplate.queryForObject(
                        "SELECT COUNT(1) FROM CodeRelationUpload WHERE SmallSerialNumber = ? AND IsDel = 0",
                        Long.class, code);
                hasRelation = c != null && c > 0;
            } else {
                Long c = jdbcTemplate.queryForObject(
                        "SELECT COUNT(1) FROM CodeRelationUpload WHERE MediumSerialNumber = ? AND IsDel = 0",
                        Long.class, code);
                hasRelation = c != null && c > 0;
            }
            if (hasRelation) {
                return ApiResult.error(400, codeTypeName + "已存在码关系（重码）：" + code);
            }
            Map<String, Object> data = new HashMap<>();
            data.put("code", code);
            data.put("packageType", packageType);
            return ApiResult.success("校验通过", data);
        } catch (Exception e) {
            log.error("[手工采集校验] 异常 code={} packageType={}", code, packageType, e);
            return ApiResult.error("校验异常：" + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
    }

    /** 已生产垛数：该订单下不同 VirtualSerialNumber 的数量（非空且 IsDel=0）。
     * 注：VirtualSerialNumber NOT NULL，去掉 IS NOT NULL 条件，可命中 idx_order_del_virtual 索引。
     */
    public int getProducedPalletCount(String orderNo) {
        if (orderNo == null || orderNo.trim().isEmpty()) return 0;
        try {
            Long n = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT VirtualSerialNumber) FROM CodeRelationUpload " +
                            "WHERE OrderNo = ? AND IsDel = 0 AND VirtualSerialNumber != ''",
                    Long.class, orderNo.trim());
            return n != null ? n.intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 写入单条剔除记录到 RejectRecord 表。
     * <p>
     * 字段说明（对应需求文档 10.1.6）：
     * <ul>
     *   <li>caseCode   — 被剔除箱的箱码（必填）</li>
     *   <li>boxCode    — 问题盒码；大标/重码场景为 null</li>
     *   <li>bottleCode — 问题瓶码；当前 TCP 采集场景暂为 null</li>
     *   <li>problemCode— 导致剔除的具体码（大标/重码时为箱码，中标不通过时为盒码，盒箱校验不通过时为 null）</li>
     *   <li>rejectReason— 枚举文本：大标不通过/重码/盒箱校验不通过/中标不通过 等</li>
     * </ul>
     * 写入失败仅记录日志，不向上抛异常，避免影响主流程。
     */
    public void insertRejectRecord(String orderNo, String caseCode, String boxCode,
                                   String bottleCode, String problemCode, String rejectReason) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO RejectRecord (OrderNo, CaseCode, BoxCode, BottleCode, ProblemCode, RejectReason, RejectTime) " +
                    "VALUES (?, ?, ?, ?, ?, ?, NOW())",
                    orderNo, caseCode, boxCode, bottleCode, problemCode, rejectReason);
            log.info("[剔除记录] 写入成功 orderNo={} caseCode={} boxCode={} problemCode={} reason={}",
                    orderNo, caseCode, boxCode, problemCode, rejectReason);
        } catch (Exception e) {
            log.error("[剔除记录] 写入失败 orderNo={} caseCode={} boxCode={} reason={}: {}",
                    orderNo, caseCode, boxCode, rejectReason, e.getMessage(), e);
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

    /**
     * P02-04 数据查询：输入任意层级码，展开该码所属垛的全部关联关系。
     */
    public Map<String, Object> queryCodeAssociation(String code) {
        if (code == null || code.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        String inputCode = code.trim();
        List<Map<String, Object>> palletRows = jdbcTemplate.queryForList(
                "SELECT VirtualSerialNumber FROM CodeRelationUpload " +
                        "WHERE (SmallSerialNumber = ? OR MediumSerialNumber = ? OR BigSerialNumber = ? OR VirtualSerialNumber = ?) " +
                        "AND VirtualSerialNumber != '' LIMIT 1",
                inputCode, inputCode, inputCode, inputCode);
        if (palletRows == null || palletRows.isEmpty()) {
            return Collections.emptyMap();
        }
        String palletCode = toStr(palletRows.get(0).get("VirtualSerialNumber"));
        if (palletCode.isEmpty()) return Collections.emptyMap();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT c.SmallSerialNumber AS bottleCode, c.MediumSerialNumber AS boxCode, " +
                        "c.BigSerialNumber AS caseCode, c.VirtualSerialNumber AS palletCode, c.AddTime AS collectTime, " +
                        "c.ProductNO AS productNo, c.OrderNo AS orderNo, p.ProductName AS productName " +
                        "FROM CodeRelationUpload c " +
                        "LEFT JOIN ProductInfo p ON p.ProductNo = c.ProductNO " +
                        "WHERE c.VirtualSerialNumber = ? " +
                        "ORDER BY CASE WHEN (c.SmallSerialNumber = ? OR c.MediumSerialNumber = ? " +
                        "OR c.BigSerialNumber = ? OR c.VirtualSerialNumber = ?) THEN 0 ELSE 1 END, " +
                        "c.BigSerialNumber, c.MediumSerialNumber, c.SmallSerialNumber",
                palletCode, inputCode, inputCode, inputCode, inputCode);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("rows", rows == null ? Collections.emptyList() : rows);
        data.put("total", rows == null ? 0 : rows.size());
        data.put("inputCode", inputCode);
        data.put("palletCode", palletCode);
        return data;
    }

    /**
     * P02-05 取消关联：按箱码删除关联记录，并将箱码（大标）和对应盒码（中标）从冷表回迁到热表。
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResult<Boolean> cancelByCaseCode(String caseCode) {
        if (caseCode == null || caseCode.trim().isEmpty()) {
            return ApiResult.error(400, "箱码不能为空");
        }
        String code = caseCode.trim();
        // 取消前先查出该箱对应的所有盒码，用于后续冷表回迁
        List<Map<String, Object>> boxRows = jdbcTemplate.queryForList(
                "SELECT DISTINCT MediumSerialNumber FROM CodeRelationUpload WHERE BigSerialNumber = ? AND IsDel = 0",
                code);
        int rows = jdbcTemplate.update("DELETE FROM CodeRelationUpload WHERE BigSerialNumber = ?", code);
        if (rows <= 0) {
            return ApiResult.error(404, "未找到可取消的数据：" + code);
        }
        // 箱码（大标）回迁热表
        returnCodeToHot(3, code);
        // 盒码（中标）回迁热表
        for (Map<String, Object> row : boxRows) {
            String boxCode = toStr(row.get("MediumSerialNumber"));
            if (!boxCode.isEmpty()) {
                returnCodeToHot(2, boxCode);
            }
        }
        return ApiResult.success("取消关联成功", true);
    }

    /**
     * P02-06 数据替换：按层级直接更新对应列码值，并同步热冷表。
     * 流程：原码存在于冷表（已被关联）→ 验证新码在热表中（未使用）→ 更新关联表 → 原码从冷表回迁热表 → 新码从热表落冷。
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResult<Boolean> replaceCode(String oldCode, String newCode, String reason) {
        // 1. 基础入参校验
        if (oldCode == null || oldCode.trim().isEmpty()) return ApiResult.error(400, "原码不能为空");
        if (newCode == null || newCode.trim().isEmpty()) return ApiResult.error(400, "新码不能为空");
        String oldV = oldCode.trim();
        String newV = newCode.trim();
        if (oldV.equals(newV)) return ApiResult.error(400, "原码和新码不能相同");

        // 2. 查询原码所在层级
        long smallCnt  = countByColumn("SmallSerialNumber",  oldV);
        long mediumCnt = countByColumn("MediumSerialNumber", oldV);
        long bigCnt    = countByColumn("BigSerialNumber",    oldV);
        int hitLayers  = (smallCnt > 0 ? 1 : 0) + (mediumCnt > 0 ? 1 : 0) + (bigCnt > 0 ? 1 : 0);
        if (hitLayers == 0) return ApiResult.error(404, "原码不存在：" + oldV);
        if (hitLayers > 1)  return ApiResult.error(400, "原码命中多个层级，不允许替换：" + oldV);

        // 3. 确定层级、列名和云端码类型（0=小标/瓶码, 1=中标/盒码, 2=大标/箱码）
        int    packageType;
        String column;
        int    codeType;
        if (smallCnt > 0) {
            packageType = 1; column = "SmallSerialNumber";  codeType = 0;
        } else if (mediumCnt > 0) {
            packageType = 2; column = "MediumSerialNumber"; codeType = 1;
        } else {
            packageType = 3; column = "BigSerialNumber";    codeType = 2;
        }

        // 4. 校验新码：必须在对应码包热表中且未被使用
        if (!isCodeInPackage(newV, packageType)) {
            return ApiResult.error(400, "新码不在对应码包热表中（未使用）：" + newV);
        }
        if (countByColumn("SmallSerialNumber",   newV) > 0
                || countByColumn("MediumSerialNumber",  newV) > 0
                || countByColumn("BigSerialNumber",     newV) > 0
                || countByColumn("VirtualSerialNumber", newV) > 0) {
            return ApiResult.error(400, "新码已存在于关联表，不允许替换：" + newV);
        }

        // 5. 查询原码记录的最大上传状态
        Long maxIsUploadVal = jdbcTemplate.queryForObject(
                "SELECT MAX(IsUpload) FROM CodeRelationUpload WHERE " + column + " = ? AND IsDel = 0",
                Long.class, oldV);
        int uploadStatus = maxIsUploadVal == null ? 0 : maxIsUploadVal.intValue();

        // IsUpload=3 表示正在上传中，禁止替换
        if (uploadStatus == 3) {
            return ApiResult.error(400, "原码正在上传中，请等待上传完成后再进行替换");
        }

        // 6. 若已上传（IsUpload=1 成功）或上传过（IsUpload=2 失败），先替换云端
        if (uploadStatus == 1 || uploadStatus == 2) {
            ApiResult<Boolean> cloudResult = callCloudCodeSubstitution(oldV, newV, codeType);
            if (cloudResult != null && cloudResult.getCode() != 200) {
                return cloudResult;
            }
        }

        // 7. 本地数据库操作（事务保护：全部成功或全部回滚）
        try {
            transactionTemplate.execute(status -> {
                int updated = jdbcTemplate.update(
                        "UPDATE CodeRelationUpload SET " + column + " = ? WHERE " + column + " = ?", newV, oldV);
                if (updated <= 0) {
                    throw new RuntimeException("替换失败，未更新到记录");
                }
                // 原码从冷表回迁热表（可重新使用）
                returnCodeToHot(packageType, oldV);
                // 新码从热表落冷（标记为已使用）
                dropCodeToCold(packageType, newV);
                return null;
            });
        } catch (RuntimeException e) {
            log.error("[码替换] 本地数据库更新失败 oldCode={} newCode={}: {}", oldV, newV, e.getMessage(), e);
            return ApiResult.error(500, e.getMessage());
        }

        return ApiResult.success("替换成功", true);
    }

    /**
     * 调用云端码替换接口（CodeSubstitution）。
     * 仅当原码已上传（IsUpload=1）或曾尝试上传（IsUpload=2）时调用。
     * 若配置缺失则跳过（返回 null）；若接口调用失败则返回错误结果。
     *
     * @param codeType 0=小标/瓶码, 1=中标/盒码, 2=大标/箱码
     */
    private ApiResult<Boolean> callCloudCodeSubstitution(String oldV, String newV, int codeType) {
        ShiwanM2SettingsDto cfg = ShiwanM2SettingsFileLoader.load();
        if (cfg == null || cfg.getApi() == null) {
            log.warn("[码替换云端] 未配置API，跳过云端替换 oldCode={}", oldV);
            return null;
        }
        String baseUrl = trimStr(cfg.getApi().getBaseUrl());
        String path    = trimStr(cfg.getApi().getCodeSubstitutionPath());
        if (baseUrl == null || path == null) {
            log.warn("[码替换云端] 缺少 baseUrl 或 codeSubstitutionPath，跳过 oldCode={}", oldV);
            return null;
        }
        String appId      = cfg.getApi().getAppId();
        String appSecret  = cfg.getApi().getAppSecret();
        String memberlogin = cfg.getApi().getMemberlogin();

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String time  = sdf.format(new Date());
            String nonce = "123";

            Map<String, Object> requestData = new LinkedHashMap<>();
            requestData.put("Memberlogin", memberlogin != null ? memberlogin : "");
            requestData.put("oldCode", oldV);
            requestData.put("newCode", newV);
            requestData.put("codeType", codeType);

            String sign = palletSign(time, nonce, appSecret, requestData);

            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("timestamp",    time);
            headers.put("appid",        appId != null ? appId : "");
            headers.put("nonce",        nonce);
            headers.put("sign",         sign);
            headers.put("Content-type", "application/json");

            String response = sendPost(baseUrl + path, headers, MAPPER.writeValueAsString(requestData));
            log.info("[码替换云端] oldCode={} newCode={} codeType={} 响应: {}", oldV, newV, codeType, response);

            JsonNode root = MAPPER.readTree(response);
            int returnCode = root.path("Return_code").asInt(-1);
            if (returnCode != 0) {
                String msg = root.path("Return_msg").asText("未知错误");
                return ApiResult.error(500, "云端替换失败：" + msg);
            }
            JsonNode dataArr = root.path("Return_data");
            if (dataArr.isArray() && dataArr.size() > 0) {
                int status = dataArr.get(0).path("status").asInt(-1);
                if (status != 0) {
                    return ApiResult.error(500, "云端替换返回失败状态：" + status);
                }
            }
            return ApiResult.success("云端替换成功", true);
        } catch (Exception e) {
            log.error("[码替换云端] 接口调用异常 oldCode={}: {}", oldV, e.getMessage(), e);
            return ApiResult.error(500, "云端替换接口调用异常：" + e.getMessage());
        }
    }

    public Map<String, Object> getProductionSummary(String startDate, String endDate, String orderNo) {
        Timestamp start = parseStartDate(startDate);
        Timestamp end = parseEndDate(endDate);
        StringBuilder commonWhere = new StringBuilder(" FROM CodeRelationUpload WHERE AddTime BETWEEN ? AND ? ");
        List<Object> commonArgs = new ArrayList<>();
        commonArgs.add(start);
        commonArgs.add(end);
        if (orderNo != null && !orderNo.trim().isEmpty()) {
            commonWhere.append(" AND OrderNo LIKE ? ");
            commonArgs.add("%" + orderNo.trim() + "%");
        }

        long palletCount = queryLong("SELECT COUNT(DISTINCT VirtualSerialNumber) " + commonWhere + " AND VirtualSerialNumber != ''", commonArgs);
        long caseCount = queryLong("SELECT COUNT(DISTINCT BigSerialNumber) " + commonWhere + " AND BigSerialNumber != ''", commonArgs);
        long boxCount = queryLong("SELECT COUNT(DISTINCT MediumSerialNumber) " + commonWhere + " AND MediumSerialNumber != ''", commonArgs);

        StringBuilder rejectWhere = new StringBuilder(" FROM RejectRecord WHERE RejectTime BETWEEN ? AND ? ");
        List<Object> rejectArgs = new ArrayList<>();
        rejectArgs.add(start);
        rejectArgs.add(end);
        if (orderNo != null && !orderNo.trim().isEmpty()) {
            rejectWhere.append(" AND OrderNo LIKE ? ");
            rejectArgs.add("%" + orderNo.trim() + "%");
        }
        long rejectCaseCount = queryLong("SELECT COUNT(DISTINCT CaseCode) " + rejectWhere, rejectArgs);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("palletCount", palletCount);
        data.put("caseCount", caseCount);
        data.put("boxCount", boxCount);
        data.put("rejectCount", rejectCaseCount);
        return data;
    }

    public Map<String, Object> getPalletList(String startDate, String endDate, String orderNo, String palletCode, int page, int pageSize) {
        Timestamp start = parseStartDate(startDate);
        Timestamp end = parseEndDate(endDate);
        int current = Math.max(1, page);
        int size = normalizePageSize(pageSize);
        int offset = (current - 1) * size;

        StringBuilder where = new StringBuilder(" FROM CodeRelationUpload WHERE AddTime BETWEEN ? AND ? AND VirtualSerialNumber != '' ");
        List<Object> args = new ArrayList<>();
        args.add(start);
        args.add(end);
        if (orderNo != null && !orderNo.trim().isEmpty()) {
            where.append(" AND OrderNo LIKE ? ");
            args.add("%" + orderNo.trim() + "%");
        }
        if (palletCode != null && !palletCode.trim().isEmpty()) {
            where.append(" AND VirtualSerialNumber LIKE ? ");
            args.add("%" + palletCode.trim() + "%");
        }

        long total = queryLong("SELECT COUNT(DISTINCT VirtualSerialNumber) " + where, args);
        List<Object> listArgs = new ArrayList<>(args);
        listArgs.add(size);
        listArgs.add(offset);
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "SELECT VirtualSerialNumber AS palletCode, COUNT(DISTINCT BigSerialNumber) AS caseCount, " +
                        "MAX(OrderNo) AS orderNo, MAX(AddTime) AS associateTime " +
                        where + " GROUP BY VirtualSerialNumber ORDER BY associateTime DESC LIMIT ? OFFSET ?",
                listArgs.toArray());
        return buildPageResult(records, total, current, size);
    }

    public Map<String, Object> getRejectRecords(String startDate, String endDate, String orderNo, String caseCode, int page, int pageSize) {
        Timestamp start = parseStartDate(startDate);
        Timestamp end = parseEndDate(endDate);
        int current = Math.max(1, page);
        int size = normalizePageSize(pageSize);
        int offset = (current - 1) * size;

        StringBuilder where = new StringBuilder(" WHERE r.RejectTime BETWEEN ? AND ? ");
        List<Object> args = new ArrayList<>();
        args.add(start);
        args.add(end);
        if (orderNo != null && !orderNo.trim().isEmpty()) {
            where.append(" AND r.OrderNo LIKE ? ");
            args.add("%" + orderNo.trim() + "%");
        }
        if (caseCode != null && !caseCode.trim().isEmpty()) {
            where.append(" AND r.CaseCode LIKE ? ");
            args.add("%" + caseCode.trim() + "%");
        }
        long total = queryLong("SELECT COUNT(*) FROM RejectRecord r" + where, args);
        List<Object> listArgs = new ArrayList<>(args);
        listArgs.add(size);
        listArgs.add(offset);
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "SELECT r.Id, r.OrderNo, r.CaseCode, r.BoxCode, r.BottleCode, r.ProblemCode, r.RejectReason, r.RejectTime, " +
                        "p.ProductNo, p.ProductName " +
                        "FROM RejectRecord r " +
                        "LEFT JOIN ProductInfo p ON p.ProductNo = (" +
                        "  SELECT c.ProductNO FROM CodeRelationUpload c WHERE c.OrderNo = r.OrderNo LIMIT 1" +
                        ") " +
                        where + " ORDER BY r.RejectTime DESC LIMIT ? OFFSET ?",
                listArgs.toArray());
        return buildPageResult(records, total, current, size);
    }

    public Map<String, Object> getUploadRecords(String startDate, String endDate, String orderNo, String status, int page, int pageSize) {
        Timestamp start = parseStartDate(startDate);
        Timestamp end = parseEndDate(endDate);
        int current = Math.max(1, page);
        int size = normalizePageSize(pageSize);
        int offset = (current - 1) * size;

        StringBuilder where = new StringBuilder(
                " FROM CodeRelationUpload WHERE UploadTime BETWEEN ? AND ? AND VirtualSerialNumber != '' ");
        List<Object> args = new ArrayList<>();
        args.add(start);
        args.add(end);
        if (orderNo != null && !orderNo.trim().isEmpty()) {
            where.append(" AND OrderNo LIKE ? ");
            args.add("%" + orderNo.trim() + "%");
        }
        Integer statusValue = null;
        if ("成功".equals(status)) statusValue = 1;
        if ("异常".equals(status)) statusValue = 2;

        String having = " HAVING MAX(IsUpload) IN (1, 2) ";
        List<Object> listArgsBase = new ArrayList<>(args);
        if (statusValue != null) {
            having += " AND MAX(IsUpload) = ? ";
            listArgsBase.add(statusValue);
        }

        long total = queryLong(
                "SELECT COUNT(*) FROM (" +
                        "SELECT VirtualSerialNumber " + where + " GROUP BY VirtualSerialNumber " + having +
                        ") t", listArgsBase);
        List<Object> listArgs = new ArrayList<>(listArgsBase);
        listArgs.add(size);
        listArgs.add(offset);
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "SELECT VirtualSerialNumber AS palletCode, COUNT(DISTINCT BigSerialNumber) AS caseCount, " +
                        "MAX(OrderNo) AS orderNo, MAX(UploadTime) AS uploadTime, MAX(IsUpload) AS isUpload, MAX(Msg) AS errorMsg " +
                        where + " GROUP BY VirtualSerialNumber " + having +
                        " ORDER BY MAX(UploadTime) DESC LIMIT ? OFFSET ?",
                listArgs.toArray());
        return buildPageResult(records, total, current, size);
    }

    /**
     * 检查盒码（MediumSerialNumber）关联的所有 Status=4（待剔除）记录，写入剔除记录表。
     * 返回 true 表示存在 Status=4 记录（调用方应将该盒码视为失败，不入队列）。
     */
    public boolean checkAndWriteRejectsForStatus4(String orderNo, String mediumSerialNumber) {
        List<Map<String, Object>> status4Rows = jdbcTemplate.queryForList(
                "SELECT SmallSerialNumber, Msg FROM CodeRelationUpload " +
                "WHERE MediumSerialNumber = ? AND Status = 4 AND IsDel = 0",
                mediumSerialNumber);
        if (status4Rows == null || status4Rows.isEmpty()) return false;
        for (Map<String, Object> row : status4Rows) {
            String bottleCode  = row.get("SmallSerialNumber") != null ? row.get("SmallSerialNumber").toString() : null;
            String msgReason   = row.get("Msg") != null && !row.get("Msg").toString().isEmpty()
                    ? row.get("Msg").toString() : "码包热表校验不通过";
            insertRejectRecord(orderNo, null, mediumSerialNumber, bottleCode,
                    bottleCode != null ? bottleCode : mediumSerialNumber, msgReason);
        }
        // 写入剔除记录后将这些数据从 CodeRelationUpload 中删除（M1同步的数据）
        jdbcTemplate.update(
                "UPDATE CodeRelationUpload SET IsDel = 1 " +
                "WHERE MediumSerialNumber = ? AND Status = 4 AND IsDel = 0",
                mediumSerialNumber);
        log.warn("[Status=4] 盒码 {} 关联记录 {} 条已写入剔除记录并标记删除",
                mediumSerialNumber, status4Rows.size());
        return true;
    }

    /**
     * 热表落冷：将指定码从 CodePackageItemHot 移至 CodePackageItemCold。
     * 热表中不存在时仅记录警告，不抛异常，由调用方事务统一回滚。
     */
    private void dropCodeToCold(int packageType, String codeValue) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT Id, ImportId FROM CodePackageItemHot WHERE PackageType = ? AND CodeValue = ?",
                packageType, codeValue);
        if (rows == null || rows.isEmpty()) {
            log.warn("[落冷] 热表中未找到 packageType={} code={}", packageType, codeValue);
            return;
        }
        long importId = ((Number) rows.get(0).get("ImportId")).longValue();
        jdbcTemplate.update(
                "INSERT IGNORE INTO CodePackageItemCold (ImportId, PackageType, CodeValue, AssociatedAt) " +
                "VALUES (?, ?, ?, NOW())",
                importId, packageType, codeValue);
        jdbcTemplate.update(
                "DELETE FROM CodePackageItemHot WHERE PackageType = ? AND CodeValue = ?",
                packageType, codeValue);
        log.debug("[落冷] 完成 packageType={} code={}", packageType, codeValue);
    }

    /**
     * 冷表回迁：将指定码从 CodePackageItemCold 移回 CodePackageItemHot。
     * 冷表中不存在时仅记录警告，不抛异常，由调用方事务统一回滚。
     */
    private void returnCodeToHot(int packageType, String codeValue) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT Id, ImportId FROM CodePackageItemCold WHERE PackageType = ? AND CodeValue = ?",
                packageType, codeValue);
        if (rows == null || rows.isEmpty()) {
            log.warn("[回迁] 冷表中未找到 packageType={} code={}", packageType, codeValue);
            return;
        }
        long importId = ((Number) rows.get(0).get("ImportId")).longValue();
        jdbcTemplate.update(
                "INSERT IGNORE INTO CodePackageItemHot (ImportId, PackageType, CodeValue) VALUES (?, ?, ?)",
                importId, packageType, codeValue);
        jdbcTemplate.update(
                "DELETE FROM CodePackageItemCold WHERE PackageType = ? AND CodeValue = ?",
                packageType, codeValue);
        log.debug("[回迁] 完成 packageType={} code={}", packageType, codeValue);
    }

    private long countByColumn(String column, String value) {
        Long n = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM CodeRelationUpload WHERE " + column + " = ?",
                Long.class, value);
        return n == null ? 0L : n;
    }

    private long queryLong(String sql, List<Object> args) {
        Long n = jdbcTemplate.queryForObject(sql, Long.class, args.toArray());
        return n == null ? 0L : n;
    }

    private Map<String, Object> buildPageResult(List<Map<String, Object>> records, long total, int current, int pageSize) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records == null ? Collections.emptyList() : records);
        result.put("total", total);
        result.put("current", current);
        result.put("pageSize", pageSize);
        result.put("pages", Math.max(1, (int) Math.ceil(total * 1.0 / pageSize)));
        return result;
    }

    private int normalizePageSize(int pageSize) {
        if (pageSize == 10 || pageSize == 20 || pageSize == 50 || pageSize == 100) return pageSize;
        return 20;
    }

    private Timestamp parseStartDate(String date) {
        LocalDateTime dt = (date == null || date.trim().isEmpty())
                ? LocalDate.now().withDayOfMonth(1).atStartOfDay()
                : LocalDate.parse(date.trim()).atStartOfDay();
        return Timestamp.valueOf(dt);
    }

    private Timestamp parseEndDate(String date) {
        LocalDateTime dt = (date == null || date.trim().isEmpty())
                ? LocalDate.now().atTime(23, 59, 59)
                : LocalDate.parse(date.trim()).atTime(23, 59, 59);
        return Timestamp.valueOf(dt);
    }

    /**
     * 成垛后将码关系数据上传至开放平台（虚拟垛标入库接口）。
     * 流程：① 设 IsUpload=3（上传中）→ ② 日志"开始上传"→ ③ 调接口 → ④ 日志"上传中" → ⑤ 5分钟后轮询。
     * 接口调用失败：设 IsUpload=2 + 日志"上传失败"。任何步骤异常均不影响外层事务。
     *
     * @param boxCount 本垛箱数（用于日志显示）
     */
    void syncVirtualPalletToCloud(String orderNo, String productNo, String palletCode, int boxCount) {
        ShiwanM2SettingsDto cfg = ShiwanM2SettingsFileLoader.load();
        if (cfg == null || cfg.getApi() == null) {
            log.warn("[虚拟垛标上传] 未配置开放平台API，跳过 palletCode={}", palletCode);
            return;
        }
        String baseUrl    = trimStr(cfg.getApi().getBaseUrl());
        String uploadPath = trimStr(cfg.getApi().getSyncCodeAndVirtualRelationPath());
        if (baseUrl == null || uploadPath == null) {
            log.warn("[虚拟垛标上传] 缺少 baseUrl 或 syncCodeAndVirtualRelationPath，跳过 palletCode={}", palletCode);
            return;
        }
        String appId     = cfg.getApi().getAppId();
        String appSecret = cfg.getApi().getAppSecret();

        // 查询该垛所有码关系记录
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT VirtualSerialNumber, BigSerialNumber, MediumSerialNumber, SmallSerialNumber, DxCode " +
                "FROM CodeRelationUpload WHERE VirtualSerialNumber = ? AND OrderNo = ? AND IsDel = 0",
                palletCode, orderNo.trim());
        if (rows == null || rows.isEmpty()) {
            log.warn("[虚拟垛标上传] 未查询到垛标 {} 的码关系数据，跳过上传", palletCode);
            return;
        }

        // ① 标记为"上传中"（IsUpload=3）
        try {
            jdbcTemplate.update(
                    "UPDATE CodeRelationUpload SET IsUpload = 3 WHERE VirtualSerialNumber = ? AND IsDel = 0",
                    palletCode);
        } catch (Exception e) {
            log.error("[虚拟垛标上传] 更新 IsUpload=3 失败 palletCode={}", palletCode, e);
        }

        // ② 日志：开始上传（灰色）+ 推送垛状态事件（上传中）
        String logTime = LocalDateTime.now().format(LOG_TIME_FMT);
        UploadLogBus.log(
                logTime + "  垛码 " + palletCode + " " + boxCount + "箱 开始上传",
                UploadLogBus.Color.GRAY);
        UploadLogBus.firePalletEvent(palletCode, boxCount,
                UploadLogBus.PalletUploadStatus.UPLOADING, null);

        try {
            // 组装 coderelation 列表
            List<Map<String, Object>> codeRelationList = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("biggerserialnumber", "");
                item.put("bigserialnumber",    toStr(row.get("BigSerialNumber")));
                item.put("mediumserialnumber", toStr(row.get("MediumSerialNumber")));
                item.put("smallserialnumber",  toStr(row.get("SmallSerialNumber")));
                item.put("dxcode",             "");
                codeRelationList.add(item);
            }
            String codeRelationJson = MAPPER.writeValueAsString(codeRelationList);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String time  = sdf.format(new Date());
            String nonce = "123";

            String warehouseNo = (cfg.getWarehouseNo() != null && !cfg.getWarehouseNo().trim().isEmpty())
                    ? cfg.getWarehouseNo().trim() : "001";

            Map<String, Object> requestData = new LinkedHashMap<>();
            requestData.put("coderelation",        codeRelationJson);
            requestData.put("issync",              "1");
            requestData.put("synctype",            "1");
            requestData.put("productno",           productNo != null ? productNo : "");
            requestData.put("storageno",           orderNo.trim());
            requestData.put("warehouseno",         warehouseNo);
            requestData.put("sybatchno",           "");
            requestData.put("virtualserialnumber", palletCode);

            String sign = palletSign(time, nonce, appSecret, requestData);

            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("timestamp",    time);
            headers.put("appid",        appId != null ? appId : "");
            headers.put("nonce",        nonce);
            headers.put("sign",         sign);
            headers.put("Content-type", "application/json");

            String jsonStr  = MAPPER.writeValueAsString(requestData);
            String response = sendPost(baseUrl + uploadPath, headers, jsonStr);
            log.info("[虚拟垛标上传] palletCode={} 接口响应: {}", palletCode, response);

            // ③ 日志：上传中（蓝色）
            logTime = LocalDateTime.now().format(LOG_TIME_FMT);
            UploadLogBus.log(
                    logTime + "  垛码 " + palletCode + " " + boxCount + "箱 上传中...",
                    UploadLogBus.Color.BLUE);

            // ④ 5 分钟后轮询上传结果
            pollScheduler.schedule(
                    () -> pollAndUpdatePalletStatus(palletCode, orderNo, boxCount),
                    10, TimeUnit.SECONDS);

        } catch (Exception e) {
            log.error("[虚拟垛标上传] palletCode={} 接口调用异常: {}", palletCode, e.getMessage(), e);
            // 标记为上传失败
            try {
                jdbcTemplate.update(
                "UPDATE CodeRelationUpload SET IsUpload = 2, Msg = ? WHERE VirtualSerialNumber = ? AND IsDel = 0",
                e.getMessage(), palletCode);
            } catch (Exception ex) {
                log.error("[虚拟垛标上传] 更新 IsUpload=2 失败 palletCode={}", palletCode, ex);
            }
            logTime = LocalDateTime.now().format(LOG_TIME_FMT);
            UploadLogBus.log(
                    logTime + "  垛码 " + palletCode + " " + boxCount + "箱 上传失败：" + e.getMessage(),
                    UploadLogBus.Color.RED);
        }
    }

    /**
     * 5 分钟后轮询 GetSyncCodeAndVirtualRelationResult 接口，根据结果更新 IsUpload 并刷新实时上传区。
     * return_code=0 → IsUpload=1（已上传）；否则 → IsUpload=2（上传失败）。
     */
    private void pollAndUpdatePalletStatus(String palletCode, String orderNo, int boxCount) {
        try {
            ShiwanM2SettingsDto cfg = ShiwanM2SettingsFileLoader.load();
            if (cfg == null || cfg.getApi() == null) {
                log.warn("[垛标结果查询] 未配置API，跳过 palletCode={}", palletCode);
                return;
            }
            String baseUrl = trimStr(cfg.getApi().getBaseUrl());
            String path    = trimStr(cfg.getApi().getGetSyncResultPath());
            if (baseUrl == null || path == null) {
                log.warn("[垛标结果查询] 缺少 getSyncResultPath 配置，跳过 palletCode={}", palletCode);
                return;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String time  = sdf.format(new Date());
            String nonce = "123";

            Map<String, Object> requestData = new LinkedHashMap<>();
            requestData.put("virtualserialnumber", palletCode);

            String sign = palletSign(time, nonce, cfg.getApi().getAppSecret(), requestData);

            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("timestamp",    time);
            headers.put("appid",        cfg.getApi().getAppId() != null ? cfg.getApi().getAppId() : "");
            headers.put("nonce",        nonce);
            headers.put("sign",         sign);
            headers.put("Content-type", "application/json");

            String response = sendPost(baseUrl + path, headers, MAPPER.writeValueAsString(requestData));
            log.info("[垛标结果查询] palletCode={} 响应: {}", palletCode, response);

            JsonNode root       = MAPPER.readTree(response);
            String   returnCode = root.has("return_code") ? root.get("return_code").asText() : "-1";
            String   returnMsg  = root.has("return_msg")  ? root.get("return_msg").asText()  : "未知错误";
            String   logTime    = LocalDateTime.now().format(LOG_TIME_FMT);

            if ("处理成功".equals(returnMsg)) {
                jdbcTemplate.update(
                        "UPDATE CodeRelationUpload SET IsUpload = 1, UploadTime = NOW() WHERE VirtualSerialNumber = ? AND IsDel = 0",
                        palletCode);
                log.info("[垛标结果查询] 垛 {} 上传成功", palletCode);
                UploadLogBus.log(
                        logTime + "  垛码 " + palletCode + " " + boxCount + "箱 上传成功",
                        UploadLogBus.Color.GREEN);
                UploadLogBus.firePalletEvent(palletCode, boxCount,
                        UploadLogBus.PalletUploadStatus.SUCCESS, null);
            } else {
                jdbcTemplate.update(
                        "UPDATE CodeRelationUpload SET IsUpload = 2, Msg = ? WHERE VirtualSerialNumber = ? AND IsDel = 0",
                        returnMsg, palletCode);
                log.warn("[垛标结果查询] 垛 {} 上传失败: {}", palletCode, returnMsg);
                UploadLogBus.log(
                        logTime + "  垛码 " + palletCode + " " + boxCount + "箱 上传失败：" + returnMsg,
                        UploadLogBus.Color.RED);
                UploadLogBus.firePalletEvent(palletCode, boxCount,
                        UploadLogBus.PalletUploadStatus.FAILED, returnMsg);
            }
        } catch (Exception e) {
            log.error("[垛标结果查询] palletCode={} 异常: {}", palletCode, e.getMessage(), e);
        }
    }

    // ================================================================
    //  上传管理公共接口（供 ShiwanM2UploadApiController 调用）
    // ================================================================

    /**
     * 按垛码查询上传状态。
     * @return Map 含 isUpload(0=成功,1=待上传,2=失败,3=上传中)、uploadTime、boxCount、msg；垛码不存在返回 null。
     */
    public Map<String, Object> queryPalletStatus(String palletCode) {
        if (palletCode == null || palletCode.trim().isEmpty()) return null;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT MIN(IsUpload) AS isUpload, MAX(UploadTime) AS uploadTime, " +
                "COUNT(DISTINCT BigSerialNumber) AS boxCount, MAX(Msg) AS msg " +
                "FROM CodeRelationUpload WHERE VirtualSerialNumber = ? AND IsDel = 0",
                palletCode.trim());
        if (rows == null || rows.isEmpty() || rows.get(0).get("isUpload") == null) return null;
        Map<String, Object> row = rows.get(0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("palletCode",  palletCode.trim());
        result.put("isUpload",    row.get("isUpload"));
        result.put("uploadTime",  row.get("uploadTime"));
        result.put("boxCount",    row.get("boxCount"));
        result.put("msg",         row.get("msg"));
        return result;
    }

    /**
     * 将指定垛的 IsUpload 置为 0（设为未上传）。
     * @return 更新行数，0 表示垛码不存在。
     */
    public int setPalletNotUploaded(String palletCode) {
        if (palletCode == null || palletCode.trim().isEmpty()) return 0;
        return jdbcTemplate.update(
                "UPDATE CodeRelationUpload SET IsUpload = 0 WHERE VirtualSerialNumber = ? AND IsDel = 0",
                palletCode.trim());
    }

    /**
     * 将指定垛的 IsUpload 置为 1（标记为已上传）。
     * @return 更新行数，0 表示垛码不存在。
     */
    public int setPalletUploaded(String palletCode) {
        if (palletCode == null || palletCode.trim().isEmpty()) return 0;
        return jdbcTemplate.update(
                "UPDATE CodeRelationUpload SET IsUpload = 1 WHERE VirtualSerialNumber = ? AND IsDel = 0",
                palletCode.trim());
    }

    /**
     * 查询所有待上传的垛（IsUpload=0、VirtualSerialNumber 非空）。
     * 返回列表每项含 palletCode / orderNo / productNo / boxCount，按最早入库时间排序。
     */
    public List<Map<String, Object>> getPendingUploadPallets() {
        return jdbcTemplate.queryForList(
                "SELECT VirtualSerialNumber AS palletCode, " +
                "MAX(OrderNo) AS orderNo, MAX(ProductNO) AS productNo, " +
                "COUNT(DISTINCT BigSerialNumber) AS boxCount " +
                "FROM CodeRelationUpload " +
                "WHERE IsUpload = 0 AND VirtualSerialNumber != '' AND IsDel = 0 " +
                "GROUP BY VirtualSerialNumber ORDER BY MIN(AddTime) ASC");
    }

    /**
     * 手动上传：查找所有 IsUpload=0（待上传）的垛，在后台线程中逐垛串行调用上传接口。
     * 方法立即返回，上传过程异步执行并通过实时日志反馈进度。
     *
     * @return 找到的待上传垛数（0 表示无需上传）
     */
    public int startManualUpload() {
        List<Map<String, Object>> pending = getPendingUploadPallets();
        if (pending.isEmpty()) {
            UploadLogBus.log(
                    LocalDateTime.now().format(LOG_TIME_FMT) + "  无待上传的垛数据",
                    UploadLogBus.Color.GRAY);
            return 0;
        }
        UploadLogBus.log(
                LocalDateTime.now().format(LOG_TIME_FMT) +
                "  手动上传任务已启动，共 " + pending.size() + " 个待上传垛...",
                UploadLogBus.Color.GRAY);
        // 在后台定时线程中逐垛串行上传
        pollScheduler.submit(() -> {
            for (Map<String, Object> pallet : pending) {
                String palletCode = toStr(pallet.get("palletCode"));
                String orderNo    = toStr(pallet.get("orderNo"));
                String productNo  = toStr(pallet.get("productNo"));
                int    boxCount   = pallet.get("boxCount") instanceof Number
                        ? ((Number) pallet.get("boxCount")).intValue() : 0;
                syncVirtualPalletToCloud(orderNo, productNo, palletCode, boxCount);
            }
        });
        return pending.size();
    }

    private static String palletSign(String time, String nonce, String secret, Map<String, Object> data) throws Exception {
        TreeMap<String, Object> sorted = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        sorted.putAll(data);
        StringBuilder params = new StringBuilder();
        for (Map.Entry<String, Object> entry : sorted.entrySet()) {
            String key = entry.getKey();
            String value = String.valueOf(entry.getValue());
            if (key != null && !key.isEmpty()) {
                params.append(key).append(value);
            }
        }
        String signStr = time + nonce + (secret != null ? secret : "") + params;
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] bytes = md.digest(signStr.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }

    private static String sendPost(String urlStr, Map<String, String> headers, String data) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        StringBuilder response = new StringBuilder();
        try {
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
            conn.setDoOutput(true);
            conn.setDoInput(true);
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = data.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line.trim());
                }
            }
        } finally {
            conn.disconnect();
        }
        return response.toString();
    }

    private static String trimStr(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String toStr(Object val) {
        if (val == null) return "";
        return val.toString().replace("\uFEFF", "");
    }
}
