package cn.oa.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class OnlineUserVO implements Serializable {
    private Long empId;
    private String empName;
    private String ip;
    private String browser;
    private String loginTime;
    private String deptName;
}
