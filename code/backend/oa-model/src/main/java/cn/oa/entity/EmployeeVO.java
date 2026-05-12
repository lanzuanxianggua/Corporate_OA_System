package cn.oa.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class EmployeeVO implements Serializable {
    private Long id;
    private String empCode;
    private String empName;
    private String phone;
    private String email;
    private String deptName;
    private String roleName;
    private Integer status;
}
