package cn.oa.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class AttendanceStatsDTO implements Serializable {
    private int normalDays;
    private int lateDays;
    private int earlyLeaveDays;
    private int absentDays;
    private double attendanceRate;
}
