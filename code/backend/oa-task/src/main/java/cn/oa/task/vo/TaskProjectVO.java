package cn.oa.task.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目VO
 */
@Data
public class TaskProjectVO {

    private Long id;
    private String name;
    private String description;
    private String status;
    private String statusName;
    private Integer progress;
    private Long ownerId;
    private String ownerName;
    private Integer memberCount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate plannedStartDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate plannedEndDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate actualStartDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate actualEndDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** 成员列表（详情时返回） */
    private List<MemberVO> members;

    @Data
    public static class MemberVO {
        private Long id;
        private Long empId;
        private String empName;
        private String role;
        private String roleName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime joinedAt;
    }
}
