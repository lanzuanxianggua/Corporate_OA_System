package cn.oa.hr.leave.service;

import cn.oa.hr.leave.dto.HrLeaveCreateDTO;
import cn.oa.hr.leave.dto.HrLeaveQueryDTO;
import cn.oa.hr.leave.entity.HrLeave;
import cn.oa.hr.leave.mapper.HrLeaveMapper;
import cn.oa.hr.leave.vo.HrLeaveVO;
import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import cn.oa.workflow.service.WfInstanceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * 请假业务 Service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrLeaveService {

    private final HrLeaveMapper mapper;
    private final HrLeaveBalanceService balanceService;
    private final WfInstanceService wfInstanceService;

    /**
     * 提交请假申请.
     * 1) 检查余额
     * 2) 冻结余额
     * 3) 创建 hr_leave (PENDING)
     * 4) 启动 workflow:hr_leave 流程, businessKey = "LEAVE_" + leaveId
     * 5) 回写 wf_instance_id
     */
    @Transactional
    public Long submit(Long empId, HrLeaveCreateDTO dto) {
        LocalDate start = dto.getStartDate();
        LocalDate end = dto.getEndDate();
        if (end.isBefore(start)) {
            throw new BizException(RCode.BAD_REQUEST, "结束日期不能早于开始日期");
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        BigDecimal totalDays = BigDecimal.valueOf(days);

        // 1) 检查余额 + 冻结
        int year = start.getYear();
        balanceService.freezeOnSubmit(empId, dto.getLeaveType(), year, totalDays);

        // 2) 创建 hr_leave
        HrLeave leave = new HrLeave();
        leave.setEmpId(empId);
        leave.setLeaveType(dto.getLeaveType());
        leave.setStartDate(start);
        leave.setEndDate(end);
        leave.setTotalDays(totalDays);
        leave.setReason(dto.getReason());
        leave.setStatus("PENDING");
        leave.setCreateBy(String.valueOf(empId));
        mapper.insert(leave);
        Long leaveId = leave.getId();

        // 3) 启动流程
        String businessKey = "LEAVE_" + leaveId;
        Long wfInstanceId = wfInstanceService.start("hr_leave", businessKey, empId);
        leave.setWfInstanceId(wfInstanceId);
        mapper.updateById(leave);

        log.info("请假申请已提交: leaveId={}, empId={}, type={}, days={}", leaveId, empId, dto.getLeaveType(), totalDays);
        return leaveId;
    }

    /**
     * 撤回请假申请.
     * 仅 PENDING 状态可撤回.
     */
    @Transactional
    public void revoke(Long id, Long empId) {
        HrLeave leave = mapper.selectById(id);
        if (leave == null) {
            throw new BizException(RCode.NOT_FOUND, "请假单不存在: " + id);
        }
        if (!leave.getEmpId().equals(empId)) {
            throw new BizException(RCode.FORBIDDEN, "只能撤回自己的请假申请");
        }
        if (!"PENDING".equals(leave.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST, "仅待审批状态可撤回, 当前状态: " + leave.getStatus());
        }

        // 1) 更新状态
        leave.setStatus("CANCELLED");
        mapper.updateById(leave);

        // 2) 解冻余额
        int year = leave.getStartDate().getYear();
        balanceService.unfreezeOnReject(empId, leave.getLeaveType(), year, leave.getTotalDays());

        log.info("请假申请已撤回: leaveId={}, empId={}", id, empId);
    }

    /**
     * 我的请假列表 (不分页, 向后兼容).
     */
    public List<Map<String, Object>> listByEmpId(Long empId, int limit) {
        return mapper.findByEmpId(empId, limit);
    }

    /**
     * 请假单详情.
     */
    public Map<String, Object> getDetail(Long id) {
        return mapper.findDetail(id);
    }

    /**
     * 分页查询我的请假列表.
     */
    public PageResult<HrLeaveVO> listPage(Long empId, HrLeaveQueryDTO query) {
        Page<HrLeave> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<HrLeave> wrapper = new LambdaQueryWrapper<HrLeave>()
                .eq(HrLeave::getEmpId, empId)
                .eq(query.getStatus() != null, HrLeave::getStatus, query.getStatus())
                .eq(query.getLeaveType() != null, HrLeave::getLeaveType, query.getLeaveType())
                .orderByDesc(HrLeave::getCreateTime);

        Page<HrLeave> result = mapper.selectPage(page, wrapper);

        List<HrLeaveVO> voList = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(voList, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 查询我的假期余额列表.
     */
    public List<Map<String, Object>> listMyBalances(Long empId) {
        return mapper.findBalancesByEmpId(empId);
    }

    private HrLeaveVO toVO(HrLeave leave) {
        HrLeaveVO vo = new HrLeaveVO();
        vo.setId(leave.getId());
        vo.setEmpId(leave.getEmpId());
        vo.setLeaveType(leave.getLeaveType());
        vo.setStartDate(leave.getStartDate());
        vo.setEndDate(leave.getEndDate());
        vo.setTotalDays(leave.getTotalDays());
        vo.setReason(leave.getReason());
        vo.setStatus(leave.getStatus());
        vo.setWfInstanceId(leave.getWfInstanceId());
        vo.setCreateTime(leave.getCreateTime());
        return vo;
    }
}
