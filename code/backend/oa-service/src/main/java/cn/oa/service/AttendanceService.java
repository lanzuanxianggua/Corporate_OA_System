package cn.oa.service;

import cn.oa.entity.OaAttendance;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AttendanceService extends IService<OaAttendance> {

    void clockIn(Long empId);

    void clockOut(Long empId);

    OaAttendance getTodayAttendance(Long empId);

    List<OaAttendance> getAttendanceHistory(Long empId, LocalDate startDate, LocalDate endDate);

    IPage<Map<String, Object>> adminPage(int pageNum, int pageSize, String empName, Integer status, LocalDate startDate, LocalDate endDate);

    void markLeaveAttendance(Long empId, LocalDate startDate, LocalDate endDate);

    void markTripAttendance(Long empId, LocalDate startDate, LocalDate endDate);

    void removeMarkedAttendance(Long empId, LocalDate startDate, LocalDate endDate, Integer status);

    List<OaAttendance> getHistoryByDateRange(LocalDate startDate, LocalDate endDate);
}
