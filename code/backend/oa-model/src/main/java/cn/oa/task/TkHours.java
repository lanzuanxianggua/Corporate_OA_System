package cn.oa.task;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("tk_hours")
public class TkHours {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Long empId;

    private BigDecimal hours;

    private LocalDate workDate;

    private String description;

    private LocalDateTime createdAt;
}