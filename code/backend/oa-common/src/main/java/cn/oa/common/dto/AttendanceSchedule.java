package cn.oa.common.dto;

import java.time.LocalTime;

public class AttendanceSchedule {

    private final LocalTime workStart;
    private final LocalTime workEnd;
    private final int lateThresholdMinutes;

    public AttendanceSchedule(LocalTime workStart, LocalTime workEnd, int lateThresholdMinutes) {
        this.workStart = workStart;
        this.workEnd = workEnd;
        this.lateThresholdMinutes = lateThresholdMinutes;
    }

    public static AttendanceSchedule defaultSchedule() {
        return new AttendanceSchedule(LocalTime.of(9, 0), LocalTime.of(18, 0), 0);
    }

    public LocalTime getWorkStart() {
        return workStart;
    }

    public LocalTime getWorkEnd() {
        return workEnd;
    }

    public int getLateThresholdMinutes() {
        return lateThresholdMinutes;
    }

    public LocalTime getLateDeadline() {
        return workStart.plusMinutes(Math.max(lateThresholdMinutes, 0));
    }
}
