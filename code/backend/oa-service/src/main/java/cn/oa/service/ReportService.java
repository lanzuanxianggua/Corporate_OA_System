package cn.oa.service;

import cn.oa.entity.AdminReportVO;
import cn.oa.entity.PersonalReportVO;

public interface ReportService {
    PersonalReportVO.AttendanceSummary getPersonalAttendanceSummary(Long empId, String month);
    PersonalReportVO getPersonalReport(Long empId, String month, int months);
    AdminReportVO.AttendanceSummary getAdminAttendanceSummary(String month);
    AdminReportVO getAdminReport(String month, int months, Long deptId);
    AdminReportVO.TodayOverview getTodayOverview();
    Object getDeptCompare(String month);
    Object getLeaveAnalysis(String month);
    Object getEmployeeRanking(String month, String type);
}
