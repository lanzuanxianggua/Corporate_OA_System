package cn.oa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("oa_notice_read")
public class OaNoticeRead {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long noticeId;

    private Long empId;

    private LocalDateTime readTime;
}
