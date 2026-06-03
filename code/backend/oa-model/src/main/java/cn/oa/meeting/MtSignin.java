package cn.oa.meeting;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mt_signin")
public class MtSignin {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long bookingId;

    private Long empId;

    private LocalDateTime signinTime;

    private String signinType;

    private String location;

    private Integer verified;
}