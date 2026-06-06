package cn.oa.hr.recruitment.controller;
import cn.oa.hr.recruitment.entity.HrRecruitJob;
import cn.oa.hr.recruitment.service.HrRecruitJobService;
import cn.oa.platform.common.api.R;
import cn.oa.platform.security.annotation.RequirePermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@Tag(name="招聘管理") @RestController @RequestMapping("/api/v1/hr-recruitment") @RequiredArgsConstructor
public class HrRecruitJobController {
    private final HrRecruitJobService service;
    @PostMapping("/jobs") @Operation(summary="发布岗位") @RequirePermission("hr-recruitment:job:list")
    public R<Long> create(@RequestBody HrRecruitJob j) { return R.ok(service.create(j)); }
    @PutMapping("/jobs/{id}") @Operation(summary="更新岗位") @RequirePermission("hr-recruitment:job:list")
    public R<Void> update(@PathVariable Long id, @RequestBody HrRecruitJob j) { j.setId(id); service.update(j); return R.ok(); }
    @GetMapping("/jobs/{id}") @Operation(summary="岗位详情")
    public R<HrRecruitJob> get(@PathVariable Long id) { return R.ok(service.getById(id)); }
    @GetMapping("/jobs") @Operation(summary="岗位列表")
    public R<Page<HrRecruitJob>> list(@RequestParam(required=false) String status,
        @RequestParam(defaultValue="1") int pn, @RequestParam(defaultValue="10") int ps) { return R.ok(service.listPage(status, pn, ps)); }
}