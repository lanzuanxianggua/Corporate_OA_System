package cn.oa.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class LeaveApplyDTO implements Serializable {
    private String leaveType;
    private String startDate;
    private String endDate;
    private Integer days;
    private String reason;
}
