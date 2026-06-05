package cn.oa.hr.leave.service;

import cn.oa.hr.leave.entity.HrLeave;
import cn.oa.hr.leave.mapper.HrLeaveMapper;
import cn.oa.workflow.service.WfInstanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * 请假业务 Service.
 */
@Service
@RequiredArgsConstructor
public class HrLeaveService {

    private final HrLeaveMapper mapper;
    private final WfInstanceService wfInstanceService;

    /**
     * 提交请假申请.
     * 1) 创建 hr_leave (PENDING)
     * 2) 启动 workflow:hr_leave 流程, businessKey = "LEAVE_" + leaveId
     * 3) 回写 wf_instance_id
     */
    @Transactional
    public Long submit(Long empId, String leaveType, String startDate, String endDate, String reason) {
        // 1) 算天数
        java.time.LocalDate start = java.time.LocalDate.parse(startDate);
        java.time.LocalDate end = java.time.LocalDate.parse(endDate);
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        BigDecimal totalDays = BigDecimal.valueOf(days);

        // 2) 创建 hr_leave
        HrLeave leave = new HrLeave();
        leave.setEmpId(empId);
        leave.setLeaveType(leaveType);
        leave.setStartDate(start);
        leave.setEndDate(end);
        leave.setTotalDays(totalDays);
        leave.setReason(reason);
        leave.setStatus("PENDING");
        leave.setCreateBy(String.valueOf(empId));
        mapper.insert(leave);
        Long leaveId = leave.getId();

        // 3) 启动流程
        String businessKey = "LEAVE_" + leaveId;
        Long wfInstanceId = wfInstanceService.start("hr_leave", businessKey, empId);
        leave.setWfInstanceId(wfInstanceId);
        mapper.updateById(leave);

        return leaveId;
    }

    public List<Map<String, Object>> listByEmpId(Long empId, int limit) {
        return mapper.findByEmpId(empId, limit);
    }

    public Map<String, Object> getDetail(Long id) {
        return mapper.findDetail(id);
    }
}
