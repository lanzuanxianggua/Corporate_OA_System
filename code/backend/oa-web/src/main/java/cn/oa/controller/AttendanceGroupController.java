package cn.oa.controller;

import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.entity.OaAttendanceGroup;
import cn.oa.service.AttendanceGroupService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance-group")
@Tag(name = "考勤组管理")
public class AttendanceGroupController {

    @Autowired
    private AttendanceGroupService attendanceGroupService;

    @GetMapping("/page")
    @RequireAdmin
    @Operation(summary = "分页查询考勤组")
    public R<PageResult<OaAttendanceGroup>> page(@RequestParam int pageNum,
                                                  @RequestParam int pageSize,
                                                  @RequestParam(required = false) String groupName) {
        IPage<OaAttendanceGroup> page = attendanceGroupService.pageList(pageNum, pageSize, groupName);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @PostMapping
    @RequireAdmin
    @Operation(summary = "新增考勤组")
    public R<Void> add(@RequestBody OaAttendanceGroup group) {
        attendanceGroupService.save(group);
        return R.ok();
    }

    @PutMapping
    @RequireAdmin
    @Operation(summary = "修改考勤组")
    public R<Void> update(@RequestBody OaAttendanceGroup group) {
        attendanceGroupService.updateById(group);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @RequireAdmin
    @Operation(summary = "删除考勤组")
    public R<Void> delete(@PathVariable Long id) {
        attendanceGroupService.removeById(id);
        return R.ok();
    }

    @PostMapping("/{id}/employees")
    @RequireAdmin
    @Operation(summary = "分配员工到考勤组")
    public R<Void> assignEmployees(@PathVariable Long id, @RequestBody Map<String, List<Long>> params) {
        attendanceGroupService.assignEmployees(id, params.get("empIds"));
        return R.ok();
    }

    @DeleteMapping("/{id}/employees")
    @RequireAdmin
    @Operation(summary = "从考勤组移除员工")
    public R<Void> removeEmployees(@PathVariable Long id, @RequestBody Map<String, List<Long>> params) {
        attendanceGroupService.removeEmployees(id, params.get("empIds"));
        return R.ok();
    }
}
