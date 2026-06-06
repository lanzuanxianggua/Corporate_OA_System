package cn.oa.hr.attendance.service;
import cn.oa.hr.attendance.dto.HrAttendanceQueryDTO;
import cn.oa.hr.attendance.entity.HrAttendanceException;
import cn.oa.hr.attendance.mapper.HrAttendanceExceptionMapper;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
@Slf4j @RequiredArgsConstructor @Service
public class HrAttendanceExceptionService {
    private final HrAttendanceExceptionMapper mapper;

    public Page<HrAttendanceException> listPage(HrAttendanceQueryDTO q) {
        return mapper.selectPage(new Page<>(q.getPageNum(), q.getPageSize()),
                new LambdaQueryWrapper<HrAttendanceException>()
                .eq(q.getEmpId()!=null, HrAttendanceException::getEmpId, q.getEmpId())
                .eq(q.getStatus()!=null, HrAttendanceException::getStatus, q.getStatus()));
    }

    @Transactional public void appeal(Long id, String content) {
        HrAttendanceException e = mapper.selectById(id);
        if (e == null) throw new BizException(RCode.NOT_FOUND, "异常不存在");
        e.setAppealContent(content); e.setAppealTime(LocalDateTime.now());
        if ("PENDING".equals(e.getStatus())) e.setStatus("PENDING");
        mapper.updateById(e);
    }

    @Transactional public void handle(Long id, String status, String comment, Long handleEmpId) {
        HrAttendanceException e = mapper.selectById(id);
        if (e == null) throw new BizException(RCode.NOT_FOUND, "异常不存在");
        e.setStatus(status); e.setHandleComment(comment); e.setHandleEmpId(handleEmpId); e.setHandleTime(LocalDateTime.now());
        mapper.updateById(e);
    }
}