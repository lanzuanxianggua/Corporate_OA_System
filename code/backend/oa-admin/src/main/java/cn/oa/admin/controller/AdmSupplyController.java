package cn.oa.admin.controller;

import cn.oa.admin.dto.AdmStockOperationDTO;
import cn.oa.admin.entity.AdmSupply;
import cn.oa.admin.service.AdmSupplyService;
import cn.oa.admin.vo.AdmSupplyVO;
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
 * 办公用品管理 Controller
 *
 * @author oa-admin
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/supplies")
@Tag(name = "办公用品管理")
public class AdmSupplyController {

    @Autowired
    private AdmSupplyService admSupplyService;

    // ============ 用品基础管理 ============

    @GetMapping("/page")
    @Operation(summary = "分页查询办公用品（含库存信息）")
    public R<PageResult<AdmSupplyVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        IPage<AdmSupplyVO> page = admSupplyService.pageSupplies(keyword, category, pageNum, pageSize);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询用品详情（含库存）")
    public R<AdmSupplyVO> detail(@PathVariable Long id) {
        return R.ok(admSupplyService.getSupplyDetail(id));
    }

    @PostMapping
    @RequireAdmin
    @Operation(summary = "新增办公用品")
    @OperationLog(module = "办公用品管理", operation = "新增办公用品")
    public R<Long> create(@RequestBody @Valid AdmSupply supply) {
        Long id = admSupplyService.createSupply(supply);
        log.info("办公用品创建成功: id={}", id);
        return R.ok(id);
    }

    @PutMapping
    @RequireAdmin
    @Operation(summary = "修改办公用品")
    @OperationLog(module = "办公用品管理", operation = "修改办公用品")
    public R<Void> update(@RequestBody @Valid AdmSupply supply) {
        admSupplyService.updateSupply(supply);
        log.info("办公用品更新成功: id={}", supply.getId());
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @RequireAdmin
    @Operation(summary = "删除办公用品")
    @OperationLog(module = "办公用品管理", operation = "删除办公用品")
    public R<Void> delete(@PathVariable Long id) {
        admSupplyService.deleteSupply(id);
        log.info("办公用品删除成功: id={}", id);
        return R.ok();
    }

    // ============ 库存操作 ============

    @PostMapping("/inbound")
    @RequireAdmin
    @Operation(summary = "入库")
    @OperationLog(module = "办公用品管理", operation = "入库")
    public R<Void> inbound(@RequestBody @Valid AdmStockOperationDTO dto, HttpServletRequest request) {
        String operator = WebUtil.getEmpName(request);
        admSupplyService.inbound(dto.getSupplyId(), dto.getQuantity(), operator);
        log.info("入库成功: supplyId={}, quantity={}", dto.getSupplyId(), dto.getQuantity());
        return R.ok();
    }

    @PostMapping("/outbound")
    @RequireAdmin
    @Operation(summary = "出库")
    @OperationLog(module = "办公用品管理", operation = "出库")
    public R<Void> outbound(@RequestBody @Valid AdmStockOperationDTO dto, HttpServletRequest request) {
        String operator = WebUtil.getEmpName(request);
        admSupplyService.outbound(dto.getSupplyId(), dto.getQuantity(), operator);
        log.info("出库成功: supplyId={}, quantity={}", dto.getSupplyId(), dto.getQuantity());
        return R.ok();
    }

    // ============ 库存预警 ============

    @GetMapping("/low-stock")
    @Operation(summary = "查询低库存办公用品列表")
    public R<PageResult<AdmSupplyVO>> lowStock(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "10") Integer threshold) {
        IPage<AdmSupplyVO> page = admSupplyService.pageLowStockSupplies(threshold, pageNum, pageSize);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }
}
