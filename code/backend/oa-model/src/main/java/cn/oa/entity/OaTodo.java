package cn.oa.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("oa_todo")
public class OaTodo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long empId;

    private String title;

    /** approval/meeting/notice/task */
    private String todoType;

    private Long businessId;

    private String businessType;

    /** 0-pending 1-done 2-ignored */
    private String status = "0";

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime doneTime;
}
