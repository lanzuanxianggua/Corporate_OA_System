package cn.oa.finance.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.annotation.RequirePermission;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.finance.dto.FinBudgetCreateDTO;
import cn.oa.finance.dto.FinBudgetQueryDTO;
import cn.oa.finance.dto.FinBudgetUpdateDTO;
import cn.oa.finance.service.FinBudgetService;
import cn.oa.finance.vo.FinBudgetVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 费控-预算管理 Controller
 *
 * @author oa-finance
 */
@RestController
@RequestMapping("/api/finance/budgets")
@Tag(name = "费控-预算管理")
@Slf4j
@RequiredArgsConstructor
public class FinBudgetController {

    private final FinBudgetService finBudgetService;

    /**
     * 创建预算
     */
    @PostMapping
    @Operation(summary = "创建预算")
    @OperationLog(module = "费控-预算管理", operation = "创建预算")
    @RequirePermission("finance:budget:create")
    public R<Long> createBudget(@RequestBody @Valid FinBudgetCreateDTO dto) {
        Long id = finBudgetService.createBudget(dto);
        log.info("Budget created: id={}", id);
        return R.ok(id);
    }

    /**
     * 更新预算
     */
    @PutMapping
    @Operation(summary = "更新预算")
    @OperationLog(module = "费控-预算管理", operation = "更新预算")
    @RequirePermission("finance:budget:update")
    public R<Void> updateBudget(@RequestBody @Valid FinBudgetUpdateDTO dto) {
        finBudgetService.updateBudget(dto);
        return R.ok();
    }

    /**
     * 分页查询预算
     */
    @GetMapping
    @Operation(summary = "分页查询预算")
    @RequirePermission("finance:budget:list")
    public R<PageResult<FinBudgetVO>> pageQuery(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "部门ID") @RequestParam(required = false) Long deptId,
            @Parameter(description = "项目ID") @RequestParam(required = false) Long projectId,
            @Parameter(description = "费用类别") @RequestParam(required = false) String expenseCategory,
            @Parameter(description = "年份") @RequestParam(required = false) Integer year,
            @Parameter(description = "月份") @RequestParam(required = false) Integer month,
            @Parameter(description = "状态") @RequestParam(required = false) String status) {

        FinBudgetQueryDTO query = new FinBudgetQueryDTO();
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        query.setDeptId(deptId);
        query.setProjectId(projectId);
        query.setExpenseCategory(expenseCategory);
        query.setYear(year);
        query.setMonth(month);
        query.setStatus(status);

        IPage<FinBudgetVO> page = finBudgetService.pageQuery(query);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    /**
     * 查询预算详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询预算详情")
    @RequirePermission("finance:budget:detail")
    public R<FinBudgetVO> getDetail(@Parameter(description = "预算ID") @PathVariable Long id) {
        FinBudgetVO vo = finBudgetService.getDetail(id);
        if (vo == null) {
            return R.fail("预算记录不存在");
        }
        return R.ok(vo);
    }

    /**
     * 删除预算
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除预算")
    @OperationLog(module = "费控-预算管理", operation = "删除预算")
    @RequirePermission("finance:budget:delete")
    public R<Void> deleteBudget(@Parameter(description = "预算ID") @PathVariable Long id) {
        finBudgetService.deleteBudget(id);
        return R.ok();
    }
}
