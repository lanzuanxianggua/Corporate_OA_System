package cn.oa.document.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.result.R;
import cn.oa.document.service.DocSerialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 文号管理控制器
 *
 * @author oa-document
 */
@Slf4j
@RestController
@RequestMapping("/api/document/serial")
@Tag(name = "文号管理")
@RequiredArgsConstructor
public class DocSerialController {

    private final DocSerialService docSerialService;

    @PostMapping("/lock")
    @Operation(summary = "锁定文号")
    @OperationLog(module = "文号管理", operation = "锁定文号")
    public R<String> lockSerial(
            @RequestParam @NotBlank String orgCode,
            @RequestParam @NotNull Integer year,
            @RequestParam @NotNull Long lockBy) {
        String serialNo = docSerialService.lockSerial(orgCode, year, lockBy);
        return R.ok(serialNo);
    }

    @PostMapping("/{id}/release")
    @Operation(summary = "释放文号")
    @OperationLog(module = "文号管理", operation = "释放文号")
    public R<Void> releaseSerial(@PathVariable Long id) {
        docSerialService.releaseSerial(id);
        return R.ok();
    }

    @PostMapping("/{id}/use")
    @Operation(summary = "使用文号")
    @OperationLog(module = "文号管理", operation = "使用文号")
    public R<Void> useSerial(@PathVariable Long id, @RequestParam Long dispatchId) {
        docSerialService.useSerial(id, dispatchId);
        return R.ok();
    }
}
