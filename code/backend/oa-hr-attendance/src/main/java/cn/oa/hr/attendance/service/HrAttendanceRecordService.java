package cn.oa.hr.attendance.service;
import cn.oa.hr.attendance.dto.HrAttendanceRecordCreateDTO;
import cn.oa.hr.attendance.dto.HrAttendanceQueryDTO;
import cn.oa.hr.attendance.entity.HrAttendanceRecord;
import cn.oa.hr.attendance.mapper.HrAttendanceRecordMapper;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Slf4j @RequiredArgsConstructor @Service
public class HrAttendanceRecordService {
    private final HrAttendanceRecordMapper mapper;

    @Transactional public Long clockIn(HrAttendanceRecordCreateDTO dto, Long empId) {
        LocalDate today = dto.getClockDate() != null ? dto.getClockDate() : LocalDate.now();
        HrAttendanceRecord exist = mapper.selectOne(new LambdaQueryWrapper<HrAttendanceRecord>()
                .eq(HrAttendanceRecord::getEmpId, empId).eq(HrAttendanceRecord::getClockDate, today));
        if (exist != null && exist.getClockInTime() != null) throw new BizException(RCode.BAD_REQUEST, "今日已打卡");
        HrAttendanceRecord r = exist != null ? exist : new HrAttendanceRecord();
        r.setEmpId(empId); r.setClockDate(today);
        r.setClockInTime(dto.getClockTime() != null ? dto.getClockTime() : LocalDateTime.now());
        r.setClockInMethod(dto.getMethod());
        if (exist == null) { mapper.insert(r); log.info("打卡签到: empId={}", empId); }
        else { mapper.updateById(r); }
        return r.getId();
    }

    @Transactional public Long clockOut(Long empId) {
        LocalDate today = LocalDate.now();
        HrAttendanceRecord r = mapper.selectOne(new LambdaQueryWrapper<HrAttendanceRecord>()
                .eq(HrAttendanceRecord::getEmpId, empId).eq(HrAttendanceRecord::getClockDate, today));
        if (r == null) throw new BizException(RCode.BAD_REQUEST, "今日未签到");
        r.setClockOutTime(LocalDateTime.now()); mapper.updateById(r);
        return r.getId();
    }

    public Page<HrAttendanceRecord> listPage(HrAttendanceQueryDTO q) {
        Page<HrAttendanceRecord> p = new Page<>(q.getPageNum(), q.getPageSize());
        return mapper.selectPage(p, new LambdaQueryWrapper<HrAttendanceRecord>()
                .eq(q.getEmpId()!=null, HrAttendanceRecord::getEmpId, q.getEmpId())
                .eq(q.getStatus()!=null, HrAttendanceRecord::getStatus, q.getStatus())
                .ge(q.getStartDate()!=null, HrAttendanceRecord::getClockDate, q.getStartDate())
                .le(q.getEndDate()!=null, HrAttendanceRecord::getClockDate, q.getEndDate())
                .orderByDesc(HrAttendanceRecord::getClockDate));
    }
}