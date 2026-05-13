package cn.oa.service.impl;

import cn.oa.common.exception.BusinessException;
import cn.oa.entity.OaAttendance;
import cn.oa.mapper.OaAttendanceMapper;
import cn.oa.service.AttendanceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Service
public class AttendanceServiceImpl extends ServiceImpl<OaAttendanceMapper, OaAttendance> implements AttendanceService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void clockIn(Long empId) {
        // 先查数据库是否已有今日记录
        OaAttendance existing = getTodayAttendance(empId);
        if (existing != null) {
            throw new BusinessException("今日已打卡");
        }
        // 保存打卡记录到数据库
        OaAttendance attendance = new OaAttendance();
        attendance.setEmpId(empId);
        attendance.setWorkDate(LocalDate.now());
        attendance.setClockIn(LocalDateTime.now());
        attendance.setStatus(1);
        this.save(attendance);
    }

    @Override
    public void clockOut(Long empId) {
        OaAttendance attendance = getTodayAttendance(empId);
        if (attendance == null) {
            throw new BusinessException("今日未打卡，请先签到");
        }
        if (attendance.getClockOut() != null) {
            throw new BusinessException("今日已签退");
        }
        attendance.setClockOut(LocalDateTime.now());
        this.updateById(attendance);
    }

    @Override
    public OaAttendance getTodayAttendance(Long empId) {
        LambdaQueryWrapper<OaAttendance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaAttendance::getEmpId, empId)
                .eq(OaAttendance::getWorkDate, LocalDate.now());
        return this.getOne(wrapper);
    }
}
