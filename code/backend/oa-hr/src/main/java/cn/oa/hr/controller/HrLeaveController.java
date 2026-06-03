package cn.oa.hr.controller;

import cn.oa.common.annotation.OperationLog;
import cn.oa.common.annotation.RequirePermission;
import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.WebUtil;
import cn.oa.hr.dto.HrLeaveCreateDTO;
import cn.oa.hr.dto.HrLeaveQueryDTO;
import cn.oa.hr.service.HrLeaveBalanceService;
import cn.oa.hr.service.HrLeaveService;
import cn.oa.hr.vo.HrLeaveBalanceVO;
import cn.oa.hr.vo.HrLeaveVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * HR请假管理Controller
 *
 * @author oa-hr
 */
@RestController
@RequestMapping("/api/hr/leaves")
@Tag(name = "HR请假管理")
@Slf4j
@RequiredArgsConstructor
public class HrLeaveController {

    private final HrLeaveService hrLeaveService;
    private final HrLeaveBalanceService hrLeaveBalanceService;

    /**
     * 创建并提交请假申请
     */
    @PostMapping
    @Operation(summary = "创建并提交请假申请")
    @OperationLog(module = "HR请假管理", operation = "提交请假申请")
    @RequirePermission("hr:leave:create")
    public R<Long> createAndSubmit(
            @RequestBody @Valid HrLeaveCreateDTO dto,
            HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        if (empId == null) {
            return R.fail("用户未登录");
        }

        // deptId 从请求属性获取，如果没有则使用默认值
        Long deptId = (Long) request.getAttribute("deptId");
        if (deptId == null) {
            deptId = 0L; // 默认部门ID，实际应从用户信息获取
        }

        Long applyId = hrLeaveService.createAndSubmit(dto, empId, deptId);
        log.info("HR Leave submitted: empId={}, applyId={}", empId, applyId);
        return R.ok(applyId);
    }

    /**
     * 分页查询请假申请
     */
    @GetMapping
    @Operation(summary = "分页查询请假申请")
    @RequirePermission("hr:leave:list")
    public R<PageResult<HrLeaveVO>> pageQuery(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "员工ID") @RequestParam(required = false) Long empId,
            @Parameter(description = "部门ID") @RequestParam(required = false) Long deptId,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "假期类型") @RequestParam(required = false) String leaveType,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            HttpServletRequest request) {

        HrLeaveQueryDTO query = new HrLeaveQueryDTO();
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        query.setEmpId(empId);
        query.setDeptId(deptId);
        query.setStatus(status);
        query.setLeaveType(leaveType);
        query.setStartDate(startDate);
        query.setEndDate(endDate);

        // 获取当前用户信息
        Long currentEmpId = WebUtil.getEmpId(request);
        boolean isAdmin = isAdmin(request);

        IPage<HrLeaveVO> page = hrLeaveService.pageQuery(query, currentEmpId, isAdmin);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    /**
     * 查询请假详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询请假详情")
    @RequirePermission("hr:leave:detail")
    public R<HrLeaveVO> getDetail(
            @Parameter(description = "申请ID") @PathVariable Long id) {
        HrLeaveVO vo = hrLeaveService.getDetail(id);
        if (vo == null) {
            return R.fail("请假申请不存在");
        }
        return R.ok(vo);
    }

    /**
     * 撤回请假申请
     */
    @PostMapping("/{id}/actions/revoke")
    @Operation(summary = "撤回请假申请")
    @OperationLog(module = "HR请假管理", operation = "撤回请假申请")
    @RequirePermission("hr:leave:revoke")
    public R<Void> revoke(
            @Parameter(description = "申请ID") @PathVariable Long id,
            HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        if (empId == null) {
            return R.fail("用户未登录");
        }

        boolean isAdmin = isAdmin(request);
        hrLeaveService.revoke(id, empId, isAdmin);
        log.info("HR Leave revoked: id={}, empId={}, isAdmin={}", id, empId, isAdmin);
        return R.ok();
    }

    /**
     * 驳回后重新提交
     */
    @PostMapping("/{id}/actions/resubmit")
    @Operation(summary = "驳回后重新提交")
    @OperationLog(module = "HR请假管理", operation = "重新提交请假申请")
    @RequirePermission("hr:leave:resubmit")
    public R<Void> resubmit(
            @Parameter(description = "申请ID") @PathVariable Long id,
            @RequestBody @Valid HrLeaveCreateDTO dto,
            HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        if (empId == null) {
            return R.fail("用户未登录");
        }

        hrLeaveService.resubmit(id, dto, empId);
        log.info("HR Leave resubmitted: id={}, empId={}", id, empId);
        return R.ok();
    }

    /**
     * 查询当前用户假期余额
     */
    @GetMapping("/my-balances")
    @Operation(summary = "查询当前用户假期余额")
    @RequirePermission("hr:leave:balance:view")
    public R<List<HrLeaveBalanceVO>> getMyBalances(
            @Parameter(description = "年度") @RequestParam(required = false) Integer year,
            HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        if (empId == null) {
            return R.fail("用户未登录");
        }

        List<HrLeaveBalanceVO> balances = hrLeaveBalanceService.getMyBalances(empId, year);
        return R.ok(balances);
    }

    /**
     * 判断当前用户是否为管理员
     */
    private boolean isAdmin(HttpServletRequest request) {
        Object isAdminAttr = request.getAttribute("isAdmin");
        if (isAdminAttr instanceof Boolean) {
            return (Boolean) isAdminAttr;
        }
        if (isAdminAttr != null) {
            return Boolean.parseBoolean(isAdminAttr.toString());
        }
        return false;
    }
}
