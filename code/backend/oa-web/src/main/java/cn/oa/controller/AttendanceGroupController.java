package cn.oa.controller;

import cn.oa.common.annotation.RequireAdmin;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.WebUtil;
import cn.oa.entity.OaAttendanceGroup;
import cn.oa.entity.dto.EmpIdsDTO;
import cn.oa.service.AttendanceGroupService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
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
    @cn.oa.common.annotation.OperationLog(module = "考勤组管理", operation = "新增考勤组")
    public R<Void> add(@RequestBody @Valid OaAttendanceGroup group) {
        attendanceGroupService.save(group);
        log.info("Attendance group created: id={}", group.getId());
        return R.ok();
    }

    @PutMapping
    @RequireAdmin
    @Operation(summary = "修改考勤组")
    @cn.oa.common.annotation.OperationLog(module = "考勤组管理", operation = "修改考勤组")
    public R<Void> update(@RequestBody @Valid OaAttendanceGroup group) {
        attendanceGroupService.updateById(group);
        log.info("Attendance group updated: id={}", group.getId());
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @RequireAdmin
    @Operation(summary = "删除考勤组")
    @cn.oa.common.annotation.OperationLog(module = "考勤组管理", operation = "删除考勤组")
    public R<Void> delete(@PathVariable Long id) {
        attendanceGroupService.removeById(id);
        log.info("Attendance group deleted: id={}", id);
        return R.ok();
    }

    @PostMapping("/{id}/employees")
    @RequireAdmin
    @Operation(summary = "分配员工到考勤组")
    @cn.oa.common.annotation.OperationLog(module = "考勤组管理", operation = "分配员工到考勤组")
    public R<Void> assignEmployees(@PathVariable Long id, @RequestBody @Valid EmpIdsDTO dto) {
        attendanceGroupService.assignEmployees(id, dto.getEmpIds());
        log.info("Employees assigned to attendance group: groupId={}, count={}", id, dto.getEmpIds().size());
        return R.ok();
    }

    @PostMapping("/{id}/employees/remove")
    @RequireAdmin
    @Operation(summary = "从考勤组移除员工")
    @cn.oa.common.annotation.OperationLog(module = "考勤组管理", operation = "从考勤组移除员工")
    public R<Void> removeEmployees(@PathVariable Long id, @RequestBody @Valid EmpIdsDTO dto) {
        attendanceGroupService.removeEmployees(id, dto.getEmpIds());
        log.info("Employees removed from attendance group: groupId={}, count={}", id, dto.getEmpIds().size());
        return R.ok();
    }
}
