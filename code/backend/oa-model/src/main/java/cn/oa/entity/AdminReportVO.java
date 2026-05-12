package cn.oa.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class AdminReportVO implements Serializable {
    private AttendanceSummary attendanceSummary;
    private List<Map<String, Object>> deptCompare;
    private List<Map<String, Object>> attendanceTrend;
    private List<Map<String, Object>> leaveAnalysis;
    private List<Map<String, Object>> employeeRanking;
    private TodayOverview todayOverview;

    @Data
    public static class AttendanceSummary implements Serializable {
        private long totalRecords;
        private long normalCount;
        private long lateCount;
        private long earlyLeaveCount;
        private long absentCount;
        private double avgAttendanceRate;
    }

    @Data
    public static class TodayOverview implements Serializable {
        private long totalEmployees;
        private long clockedIn;
        private long notClockedIn;
        private long late;
    }
}
