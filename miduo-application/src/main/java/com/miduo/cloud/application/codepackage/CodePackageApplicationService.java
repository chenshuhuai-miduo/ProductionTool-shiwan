package com.miduo.cloud.application.codepackage;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.common.dto.PageOutput;
import com.miduo.cloud.entity.dto.codepackage.CodePackageImportVO;
import com.miduo.cloud.entity.dto.codepackage.CodePackageLocalImportRequest;
import com.miduo.cloud.entity.dto.codepackage.CodePackageOnlineImportResultVO;
import com.miduo.cloud.entity.dto.codepackage.CodePackagePageQueryDTO;
import com.miduo.cloud.entity.dto.codepackage.CodePackageViewCodeVO;
import com.miduo.cloud.entity.enums.CodePackageImportSourceEnum;
import com.miduo.cloud.entity.enums.CodePackageStatusEnum;
import com.miduo.cloud.entity.enums.CodePackageTypeEnum;
import com.miduo.cloud.infrastructure.mapper.CodePackageCodeQueryMapper;
import com.miduo.cloud.infrastructure.mapper.CodePackageImportMapper;
import com.miduo.cloud.infrastructure.mapper.CodePackageItemColdMapper;
import com.miduo.cloud.infrastructure.mapper.CodePackageItemHotMapper;
import com.miduo.cloud.infrastructure.mapper.CodePackageRelationMapper;
import com.miduo.cloud.infrastructure.openplatform.CodePackageOpenPlatformClient;
import com.miduo.cloud.infrastructure.persistence.mybatis.po.CodeAssociationTimePO;
import com.miduo.cloud.infrastructure.persistence.mybatis.po.CodePackageImportPO;
import com.miduo.cloud.infrastructure.persistence.mybatis.po.CodePackageItemColdPO;
import com.miduo.cloud.infrastructure.persistence.mybatis.po.CodePackageItemHotPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 码包应用服务
 */
@Service
public class CodePackageApplicationService {

    private static final int CODE_QUERY_BATCH_SIZE = 800;
    private static final int INSERT_BATCH_SIZE = 1000;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private CodePackageImportMapper codePackageImportMapper;
    @Autowired
    private CodePackageItemHotMapper codePackageItemHotMapper;
    @Autowired
    private CodePackageItemColdMapper codePackageItemColdMapper;
    @Autowired
    private CodePackageCodeQueryMapper codePackageCodeQueryMapper;
    @Autowired
    private CodePackageRelationMapper codePackageRelationMapper;
    @Autowired
    private CodePackageOpenPlatformClient openPlatformClient;

    @Value("${code.package.import.password:123456}")
    private String importPassword;

    @Value("${code.package.import.overlap-threshold:0.8}")
    private double overlapThreshold;

    @Value("${code.package.default-pull-start-time:2024-01-01 00:00:00}")
    private String defaultPullStartTime;

    public ApiResult<PageOutput<CodePackageImportVO>> pageQuery(CodePackagePageQueryDTO queryDTO) {
        try {
            LambdaQueryWrapper<CodePackageImportPO> queryWrapper = new LambdaQueryWrapper<>();
            if (StringUtils.hasText(queryDTO.getKeyword())) {
                String keyword = queryDTO.getKeyword().trim();
                queryWrapper.and(wrapper -> wrapper.like(CodePackageImportPO::getPackageName, keyword)
                        .or()
                        .like(CodePackageImportPO::getFileName, keyword));
            }
            if (queryDTO.getStartTime() != null) {
                queryWrapper.ge(CodePackageImportPO::getImportTime, queryDTO.getStartTime());
            }
            if (queryDTO.getEndTime() != null) {
                queryWrapper.le(CodePackageImportPO::getImportTime, queryDTO.getEndTime());
            }
            if (queryDTO.getImportSource() != null) {
                queryWrapper.eq(CodePackageImportPO::getImportSource, queryDTO.getImportSource());
            }
            if (queryDTO.getPackageType() != null) {
                queryWrapper.eq(CodePackageImportPO::getPackageType, queryDTO.getPackageType());
            }
            if (queryDTO.getStatus() != null) {
                queryWrapper.eq(CodePackageImportPO::getStatus, queryDTO.getStatus());
            }
            queryWrapper.orderByDesc(CodePackageImportPO::getImportTime);

            Page<CodePackageImportPO> page = new Page<>(queryDTO.resolveCurrent(), queryDTO.resolveSize());
            IPage<CodePackageImportPO> pageResult = codePackageImportMapper.selectPage(page, queryWrapper);

            List<CodePackageImportVO> records = pageResult.getRecords()
                    .stream()
                    .map(this::convertToImportVO)
                    .collect(Collectors.toList());

            PageOutput<CodePackageImportVO> pageOutput = new PageOutput<>();
            pageOutput.setCurrent(pageResult.getCurrent());
            pageOutput.setSize(pageResult.getSize());
            pageOutput.setTotal(pageResult.getTotal());
            pageOutput.setPages(pageResult.getPages());
            pageOutput.setRecords(records);
            return ApiResult.success("查询成功", pageOutput);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("查询码包列表失败：" + e.getMessage());
        }
    }

    public ApiResult<CodePackageOnlineImportResultVO> importOnline() {
        try {
            LocalDateTime defaultTime = parseTimeOrDefault(defaultPullStartTime, LocalDateTime.of(2024, 1, 1, 0, 0, 0));
            LocalDateTime smallTypeStart = getLastOnlineImportCreateTime(1, defaultTime);
            LocalDateTime bigTypeStart = getLastOnlineImportCreateTime(3, defaultTime);
            LocalDateTime queryStart = smallTypeStart.isBefore(bigTypeStart) ? smallTypeStart : bigTypeStart;
            LocalDateTime queryEnd = LocalDateTime.now().plusHours(6).toLocalDate().atTime(23, 59, 59);

            CodePackageOpenPlatformClient.QueryCompletedResponse response = openPlatformClient.queryCompleted(queryStart, queryEnd);
            CodePackageOnlineImportResultVO resultVO = new CodePackageOnlineImportResultVO();
            for (CodePackageOpenPlatformClient.QueryCompletedItem item : response.getItems()) {
                if (item == null || item.getRelationshipType() == null) {
                    continue;
                }
                if (item.getRelationshipType() != 1 && item.getRelationshipType() != 3) {
                    continue;
                }
                resultVO.setTotalProcessed(resultVO.getTotalProcessed() + 1);
                if (!StringUtils.hasText(item.getFileDownloadAddress())) {
                    addOnlineFailed(resultVO, item.getRelationshipType(), item.getFileName(), "无可用下载地址");
                    continue;
                }
                try {
                    List<String> codeLines = openPlatformClient.downloadCodeLines(item.getFileDownloadAddress());
                    PersistResult persistResult = persistCodes(
                            item.getRelationshipType(),
                            item.getFileName(),
                            item.getFileName(),
                            CodePackageImportSourceEnum.ONLINE.getCode(),
                            codeLines,
                            true
                    );
                    addOnlineSuccess(resultVO, item.getRelationshipType(), item.getFileName(), persistResult.getImportedCount());
                } catch (Exception ex) {
                    addOnlineFailed(resultVO, item.getRelationshipType(), item.getFileName(), ex.getMessage());
                }
            }
            return ApiResult.success("在线更新执行完成", resultVO);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("在线更新失败：" + e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResult<CodePackageImportVO> importLocal(CodePackageLocalImportRequest request) {
        try {
            if (request == null) {
                return ApiResult.error(400, "请求不能为空");
            }
            if (!StringUtils.hasText(request.getPassword()) || !importPassword.equals(request.getPassword().trim())) {
                return ApiResult.error(401, "导入密码错误");
            }
            PersistResult persistResult = persistCodes(
                    request.getPackageType(),
                    request.getPackageName(),
                    request.getFileName(),
                    CodePackageImportSourceEnum.LOCAL.getCode(),
                    request.getCodes(),
                    true
            );
            CodePackageImportPO importPO = codePackageImportMapper.selectById(persistResult.getImportId());
            return ApiResult.success("本地导入成功", convertToImportVO(importPO));
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("本地导入失败：" + e.getMessage());
        }
    }

    public ApiResult<PageOutput<CodePackageViewCodeVO>> queryCodes(Long importId,
                                                                    String keyword,
                                                                    Long pageNum,
                                                                    Long pageSize) {
        try {
            CodePackageImportPO importPO = codePackageImportMapper.selectById(importId);
            if (importPO == null) {
                return ApiResult.error(404, "码包不存在");
            }

            LambdaQueryWrapper<CodePackageItemHotPO> hotWrapper = new LambdaQueryWrapper<>();
            hotWrapper.eq(CodePackageItemHotPO::getImportId, importId);
            if (StringUtils.hasText(keyword)) {
                hotWrapper.like(CodePackageItemHotPO::getCodeValue, keyword.trim());
            }
            List<CodePackageItemHotPO> hotCodes = codePackageItemHotMapper.selectList(hotWrapper);

            LambdaQueryWrapper<CodePackageItemColdPO> coldWrapper = new LambdaQueryWrapper<>();
            coldWrapper.eq(CodePackageItemColdPO::getImportId, importId);
            if (StringUtils.hasText(keyword)) {
                coldWrapper.like(CodePackageItemColdPO::getCodeValue, keyword.trim());
            }
            List<CodePackageItemColdPO> coldCodes = codePackageItemColdMapper.selectList(coldWrapper);

            List<CodePackageViewCodeVO> allCodes = new ArrayList<>();
            for (CodePackageItemHotPO hotCode : hotCodes) {
                CodePackageViewCodeVO vo = new CodePackageViewCodeVO();
                vo.setCodeValue(hotCode.getCodeValue());
                vo.setAssociated(false);
                allCodes.add(vo);
            }
            for (CodePackageItemColdPO coldCode : coldCodes) {
                CodePackageViewCodeVO vo = new CodePackageViewCodeVO();
                vo.setCodeValue(coldCode.getCodeValue());
                vo.setAssociated(true);
                vo.setAssociatedAt(coldCode.getAssociatedAt());
                allCodes.add(vo);
            }
            allCodes.sort(Comparator.comparing(CodePackageViewCodeVO::getCodeValue));

            long current = pageNum == null || pageNum < 1 ? 1L : pageNum;
            long size = pageSize == null || pageSize < 1 ? 20L : pageSize;
            int from = (int) ((current - 1) * size);
            int to = Math.min(allCodes.size(), from + (int) size);
            List<CodePackageViewCodeVO> pageRecords;
            if (from >= allCodes.size()) {
                pageRecords = new ArrayList<>();
            } else {
                pageRecords = new ArrayList<>(allCodes.subList(from, to));
            }

            refreshAssociationInfo(importPO.getPackageType(), pageRecords);

            PageOutput<CodePackageViewCodeVO> pageOutput = new PageOutput<>();
            pageOutput.setCurrent(current);
            pageOutput.setSize(size);
            pageOutput.setTotal((long) allCodes.size());
            pageOutput.setPages(Math.max(1L, (allCodes.size() + size - 1) / size));
            pageOutput.setRecords(pageRecords);
            return ApiResult.success("查询成功", pageOutput);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("查看码列表失败：" + e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResult<String> deletePackage(Long importId) {
        try {
            CodePackageImportPO importPO = codePackageImportMapper.selectById(importId);
            if (importPO == null) {
                return ApiResult.error(404, "码包不存在");
            }
            Long coldCount = codePackageItemColdMapper.countByImportId(importId);
            if (coldCount != null && coldCount > 0) {
                return ApiResult.error(400, "存在已关联码，禁止删除");
            }
            List<String> hotCodeValues = codePackageItemHotMapper.selectCodeValuesByImportId(importId);
            if (hasAssociatedCodes(importPO.getPackageType(), hotCodeValues)) {
                return ApiResult.error(400, "存在已关联码，禁止删除");
            }

            codePackageItemHotMapper.deleteByImportId(importId);
            codePackageImportMapper.deleteById(importId);
            return ApiResult.success("删除成功", "OK");
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 门禁检查：小标(1)、中标(2)、大标(3) 是否均已导入（至少有一条 Status=1 的导入记录）。
     * 用于 2 号机开始采集前码包门禁。
     */
    public ApiResult<Map<String, Object>> checkCodePackageGate() {
        try {
            LambdaQueryWrapper<CodePackageImportPO> q1 = new LambdaQueryWrapper<>();
            q1.eq(CodePackageImportPO::getPackageType, 1).eq(CodePackageImportPO::getStatus, 1);
            long c1 = codePackageImportMapper.selectCount(q1);
            LambdaQueryWrapper<CodePackageImportPO> q2 = new LambdaQueryWrapper<>();
            q2.eq(CodePackageImportPO::getPackageType, 2).eq(CodePackageImportPO::getStatus, 1);
            long c2 = codePackageImportMapper.selectCount(q2);
            LambdaQueryWrapper<CodePackageImportPO> q3 = new LambdaQueryWrapper<>();
            q3.eq(CodePackageImportPO::getPackageType, 3).eq(CodePackageImportPO::getStatus, 1);
            long c3 = codePackageImportMapper.selectCount(q3);
            Map<String, Object> data = new HashMap<>();
            data.put("smallImported", c1 > 0);
            data.put("mediumImported", c2 > 0);
            data.put("largeImported", c3 > 0);
            data.put("passed", c1 > 0 && c2 > 0 && c3 > 0);
            if (c1 > 0 && c2 > 0 && c3 > 0) {
                return ApiResult.success("小标、中标、大标均已导入", data);
            }
            List<String> missing = new ArrayList<>();
            if (c1 == 0) missing.add("小标");
            if (c2 == 0) missing.add("中标");
            if (c3 == 0) missing.add("大标");
            ApiResult<Map<String, Object>> err = ApiResult.error(400, "请先导入对应码包：" + String.join("、", missing));
            err.setData(data);
            return err;
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("码包门禁检查失败：" + e.getMessage());
        }
    }

    private void refreshAssociationInfo(Integer packageType, List<CodePackageViewCodeVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<String> codeValues = records.stream()
                .map(CodePackageViewCodeVO::getCodeValue)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        Map<String, LocalDateTime> associationMap = queryAssociationTimeMap(packageType, codeValues);
        for (CodePackageViewCodeVO record : records) {
            LocalDateTime relationTime = associationMap.get(record.getCodeValue());
            if (relationTime != null) {
                record.setAssociated(true);
                record.setAssociatedAt(relationTime);
            } else if (record.getAssociatedAt() == null) {
                record.setAssociated(false);
            }
        }
    }

    private LocalDateTime getLastOnlineImportCreateTime(Integer packageType, LocalDateTime defaultTime) {
        LocalDateTime latestCreateTime = codePackageImportMapper.selectLatestOnlineCreateTimeByType(packageType);
        return latestCreateTime == null ? defaultTime : latestCreateTime;
    }

    private PersistResult persistCodes(Integer packageType,
                                       String packageName,
                                       String fileName,
                                       Integer importSource,
                                       List<String> rawCodes,
                                       boolean checkFileNameDuplicate) {
        validatePackageType(packageType);

        List<String> deduplicatedCodes = normalizeCodes(rawCodes);
        if (deduplicatedCodes.isEmpty()) {
            throw new IllegalArgumentException("导入文件中未解析到有效码");
        }
        if (checkFileNameDuplicate && StringUtils.hasText(fileName)) {
            Long duplicateFile = codePackageImportMapper.countActiveByTypeAndFileName(packageType, fileName.trim());
            if (duplicateFile != null && duplicateFile > 0) {
                throw new IllegalArgumentException("同类型下已存在同名文件，疑似重复导入");
            }
        }

        Set<String> existsSet = queryExistingCodes(packageType, deduplicatedCodes);
        double overlapRate = deduplicatedCodes.isEmpty() ? 0D : (double) existsSet.size() / deduplicatedCodes.size();
        if (overlapRate >= overlapThreshold) {
            throw new IllegalArgumentException("与现有码包重叠率过高（" + (int) (overlapRate * 100) + "%），已拒绝导入");
        }

        List<String> newCodes = deduplicatedCodes.stream()
                .filter(code -> !existsSet.contains(code))
                .collect(Collectors.toList());
        if (newCodes.isEmpty()) {
            throw new IllegalArgumentException("码包内容全部重复，未导入");
        }

        LocalDateTime now = LocalDateTime.now();
        CodePackageImportPO importPO = new CodePackageImportPO();
        importPO.setPackageType(packageType);
        importPO.setPackageName(StringUtils.hasText(packageName) ? packageName.trim() : fileName);
        importPO.setImportTime(now);
        importPO.setImportSource(importSource);
        importPO.setCodeCount(newCodes.size());
        importPO.setStatus(CodePackageStatusEnum.NORMAL.getCode());
        importPO.setFileName(fileName);
        if (!existsSet.isEmpty()) {
            importPO.setRemark("检测到重复码 " + existsSet.size() + " 条，已自动过滤");
        }
        importPO.setCreateTime(now);
        importPO.setUpdateTime(now);
        codePackageImportMapper.insert(importPO);

        batchInsertHotCodes(importPO.getId(), packageType, newCodes, now);

        PersistResult result = new PersistResult();
        result.setImportId(importPO.getId());
        result.setImportedCount(newCodes.size());
        result.setDuplicateCount(existsSet.size());
        return result;
    }

    private void batchInsertHotCodes(Long importId, Integer packageType, List<String> codeValues, LocalDateTime now) {
        if (codeValues == null || codeValues.isEmpty()) {
            return;
        }
        int start = 0;
        while (start < codeValues.size()) {
            int end = Math.min(start + INSERT_BATCH_SIZE, codeValues.size());
            List<CodePackageItemHotPO> batch = new ArrayList<>(end - start);
            for (int i = start; i < end; i++) {
                CodePackageItemHotPO po = new CodePackageItemHotPO();
                po.setImportId(importId);
                po.setPackageType(packageType);
                po.setCodeValue(codeValues.get(i));
                po.setCreateTime(now);
                batch.add(po);
            }
            codePackageItemHotMapper.insertBatch(batch);
            start = end;
        }
    }

    private Set<String> queryExistingCodes(Integer packageType, List<String> deduplicatedCodes) {
        if (deduplicatedCodes == null || deduplicatedCodes.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> exists = new LinkedHashSet<>();
        int start = 0;
        while (start < deduplicatedCodes.size()) {
            int end = Math.min(start + CODE_QUERY_BATCH_SIZE, deduplicatedCodes.size());
            List<String> batch = deduplicatedCodes.subList(start, end);
            List<String> existedInBatch = codePackageCodeQueryMapper.selectExistingCodeValues(packageType, batch);
            if (existedInBatch != null) {
                exists.addAll(existedInBatch);
            }
            start = end;
        }
        return exists;
    }

    private boolean hasAssociatedCodes(Integer packageType, List<String> codeValues) {
        if (codeValues == null || codeValues.isEmpty()) {
            return false;
        }
        int start = 0;
        while (start < codeValues.size()) {
            int end = Math.min(start + CODE_QUERY_BATCH_SIZE, codeValues.size());
            List<String> batch = codeValues.subList(start, end);
            List<CodeAssociationTimePO> associationTimes = codePackageRelationMapper.selectAssociationTimeByCodes(packageType, batch);
            if (associationTimes != null && !associationTimes.isEmpty()) {
                return true;
            }
            start = end;
        }
        return false;
    }

    private Map<String, LocalDateTime> queryAssociationTimeMap(Integer packageType, List<String> codeValues) {
        if (codeValues == null || codeValues.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, LocalDateTime> map = new HashMap<>();
        int start = 0;
        while (start < codeValues.size()) {
            int end = Math.min(start + CODE_QUERY_BATCH_SIZE, codeValues.size());
            List<String> batch = codeValues.subList(start, end);
            List<CodeAssociationTimePO> associationTimes = codePackageRelationMapper.selectAssociationTimeByCodes(packageType, batch);
            if (associationTimes != null) {
                for (CodeAssociationTimePO associationTime : associationTimes) {
                    if (associationTime == null || !StringUtils.hasText(associationTime.getCodeValue())) {
                        continue;
                    }
                    LocalDateTime exists = map.get(associationTime.getCodeValue());
                    if (exists == null || (associationTime.getAssociatedAt() != null && associationTime.getAssociatedAt().isBefore(exists))) {
                        map.put(associationTime.getCodeValue(), associationTime.getAssociatedAt());
                    }
                }
            }
            start = end;
        }
        return map;
    }

    private List<String> normalizeCodes(List<String> rawCodes) {
        if (rawCodes == null || rawCodes.isEmpty()) {
            return new ArrayList<>();
        }
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String rawCode : rawCodes) {
            if (!StringUtils.hasText(rawCode)) {
                continue;
            }
            String code = rawCode.trim();
            if (StringUtils.hasText(code)) {
                set.add(code);
            }
        }
        return new ArrayList<>(set);
    }

    private void validatePackageType(Integer packageType) {
        if (CodePackageTypeEnum.fromCode(packageType) == null) {
            throw new IllegalArgumentException("不支持的码包类型: " + packageType);
        }
    }

    private CodePackageImportVO convertToImportVO(CodePackageImportPO po) {
        if (po == null) {
            return null;
        }
        CodePackageImportVO vo = new CodePackageImportVO();
        vo.setId(po.getId());
        vo.setPackageType(po.getPackageType());
        CodePackageTypeEnum typeEnum = CodePackageTypeEnum.fromCode(po.getPackageType());
        vo.setPackageTypeName(typeEnum == null ? "未知" : typeEnum.getDesc());
        vo.setPackageName(po.getPackageName());
        vo.setImportTime(po.getImportTime());
        vo.setImportSource(po.getImportSource());
        CodePackageImportSourceEnum sourceEnum = CodePackageImportSourceEnum.fromCode(po.getImportSource());
        vo.setImportSourceName(sourceEnum == null ? "未知" : sourceEnum.getDesc());
        vo.setCodeCount(po.getCodeCount());
        vo.setStatus(po.getStatus());
        CodePackageStatusEnum statusEnum = CodePackageStatusEnum.fromCode(po.getStatus());
        vo.setStatusName(statusEnum == null ? "未知" : statusEnum.getDesc());
        vo.setRemark(po.getRemark());
        vo.setFileName(po.getFileName());
        return vo;
    }

    private void addOnlineSuccess(CodePackageOnlineImportResultVO resultVO,
                                  Integer packageType,
                                  String fileName,
                                  Integer importedCount) {
        resultVO.setSuccessCount(resultVO.getSuccessCount() + 1);
        CodePackageOnlineImportResultVO.ItemResult itemResult = new CodePackageOnlineImportResultVO.ItemResult();
        itemResult.setPackageType(packageType);
        itemResult.setPackageTypeName(resolveTypeName(packageType));
        itemResult.setFileName(fileName);
        itemResult.setImportedCount(importedCount);
        itemResult.setMessage("导入成功");
        resultVO.getSuccessItems().add(itemResult);
    }

    private void addOnlineFailed(CodePackageOnlineImportResultVO resultVO,
                                 Integer packageType,
                                 String fileName,
                                 String message) {
        resultVO.setFailedCount(resultVO.getFailedCount() + 1);
        CodePackageOnlineImportResultVO.ItemResult itemResult = new CodePackageOnlineImportResultVO.ItemResult();
        itemResult.setPackageType(packageType);
        itemResult.setPackageTypeName(resolveTypeName(packageType));
        itemResult.setFileName(fileName);
        itemResult.setImportedCount(0);
        itemResult.setMessage(message);
        resultVO.getFailedItems().add(itemResult);
    }

    private LocalDateTime parseTime(String timeText) {
        if (!StringUtils.hasText(timeText)) {
            return null;
        }
        try {
            return LocalDateTime.parse(timeText.trim(), TIME_FMT);
        } catch (Exception ignored) {
            return null;
        }
    }

    private LocalDateTime parseTimeOrDefault(String timeText, LocalDateTime defaultTime) {
        LocalDateTime parsed = parseTime(timeText);
        return parsed == null ? defaultTime : parsed;
    }

    private String resolveTypeName(Integer packageType) {
        CodePackageTypeEnum typeEnum = CodePackageTypeEnum.fromCode(packageType);
        return typeEnum == null ? "未知" : typeEnum.getDesc();
    }

    private static class PersistResult {
        private Long importId;
        private Integer importedCount;
        private Integer duplicateCount;

        public Long getImportId() {
            return importId;
        }

        public void setImportId(Long importId) {
            this.importId = importId;
        }

        public Integer getImportedCount() {
            return importedCount;
        }

        public void setImportedCount(Integer importedCount) {
            this.importedCount = importedCount;
        }

        public Integer getDuplicateCount() {
            return duplicateCount;
        }

        public void setDuplicateCount(Integer duplicateCount) {
            this.duplicateCount = duplicateCount;
        }
    }
}
