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
        String date = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String redisKey = "attendance:" + empId + ":" + date;
        // 如果已存在则抛异常"今日已打卡"
        Boolean exists = redisTemplate.hasKey(redisKey);
        if (exists != null && exists) {
            throw new BusinessException("今日已打卡");
        }
        // 保存打卡记录到数据库
        OaAttendance attendance = new OaAttendance();
        attendance.setEmpId(empId);
        attendance.setWorkDate(LocalDate.now());
        attendance.setClockIn(LocalDateTime.now());
        attendance.setStatus(1);
        this.save(attendance);
        // 设置Redis标记，86400s过期
        redisTemplate.opsForValue().set(redisKey, attendance.getId(), 86400, TimeUnit.SECONDS);
    }

    @Override
    public void clockOut(Long empId) {
        // 获取今日打卡记录
        OaAttendance attendance = getTodayAttendance(empId);
        if (attendance == null) {
            throw new BusinessException("今日未打卡，请先签到");
        }
        // 更新下班打卡时间
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
