package com.miduo.cloud.controller;

import com.miduo.cloud.application.codepackage.CodePackageApplicationService;
import com.miduo.cloud.common.dto.ApiResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 石湾 2 号机码包门禁接口（开始采集前检查小标/中标/大标是否均已导入）。
 */
@RestController
@RequestMapping("/api/shiwan-m2/code-package")
@CrossOrigin
public class ShiwanM2CodePackageController {

    @Autowired
    private CodePackageApplicationService codePackageApplicationService;

    /**
     * 门禁检查：小标(1)、中标(2)、大标(3) 是否均已导入。
     * GET /api/shiwan-m2/code-package/check
     * 返回 data: { smallImported, mediumImported, largeImported, passed }
     */
    @GetMapping("/check")
    public ApiResult<Map<String, Object>> check() {
        return codePackageApplicationService.checkCodePackageGate();
    }

    /**
     * 手工采集门禁检查：热表中小标至少6条、中标至少1条。
     * GET /api/shiwan-m2/code-package/check-manual
     * 返回 data: { smallCount, mediumCount, passed }
     */
    @GetMapping("/check-manual")
    public ApiResult<Map<String, Object>> checkManual() {
        return codePackageApplicationService.checkManualCapturePackage();
    }
}
