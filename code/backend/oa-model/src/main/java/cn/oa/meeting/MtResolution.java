package cn.oa.meeting;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("mt_resolution")
public class MtResolution {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long bookingId;

    private String content;

    private Long assigneeId;

    private LocalDate dueDate;

    private Integer convertedToTask;

    private Long taskId;

    private String status = "0";

    private Integer sortOrder;

    private LocalDateTime createdAt;
}