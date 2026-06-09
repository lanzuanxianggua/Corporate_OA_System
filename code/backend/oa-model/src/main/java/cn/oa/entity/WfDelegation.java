package cn.oa.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 工作流委派实体。
 *
 * <p>字段对齐 DB 实际 schema (oa_system.wf_delegation):
 * <ul>
 *   <li>改 startTime → startDate (DB 是 DATE)</li>
 *   <li>改 endTime → endDate (DB 是 DATE)</li>
 *   <li>删 businessType / updateTime(DB 无)</li>
 *   <li>加 processCategory / notifyDelegator(DB 有)</li>
 *   <li>加 createBy (DB 有)</li>
 * </ul>
 */
@Data
@TableName("wf_delegation")
public class WfDelegation {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long delegatorId;

    @TableField("delegate_id")
    private Long delegateToId;

    private String processCategory;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private Boolean notifyDelegator = true;

    /** ACTIVE / CANCELLED */
    private String status = "ACTIVE";

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
