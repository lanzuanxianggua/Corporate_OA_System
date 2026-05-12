package cn.oa.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class ReportQueryDTO implements Serializable {
    private String month;
    private Integer months = 6;
    private Long deptId;
    private String type;
}
