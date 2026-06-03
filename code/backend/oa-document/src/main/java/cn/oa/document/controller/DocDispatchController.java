package cn.oa.document.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.WebUtil;
import cn.oa.document.dto.DocDispatchCreateDTO;
import cn.oa.document.dto.DocDispatchQueryDTO;
import cn.oa.document.dto.DocDispatchUpdateDTO;
import cn.oa.document.service.DocDispatchService;
import cn.oa.document.vo.DocDispatchVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 发文管理控制器
 *
 * @author oa-document
 */
@Slf4j
@RestController
@RequestMapping("/api/document/dispatch")
@Tag(name = "发文管理")
@RequiredArgsConstructor
public class DocDispatchController {

    private final DocDispatchService docDispatchService;

    @PostMapping
    @Operation(summary = "创建发文")
    @OperationLog(module = "发文管理", operation = "创建发文")
    public R<Long> create(@RequestBody @Valid DocDispatchCreateDTO dto, HttpServletRequest request) {
        String creator = WebUtil.getEmpName(request);
        Long id = docDispatchService.createDispatch(dto, creator);
        log.info("发文创建成功: id={}", id);
        return R.ok(id);
    }

    @PutMapping
    @Operation(summary = "更新发文")
    @OperationLog(module = "发文管理", operation = "更新发文")
    public R<Void> update(@RequestBody @Valid DocDispatchUpdateDTO dto) {
        docDispatchService.updateDispatch(dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @RequireAdmin
    @Operation(summary = "删除发文")
    @OperationLog(module = "发文管理", operation = "删除发文")
    public R<Void> delete(@PathVariable Long id) {
        docDispatchService.deleteDispatch(id);
        return R.ok();
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询发文")
    public R<PageResult<DocDispatchVO>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            DocDispatchQueryDTO query) {
        IPage<DocDispatchVO> page = docDispatchService.pageDispatch(pageNum, pageSize, query);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询发文详情")
    public R<DocDispatchVO> detail(@PathVariable Long id) {
        return R.ok(docDispatchService.getDispatchDetail(id));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交工作流")
    @OperationLog(module = "发文管理", operation = "提交工作流")
    public R<Void> submitToWorkflow(@PathVariable Long id) {
        docDispatchService.submitToWorkflow(id);
        return R.ok();
    }

    @PostMapping("/serial/lock")
    @Operation(summary = "锁定文号")
    @OperationLog(module = "发文管理", operation = "锁定文号")
    public R<String> lockSerial(
            @RequestParam String orgCode,
            @RequestParam Integer year,
            @RequestParam Long lockBy) {
        String serialNo = docDispatchService.lockSerial(orgCode, year, lockBy);
        return R.ok(serialNo);
    }

    @PostMapping("/serial/{id}/release")
    @Operation(summary = "释放文号")
    @OperationLog(module = "发文管理", operation = "释放文号")
    public R<Void> releaseSerial(@PathVariable Long id) {
        docDispatchService.releaseSerial(id);
        return R.ok();
    }

    @PostMapping("/serial/{id}/use")
    @Operation(summary = "使用文号")
    @OperationLog(module = "发文管理", operation = "使用文号")
    public R<Void> useSerial(@PathVariable Long id, @RequestParam Long dispatchId) {
        docDispatchService.useSerial(id, dispatchId);
        return R.ok();
    }
}
