package cn.oa.controller;

import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.PageParamUtil;
import cn.oa.entity.OaEmpArchive;
import cn.oa.service.EmpArchiveService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/emp-archive")
@Tag(name = "员工档案管理")
public class EmpArchiveController {

    @Autowired
    private EmpArchiveService empArchiveService;

    @GetMapping("/{empId}")
    @Operation(summary = "根据员工ID查询档案(含员工信息)")
    public R<OaEmpArchive> getByEmpId(@PathVariable Long empId) {
        return R.ok(empArchiveService.getByEmpIdWithInfo(empId));
    }

    @PostMapping
    @Operation(summary = "创建/更新员工档案")
    @cn.oa.common.annotation.OperationLog(module = "员工档案", operation = "创建/更新员工档案")
    public R<Void> save(@RequestBody @Valid OaEmpArchive archive) {
        if (archive.getId() != null) {
            empArchiveService.updateById(archive);
            log.info("Emp archive updated: empId={}", archive.getEmpId());
        } else {
            OaEmpArchive existing = empArchiveService.getByEmpId(archive.getEmpId());
            if (existing != null) {
                archive.setId(existing.getId());
                empArchiveService.updateById(archive);
                log.info("Emp archive updated: empId={}", archive.getEmpId());
            } else {
                empArchiveService.save(archive);
                log.info("Emp archive created: empId={}", archive.getEmpId());
            }
        }
        return R.ok();
    }

    @GetMapping("/page")
    @RequireAdmin
    @Operation(summary = "分页查询员工档案")
    public R<PageResult<OaEmpArchive>> page(@RequestParam int pageNum,
                                              @RequestParam int pageSize,
                                              @RequestParam(required = false) String searchKey) {
        IPage<OaEmpArchive> page = empArchiveService.pageWithEmpInfo(PageParamUtil.pageNum(pageNum), PageParamUtil.pageSize(pageSize), searchKey);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }
}

