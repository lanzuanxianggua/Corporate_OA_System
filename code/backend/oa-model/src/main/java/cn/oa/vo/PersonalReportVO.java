package cn.oa.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class PersonalReportVO implements Serializable {
    private AttendanceSummary attendanceSummary;
    private List<Map<String, Object>> attendanceTrend;
    private List<Map<String, Object>> leaveSummary;
    private MonthlyCompare monthlyCompare;

    @Data
    public static class AttendanceSummary implements Serializable {
        private int normalDays;
        private int lateDays;
        private int earlyLeaveDays;
        private int absentDays;
        private int totalDays;
        private double attendanceRate;
    }

    @Data
    public static class MonthlyCompare implements Serializable {
        private int currentMonthNormal;
        private int lastMonthNormal;
        private int currentMonthLate;
        private int lastMonthLate;
        private int currentMonthAbsent;
        private int lastMonthAbsent;
    }
}
