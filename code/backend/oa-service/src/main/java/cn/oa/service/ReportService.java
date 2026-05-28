package cn.oa.service;

import cn.oa.entity.AdminReportVO;
import cn.oa.entity.PersonalReportVO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ReportService {
    PersonalReportVO.AttendanceSummary getPersonalAttendanceSummary(Long empId, String month);
    PersonalReportVO.AttendanceSummary getPersonalAttendanceSummary(Long empId, LocalDate start, LocalDate end);
    PersonalReportVO getPersonalReport(Long empId, String month, int months);
    AdminReportVO.AttendanceSummary getAdminAttendanceSummary(String month);
    AdminReportVO getAdminReport(String month, int months, Long deptId);
    AdminReportVO.TodayOverview getTodayOverview();
    List<Map<String, Object>> getDeptCompare(String month);
    List<Map<String, Object>> getLeaveAnalysis(String month);
    List<Map<String, Object>> getEmployeeRanking(String month, String type);
}
