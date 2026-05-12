package cn.oa.service;

import cn.oa.entity.OaAttendance;
import com.baomidou.mybatisplus.extension.service.IService;

public interface AttendanceService extends IService<OaAttendance> {

    /**
     * 上班打卡
     */
    void clockIn(Long empId);

    /**
     * 下班打卡
     */
    void clockOut(Long empId);

    /**
     * 获取今日考勤记录
     */
    OaAttendance getTodayAttendance(Long empId);
}
