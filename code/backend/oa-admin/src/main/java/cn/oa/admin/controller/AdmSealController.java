package cn.oa.admin.controller;

import cn.oa.admin.dto.AdmSealUsageCreateDTO;
import cn.oa.admin.entity.AdmSeal;
import cn.oa.admin.service.AdmSealService;
import cn.oa.admin.vo.AdmSealUsageVO;
import cn.oa.admin.vo.AdmSealVO;
import cn.oa.common.annotation.OperationLog;
import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.WebUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 印章管理 Controller
 *
 * @author oa-admin
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/seals")
@Tag(name = "印章管理")
public class AdmSealController {

    @Autowired
    private AdmSealService admSealService;

    // ============ 印章基础管理 ============

    @GetMapping("/page")
    @Operation(summary = "分页查询印章")
    public R<PageResult<AdmSealVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        IPage<AdmSealVO> page = admSealService.pageSeals(keyword, status, pageNum, pageSize);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询印章详情")
    public R<AdmSealVO> detail(@PathVariable Long id) {
        return R.ok(admSealService.getSealDetail(id));
    }

    @PostMapping
    @RequireAdmin
    @Operation(summary = "新增印章")
    @OperationLog(module = "印章管理", operation = "新增印章")
    public R<Long> create(@RequestBody @Valid AdmSeal seal) {
        Long id = admSealService.createSeal(seal);
        log.info("印章创建成功: id={}", id);
        return R.ok(id);
    }

    @PutMapping
    @RequireAdmin
    @Operation(summary = "修改印章")
    @OperationLog(module = "印章管理", operation = "修改印章")
    public R<Void> update(@RequestBody @Valid AdmSeal seal) {
        admSealService.updateSeal(seal);
        log.info("印章更新成功: id={}", seal.getId());
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @RequireAdmin
    @Operation(summary = "删除印章")
    @OperationLog(module = "印章管理", operation = "删除印章")
    public R<Void> delete(@PathVariable Long id) {
        admSealService.deleteSeal(id);
        log.info("印章删除成功: id={}", id);
        return R.ok();
    }

    // ============ 印章使用申请 ============

    @PostMapping("/usage")
    @Operation(summary = "创建印章使用申请")
    @OperationLog(module = "印章管理", operation = "创建用印申请")
    public R<Long> createUsage(@RequestBody @Valid AdmSealUsageCreateDTO dto, HttpServletRequest request) {
        Long applicantId = WebUtil.getEmpId(request);
        Long id = admSealService.createUsage(dto, applicantId);
        log.info("用印申请创建成功: id={}, applicantId={}", id, applicantId);
        return R.ok(id);
    }

    @PostMapping("/usage/{id}/approve")
    @RequireAdmin
    @Operation(summary = "审批通过印章使用申请")
    @OperationLog(module = "印章管理", operation = "审批通过用印申请")
    public R<Void> approveUsage(@PathVariable Long id) {
        admSealService.approveUsage(id);
        log.info("用印申请已审批通过: id={}", id);
        return R.ok();
    }

    @PostMapping("/usage/{id}/reject")
    @RequireAdmin
    @Operation(summary = "驳回印章使用申请")
    @OperationLog(module = "印章管理", operation = "驳回用印申请")
    public R<Void> rejectUsage(@PathVariable Long id, @RequestParam(required = false) String reason) {
        admSealService.rejectUsage(id, reason);
        log.info("用印申请已驳回: id={}", id);
        return R.ok();
    }

    @GetMapping("/usage/page")
    @Operation(summary = "分页查询印章使用记录")
    public R<PageResult<AdmSealUsageVO>> usagePage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long sealId,
            @RequestParam(required = false) String status) {
        IPage<AdmSealUsageVO> page = admSealService.pageUsages(sealId, status, pageNum, pageSize);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }
}
