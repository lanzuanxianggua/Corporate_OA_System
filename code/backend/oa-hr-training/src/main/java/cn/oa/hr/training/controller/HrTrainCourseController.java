package cn.oa.hr.training.controller;
import cn.oa.hr.training.entity.HrTrainCourse;
import cn.oa.hr.training.service.HrTrainCourseService;
import cn.oa.platform.common.api.R;
import cn.oa.platform.security.annotation.RequirePermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@Tag(name="培训管理") @RestController @RequestMapping("/api/v1/hr-training") @RequiredArgsConstructor
public class HrTrainCourseController {
    private final HrTrainCourseService service;
    @PostMapping("/courses") @Operation(summary="创建课程") @RequirePermission("hr-training:course:list")
    public R<Long> create(@RequestBody HrTrainCourse c) { return R.ok(service.create(c)); }
    @PutMapping("/courses/{id}") @Operation(summary="更新课程") @RequirePermission("hr-training:course:list")
    public R<Void> update(@PathVariable Long id, @RequestBody HrTrainCourse c) { c.setId(id); service.update(c); return R.ok(); }
    @DeleteMapping("/courses/{id}") @Operation(summary="删除课程") @RequirePermission("hr-training:course:list")
    public R<Void> delete(@PathVariable Long id) { service.delete(id); return R.ok(); }
    @GetMapping("/courses") @Operation(summary="课程列表") @RequirePermission("hr-training:course:list")
    public R<Page<HrTrainCourse>> list(@RequestParam(required=false) String status,
        @RequestParam(defaultValue="1") int pn, @RequestParam(defaultValue="10") int ps) { return R.ok(service.listPage(status, pn, ps)); }
}