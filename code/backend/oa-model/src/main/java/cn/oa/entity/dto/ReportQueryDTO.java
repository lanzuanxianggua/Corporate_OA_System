package cn.oa.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class ReportQueryDTO implements Serializable {

    @NotNull(message = "月份不能为空")
    private String month;

    private Integer months = 6;

    private Long deptId;

    private String type;
}
