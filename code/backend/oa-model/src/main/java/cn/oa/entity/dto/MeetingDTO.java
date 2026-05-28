package cn.oa.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class MeetingDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "会议标题不能为空")
    private String title;

    @NotNull(message = "会议室不能为空")
    private Long roomId;

    private Long organizerId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String description;

    private String participants;

    private String status;
}
