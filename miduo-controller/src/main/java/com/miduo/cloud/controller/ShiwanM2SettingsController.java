package com.miduo.cloud.controller;

import com.miduo.cloud.common.config.ShiwanM2SettingsDto;
import com.miduo.cloud.common.config.ShiwanM2SettingsFileLoader;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.infrastructure.mapper.CodePackageImportMapper;
import com.miduo.cloud.infrastructure.persistence.mybatis.po.CodePackageImportPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 石湾 2 号机系统设置相关接口（P03-02）。
 * 提供数据库连接测试等，供前端系统设置弹窗调用。
 */
@RestController
@RequestMapping("/api/shiwan-m2/settings")
@CrossOrigin
public class ShiwanM2SettingsController {

    private static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String JDBC_URL_TEMPLATE = "jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=Asia/Shanghai&connectTimeout=5000&socketTimeout=5000";

    /** 1 号机 SQL Server（jTDS） */
    private static final String JTDS_DRIVER = "net.sourceforge.jtds.jdbc.Driver";
    private static final String JTDS_URL_TEMPLATE = "jdbc:jtds:sqlserver://%s:%s/%s;loginTimeout=5;socketTimeout=5000";
    private static final DateTimeFormatter IMPORT_TIME_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int DB_QUERY_BATCH_SIZE = 2000;
    private static final int DB_INSERT_BATCH_SIZE = 5000;
    private static final int MEMORY_BUFFER_SIZE = 20000;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private CodePackageImportMapper codePackageImportMapper;

    /**
     * 测试数据库连接（2 号机本地 MySQL）。
     * POST /api/shiwan-m2/settings/test-db-connection
     * Body: { "host": "", "port": "3306", "database": "", "username": "", "password": "" }
     */
    @PostMapping("/test-db-connection")
    public ApiResult<Boolean> testDbConnection(@RequestBody Map<String, String> body) {
        String host = body != null ? body.get("host") : null;
        String port = body != null ? body.get("port") : "3306";
        String database = body != null ? body.get("database") : null;
        String username = body != null ? body.get("username") : null;
        String password = body != null ? body.get("password") : null;
        if (host == null || host.isEmpty() || database == null || database.isEmpty() || username == null || username.isEmpty()) {
            return ApiResult.error("缺少必填项：host、database、username");
        }
        if (port == null || port.isEmpty()) port = "3306";
        if (password == null) password = "";
        String url = String.format(JDBC_URL_TEMPLATE, host.trim(), port.trim(), database.trim());
        try {
            Class.forName(MYSQL_DRIVER);
        } catch (ClassNotFoundException e) {
            return ApiResult.error("MySQL 驱动未加载，请确认依赖已引入");
        }
        try (java.sql.Connection conn = DriverManager.getConnection(url, username.trim(), password)) {
            return ApiResult.success("连接成功", true);
        } catch (Exception e) {
            return ApiResult.error("连接失败：" + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
    }

    /**
     * 测试 1 号机数据库连接（SQL Server，用于 T_Code 同步）。
     * POST /api/shiwan-m2/settings/test-m1-db-connection
     * Body: { "host": "", "port": "1433", "database": "", "username": "", "password": "" }
     */
    @PostMapping("/test-m1-db-connection")
    public ApiResult<Boolean> testM1DbConnection(@RequestBody Map<String, String> body) {
        String host = body != null ? body.get("host") : null;
        String port = body != null ? body.get("port") : "1433";
        String database = body != null ? body.get("database") : null;
        String username = body != null ? body.get("username") : null;
        String password = body != null ? body.get("password") : null;
        if (host == null || host.isEmpty() || database == null || database.isEmpty() || username == null || username.isEmpty()) {
            return ApiResult.error("缺少必填项：host、database、username");
        }
        if (port == null || port.isEmpty()) port = "1433";
        if (password == null) password = "";
        String url = String.format(JTDS_URL_TEMPLATE, host.trim(), port.trim(), database.trim());
        try {
            Class.forName(JTDS_DRIVER);
        } catch (ClassNotFoundException e) {
            return ApiResult.error("SQL Server 驱动(jTDS)未加载，请确认已引入 jtds 依赖");
        }
        try (java.sql.Connection conn = DriverManager.getConnection(url, username.trim(), password)) {
            return ApiResult.success("连接成功", true);
        } catch (Exception e) {
            return ApiResult.error("连接失败：" + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
    }

    /**
     * 主界面启动后调用：根据已保存的配置检测 2 号机本机 MySQL 是否可连接。
     * GET /api/shiwan-m2/settings/check-db-connection
     * 返回 200 表示连接成功，其他表示失败，供前端写入操作日志。
     */
    @GetMapping("/check-db-connection")
    public ApiResult<Boolean> checkDbConnection() {
        ShiwanM2SettingsDto config = ShiwanM2SettingsFileLoader.load();
        if (config == null || config.getDbConnection() == null) {
            return ApiResult.error(400, "未配置本机数据库连接，请在系统设置中填写数据库信息");
        }
        ShiwanM2SettingsDto.DbConnection db = config.getDbConnection();
        String host = db.getHost() != null ? db.getHost().trim() : "";
        String port = db.getPort() != null && !db.getPort().isEmpty() ? db.getPort().trim() : "3306";
        String database = db.getDatabase() != null ? db.getDatabase().trim() : "";
        String username = db.getUsername() != null ? db.getUsername().trim() : "";
        String password = db.getPassword() != null ? db.getPassword() : "";
        if (host.isEmpty() || database.isEmpty() || username.isEmpty()) {
            return ApiResult.error(400, "本机数据库配置不完整，请填写 host、database、username");
        }
        String url = String.format(JDBC_URL_TEMPLATE, host, port, database);
        try {
            Class.forName(MYSQL_DRIVER);
        } catch (ClassNotFoundException e) {
            return ApiResult.error("MySQL 驱动未加载，请确认依赖已引入");
        }
        try (java.sql.Connection conn = DriverManager.getConnection(url, username, password)) {
            return ApiResult.success("本机数据库连接正常（" + host + ":" + port + "/" + database + "）", true);
        } catch (Exception e) {
            return ApiResult.error("本机数据库连接失败：" + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
    }

    /**
     * 门禁用：根据已保存的配置检测 1 号机 SQL Server 是否可连接。
     * GET /api/shiwan-m2/settings/check-m1-connection
     * 读取 shiwan-m2-settings.json 中的 m1DbConnection，用与 test-m1-db-connection 相同的 JDBC 逻辑检测。
     */
    @GetMapping("/check-m1-connection")
    public ApiResult<Boolean> checkM1Connection() {
        ShiwanM2SettingsDto config = ShiwanM2SettingsFileLoader.load();
        if (config == null || config.getM1DbConnection() == null) {
            return ApiResult.error(400, "未配置 1 号机连接，请在系统设置中配置");
        }
        ShiwanM2SettingsDto.M1DbConnection m1 = config.getM1DbConnection();
        String host = m1.getHost();
        String port = m1.getPort() != null && !m1.getPort().isEmpty() ? m1.getPort() : "1433";
        String database = m1.getDatabase();
        String username = m1.getUsername();
        String password = m1.getPassword() != null ? m1.getPassword() : "";
        if (host == null || host.isEmpty() || database == null || database.isEmpty() || username == null || username.isEmpty()) {
            return ApiResult.error(400, "1 号机连接配置不完整，请在系统设置中填写 host、database、username");
        }
        String url = String.format(JTDS_URL_TEMPLATE, host.trim(), port.trim(), database.trim());
        try {
            Class.forName(JTDS_DRIVER);
        } catch (ClassNotFoundException e) {
            return ApiResult.error("SQL Server 驱动(jTDS)未加载，请确认已引入 jtds 依赖");
        }
        try (java.sql.Connection conn = DriverManager.getConnection(url, username.trim(), password)) {
            return ApiResult.success("1 号机连接正常", true);
        } catch (Exception e) {
            return ApiResult.error("1 号机连接失败：" + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
    }

    /**
     * 冷表码导入（系统设置-业务）：支持 txt/csv 按行导入（高性能流式）。
     * 入参优先：{ "filePath": "D:/.../xxx.txt" }；兼容旧入参：{ "codes": ["...", "..."] }。
     * 规则：16 位=小标(1)、12 位=中标(2)、8 位=大标(3)；冷表已存在 CodeValue 则跳过。
     */
    @PostMapping("/import-cold-codes")
    public ApiResult<Map<String, Object>> importColdCodes(@RequestBody Map<String, Object> body) {
        String filePath = body != null && body.get("filePath") != null
                ? String.valueOf(body.get("filePath")).trim()
                : null;
        if (filePath != null && !filePath.isEmpty()) {
            return importColdCodesByFilePath(filePath);
        }
        Object rawCodes = body != null ? body.get("codes") : null;
        if (!(rawCodes instanceof List)) {
            return ApiResult.error(400, "入参错误：请提供 filePath 或 codes");
        }
        List<?> arr = (List<?>) rawCodes;
        if (arr.isEmpty()) {
            return ApiResult.error(400, "未读取到可导入的数据");
        }
        List<String> lines = new ArrayList<>(arr.size());
        for (Object item : arr) {
            lines.add(item == null ? "" : String.valueOf(item));
        }
        return doImportColdCodes(lines);
    }

    private ApiResult<Map<String, Object>> importColdCodesByFilePath(String filePath) {
        File f = new File(filePath);
        if (!f.exists() || !f.isFile()) {
            return ApiResult.error(400, "文件不存在或不可读取：" + filePath);
        }
        String lower = f.getName().toLowerCase();
        if (!(lower.endsWith(".txt") || lower.endsWith(".csv"))) {
            return ApiResult.error(400, "仅支持 txt/csv 文件");
        }
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8), 64 * 1024)) {
            List<String> lines = new ArrayList<>(MEMORY_BUFFER_SIZE);
            Map<String, Object> sum = new HashMap<>();
            sum.put("totalRead", 0L);
            sum.put("emptyOrBlankCount", 0L);
            sum.put("dedupInBatchSkip", 0L);
            sum.put("invalidLengthCount", 0L);
            sum.put("smallTotal", 0L);
            sum.put("mediumTotal", 0L);
            sum.put("largeTotal", 0L);
            sum.put("insertedSmall", 0L);
            sum.put("insertedMedium", 0L);
            sum.put("insertedLarge", 0L);
            sum.put("insertedTotal", 0L);
            sum.put("duplicateInColdCount", 0L);
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
                if (lines.size() >= MEMORY_BUFFER_SIZE) {
                    ApiResult<Map<String, Object>> part = doImportColdCodes(lines);
                    if (part.getCode() != 200) {
                        return part;
                    }
                    mergeImportStats(sum, part.getData());
                    lines.clear();
                }
            }
            if (!lines.isEmpty()) {
                ApiResult<Map<String, Object>> part = doImportColdCodes(lines);
                if (part.getCode() != 200) {
                    return part;
                }
                mergeImportStats(sum, part.getData());
            }
            long insertedTotal = toLong(sum.get("insertedTotal"));
            long duplicateInColdCount = toLong(sum.get("duplicateInColdCount"));
            long invalidLengthCount = toLong(sum.get("invalidLengthCount"));
            String msg = "导入完成：新增 " + insertedTotal + " 条，已存在跳过 "
                    + duplicateInColdCount + " 条，位数不符跳过 " + invalidLengthCount + " 条";
            return ApiResult.success(msg, sum);
        } catch (Exception e) {
            return ApiResult.error(500, "导入失败：" + e.getMessage());
        }
    }

    private void mergeImportStats(Map<String, Object> sum, Map<String, Object> part) {
        if (sum == null || part == null) {
            return;
        }
        for (String key : sum.keySet()) {
            sum.put(key, toLong(sum.get(key)) + toLong(part.get(key)));
        }
    }

    private long toLong(Object v) {
        if (v == null) {
            return 0L;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (Exception ignore) {
            return 0L;
        }
    }

    private ApiResult<Map<String, Object>> doImportColdCodes(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return ApiResult.error(400, "文件中没有有效码值");
        }
        long totalRead = 0;
        long emptyOrBlankCount = 0;
        long invalidLengthCount = 0;
        long dedupInBatchSkip = 0;
        long duplicateInColdCount = 0;
        long smallTotal = 0;
        long mediumTotal = 0;
        long largeTotal = 0;

        List<String> smallCodes = new ArrayList<>();
        List<String> mediumCodes = new ArrayList<>();
        List<String> largeCodes = new ArrayList<>();
        for (String raw : lines) {
            totalRead++;
            String code = normalizeImportedCode(raw);
            if (code == null) {
                emptyOrBlankCount++;
                continue;
            }
            int len = code.length();
            if (len == 16) {
                smallCodes.add(code);
                smallTotal++;
            } else if (len == 12) {
                mediumCodes.add(code);
                mediumTotal++;
            } else if (len == 8) {
                largeCodes.add(code);
                largeTotal++;
            } else {
                invalidLengthCount++;
            }
        }

        ImportBatchResult rs = importByPackageType(1, smallCodes);
        ImportBatchResult rm = importByPackageType(2, mediumCodes);
        ImportBatchResult rl = importByPackageType(3, largeCodes);

        dedupInBatchSkip = rs.duplicateInBatch + rm.duplicateInBatch + rl.duplicateInBatch;
        duplicateInColdCount = rs.duplicateInCold + rm.duplicateInCold + rl.duplicateInCold;

        long insertedSmall = rs.inserted;
        long insertedMedium = rm.inserted;
        long insertedLarge = rl.inserted;
        long insertedTotal = insertedSmall + insertedMedium + insertedLarge;

        Map<String, Object> result = new HashMap<>();
        result.put("totalRead", totalRead);
        result.put("emptyOrBlankCount", emptyOrBlankCount);
        result.put("dedupInBatchSkip", dedupInBatchSkip);
        result.put("invalidLengthCount", invalidLengthCount);
        result.put("smallTotal", smallTotal);
        result.put("mediumTotal", mediumTotal);
        result.put("largeTotal", largeTotal);
        result.put("insertedSmall", insertedSmall);
        result.put("insertedMedium", insertedMedium);
        result.put("insertedLarge", insertedLarge);
        result.put("insertedTotal", insertedTotal);
        result.put("duplicateInColdCount", Math.max(0L, duplicateInColdCount));

        String msg = "导入完成：新增 " + insertedTotal + " 条，已存在跳过 "
                + Math.max(0L, duplicateInColdCount) + " 条，位数不符跳过 " + invalidLengthCount + " 条";
        return ApiResult.success(msg, result);
    }

    private String normalizeImportedCode(String rawLine) {
        if (rawLine == null) {
            return null;
        }
        String v = rawLine.replace("\uFEFF", "").trim();
        if (v.isEmpty()) {
            return null;
        }
        int commaIdx = v.indexOf(',');
        if (commaIdx >= 0) {
            v = v.substring(0, commaIdx).trim();
        }
        if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) {
            v = v.substring(1, v.length() - 1).trim();
        }
        return v.isEmpty() ? null : v;
    }

    private ImportBatchResult importByPackageType(int packageType, List<String> codes) {
        ImportBatchResult result = new ImportBatchResult();
        if (codes == null || codes.isEmpty()) {
            return result;
        }
        LinkedHashSet<String> dedupSet = new LinkedHashSet<>(codes);
        result.duplicateInBatch = codes.size() - dedupSet.size();
        List<String> dedupCodes = new ArrayList<>(dedupSet);
        Set<String> exists = queryExistingColdCodes(packageType, dedupCodes);
        List<String> toInsert = new ArrayList<>();
        for (String code : dedupCodes) {
            if (!exists.contains(code)) {
                toInsert.add(code);
            }
        }
        result.duplicateInCold = dedupCodes.size() - toInsert.size();
        if (toInsert.isEmpty()) {
            return result;
        }
        Long importId = resolveOrCreateImportId(packageType);
        for (int start = 0; start < toInsert.size(); start += DB_INSERT_BATCH_SIZE) {
            int end = Math.min(start + DB_INSERT_BATCH_SIZE, toInsert.size());
            List<String> batchCodes = toInsert.subList(start, end);
            List<Object[]> params = new ArrayList<>(batchCodes.size());
            for (String code : batchCodes) {
                params.add(new Object[]{importId, packageType, code});
            }
            int[] rs = jdbcTemplate.batchUpdate(
                    "INSERT INTO CodePackageItemCold (ImportId, PackageType, CodeValue, AssociatedAt, CreateTime, UpdateTime) " +
                            "VALUES (?, ?, ?, NOW(), NOW(), NOW())",
                    params
            );
            for (int n : rs) {
                if (n > 0 || n == Statement.SUCCESS_NO_INFO) {
                    result.inserted += 1;
                }
            }
        }
        return result;
    }

    private Set<String> queryExistingColdCodes(int packageType, List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Collections.emptySet();
        }
        LinkedHashSet<String> exists = new LinkedHashSet<>();
        for (int start = 0; start < codes.size(); start += DB_QUERY_BATCH_SIZE) {
            int end = Math.min(start + DB_QUERY_BATCH_SIZE, codes.size());
            List<String> batch = codes.subList(start, end);
            String placeholders = String.join(",", Collections.nCopies(batch.size(), "?"));
            String sql = "SELECT CodeValue FROM CodePackageItemCold WHERE PackageType = ? AND CodeValue IN (" + placeholders + ")";
            List<Object> args = new ArrayList<>(batch.size() + 1);
            args.add(packageType);
            args.addAll(batch);
            List<String> rows = jdbcTemplate.queryForList(sql, String.class, args.toArray());
            exists.addAll(rows);
        }
        return exists;
    }

    private static class ImportBatchResult {
        long inserted;
        long duplicateInBatch;
        long duplicateInCold;
    }

    private Long resolveOrCreateImportId(int packageType) {
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT Id FROM CodePackageImport WHERE PackageType = ? AND Status = 1 ORDER BY Id DESC LIMIT 1",
                Long.class, packageType
        );
        if (ids != null && !ids.isEmpty() && ids.get(0) != null) {
            return ids.get(0);
        }
        LocalDateTime now = LocalDateTime.now();
        CodePackageImportPO po = new CodePackageImportPO();
        po.setPackageType(packageType);
        po.setPackageName("系统设置冷表导入-" + packageType + "-" + now.format(IMPORT_TIME_FMT));
        po.setImportTime(now);
        po.setImportSource(2);
        po.setCodeCount(0);
        po.setStatus(1);
        po.setRemark("系统设置页面导入冷表码");
        po.setFileName("manual-cold-import-" + packageType + ".txt");
        po.setCreateTime(now);
        po.setUpdateTime(now);
        codePackageImportMapper.insert(po);
        return po.getId();
    }
}
