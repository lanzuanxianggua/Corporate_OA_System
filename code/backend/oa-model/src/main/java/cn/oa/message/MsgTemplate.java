package cn.oa.message;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("msg_template")
public class MsgTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private String type;

    private String channels;

    private String titleTpl;

    private String contentTpl;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}