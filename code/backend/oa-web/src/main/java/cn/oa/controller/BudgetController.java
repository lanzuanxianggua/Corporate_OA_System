package cn.oa.controller;

import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.entity.OaBudget;
import cn.oa.service.BudgetService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/budget")
@Tag(name = "预算管理")
public class BudgetController {

    @Autowired
    private BudgetService budgetService;

    @GetMapping("/page")
    @RequireAdmin
    @Operation(summary = "分页查询预算")
    public R<PageResult<OaBudget>> page(@RequestParam int pageNum,
                                          @RequestParam int pageSize,
                                          @RequestParam(required = false) Long deptId,
                                          @RequestParam(required = false) Integer budgetYear) {
        IPage<OaBudget> page = budgetService.pageList(pageNum, pageSize, deptId, budgetYear);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @PostMapping
    @RequireAdmin
    @Operation(summary = "新增预算")
    public R<Void> add(@RequestBody @Valid OaBudget budget) {
        budgetService.save(budget);
        log.info("Budget created: id={}", budget.getId());
        return R.ok();
    }

    @PutMapping
    @RequireAdmin
    @Operation(summary = "修改预算")
    public R<Void> update(@RequestBody @Valid OaBudget budget) {
        budgetService.updateById(budget);
        log.info("Budget updated: id={}", budget.getId());
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @RequireAdmin
    @Operation(summary = "删除预算")
    public R<Void> delete(@PathVariable Long id) {
        budgetService.removeById(id);
        log.info("Budget deleted: id={}", id);
        return R.ok();
    }

    @GetMapping("/dept/{deptId}/month")
    @Operation(summary = "查询部门月度预算")
    public R<OaBudget> getByDeptMonth(@PathVariable Long deptId,
                                       @RequestParam Integer year,
                                       @RequestParam Integer month) {
        return R.ok(budgetService.getByDeptMonth(deptId, year, month));
    }
}
