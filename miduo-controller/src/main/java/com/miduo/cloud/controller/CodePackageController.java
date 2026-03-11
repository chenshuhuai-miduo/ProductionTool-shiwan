package com.miduo.cloud.controller;

import com.miduo.cloud.application.codepackage.CodePackageApplicationService;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.common.dto.PageOutput;
import com.miduo.cloud.entity.dto.codepackage.CodePackageImportVO;
import com.miduo.cloud.entity.dto.codepackage.CodePackageLocalImportRequest;
import com.miduo.cloud.entity.dto.codepackage.CodePackageOnlineImportResultVO;
import com.miduo.cloud.entity.dto.codepackage.CodePackagePageQueryDTO;
import com.miduo.cloud.entity.dto.codepackage.CodePackageViewCodeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 码包管理 Controller
 */
@RestController
@RequestMapping("/api/code-package")
@CrossOrigin(originPatterns = "*", allowCredentials = "false")
public class CodePackageController {

    @Autowired
    private CodePackageApplicationService codePackageApplicationService;

    @PostMapping("/page")
    public ApiResult<PageOutput<CodePackageImportVO>> page(@RequestBody CodePackagePageQueryDTO queryDTO) {
        return codePackageApplicationService.pageQuery(queryDTO);
    }

    @PostMapping("/import/online")
    public ApiResult<CodePackageOnlineImportResultVO> importOnline() {
        return codePackageApplicationService.importOnline();
    }

    @PostMapping("/import/local")
    public ApiResult<CodePackageImportVO> importLocal(@RequestBody CodePackageLocalImportRequest request) {
        return codePackageApplicationService.importLocal(request);
    }

    @GetMapping("/{id}/codes")
    public ApiResult<PageOutput<CodePackageViewCodeVO>> queryCodes(@PathVariable("id") Long id,
                                                                    @RequestParam(value = "keyword", required = false) String keyword,
                                                                    @RequestParam(value = "pageNum", required = false) Long pageNum,
                                                                    @RequestParam(value = "pageSize", required = false) Long pageSize) {
        return codePackageApplicationService.queryCodes(id, keyword, pageNum, pageSize);
    }

    @DeleteMapping("/{id}")
    public ApiResult<String> delete(@PathVariable("id") Long id) {
        return codePackageApplicationService.deletePackage(id);
    }
}
