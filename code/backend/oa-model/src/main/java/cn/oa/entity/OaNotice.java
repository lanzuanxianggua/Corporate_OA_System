package cn.oa.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("oa_notice")
public class OaNotice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    private Integer noticeType;

    private Long publisherId;

    private Integer status = 0;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private String delFlag;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /** 发布人姓名（非数据库字段，查询时填充） */
    @TableField(exist = false)
    private String publisher;

    /** 当前用户是否已读（非数据库字段，查询时填充） */
    @TableField(exist = false)
    private Boolean isRead;
}
