package cn.oa.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class DashboardVO implements Serializable {
    private long employeeTotal;
    private AttendanceInfo attendance;
    private LeaveInfo leave;
    private List<Map<String, Object>> departmentDistribution;
    private List<Map<String, Object>> attendanceTrend;

    @Data
    public static class AttendanceInfo implements Serializable {
        private long clockedIn;
        private long notClockedIn;
        private long late;
        private long earlyLeave;
        private long absent;
    }

    @Data
    public static class LeaveInfo implements Serializable {
        private long total;
        private long pending;
        private long approved;
        private Map<String, Long> byType;
    }
}
