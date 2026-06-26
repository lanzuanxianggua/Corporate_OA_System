package cn.oa.controller;

import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.PageParamUtil;
import cn.oa.common.utils.WebUtil;
import cn.oa.entity.OaSalaryRecord;
import cn.oa.entity.OaSalaryStructure;
import cn.oa.service.SalaryRecordService;
import cn.oa.service.SalaryStructureService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/salary")
@Tag(name = "薪资管理")
public class SalaryController {

    @Autowired
    private SalaryStructureService salaryStructureService;

    @Autowired
    private SalaryRecordService salaryRecordService;

    @GetMapping("/structure/page")
    @RequireAdmin
    @Operation(summary = "分页查询薪资结构")
    public R<PageResult<OaSalaryStructure>> structurePage(@RequestParam int pageNum,
                                                           @RequestParam int pageSize,
                                                           @RequestParam(required = false) Long empId,
                                                           @RequestParam(required = false) String searchKey) {
        IPage<OaSalaryStructure> page = salaryStructureService.pageList(PageParamUtil.pageNum(pageNum), PageParamUtil.pageSize(pageSize), empId, searchKey);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @PostMapping("/structure")
    @RequireAdmin
    @Operation(summary = "新增薪资结构")
    @cn.oa.common.annotation.OperationLog(module = "薪资管理", operation = "新增薪资结构")
    public R<Void> addStructure(@RequestBody @Valid OaSalaryStructure structure) {
        salaryStructureService.save(structure);
        log.info("Salary structure created: empId={}", structure.getEmpId());
        return R.ok();
    }

    @PutMapping("/structure")
    @RequireAdmin
    @Operation(summary = "修改薪资结构")
    @cn.oa.common.annotation.OperationLog(module = "薪资管理", operation = "修改薪资结构")
    public R<Void> updateStructure(@RequestBody @Valid OaSalaryStructure structure) {
        salaryStructureService.updateById(structure);
        log.info("Salary structure updated: id={}", structure.getId());
        return R.ok();
    }

    @GetMapping("/record/page")
    @RequireAdmin
    @Operation(summary = "分页查询薪资记录")
    public R<PageResult<OaSalaryRecord>> recordPage(@RequestParam int pageNum,
                                                      @RequestParam int pageSize,
                                                      @RequestParam(required = false) Long empId,
                                                      @RequestParam(required = false) String salaryMonth,
                                                      @RequestParam(required = false) String searchKey) {
        IPage<OaSalaryRecord> page = salaryRecordService.pageList(PageParamUtil.pageNum(pageNum), PageParamUtil.pageSize(pageSize), empId, salaryMonth, searchKey);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @PostMapping("/my")
    @Operation(summary = "查询当前用户最新薪资记录")
    public R<OaSalaryRecord> my(@RequestBody(required = false) VerifySalaryDTO dto, HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        String month = dto != null ? dto.getMonth() : null;
        String password = dto != null ? dto.getPassword() : null;
        return R.ok(salaryRecordService.myLatestRecord(empId, month, password));
    }

    @Data
    public static class VerifySalaryDTO {
        private String month;
        private String password;
    }
}

