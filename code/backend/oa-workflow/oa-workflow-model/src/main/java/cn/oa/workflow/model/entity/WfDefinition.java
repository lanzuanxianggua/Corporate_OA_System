package cn.oa.workflow.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程定义表 - 工作流的核心配置表
 */
@Data
@TableName("wf_definition")
public class WfDefinition {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 流程编码，唯一标识 */
    private String code;

    /** 流程名称 */
    private String name;

    /** 版本号（递增） */
    private Integer version;

    /** 状态: DRAFT/PUBLISHED/DISABLED */
    private String status;

    /** 流程分类 */
    private String category;

    /** 关联表单定义ID */
    private Long formDefId;

    /** 流程描述 */
    private String description;

    /** 流程图标 */
    private String icon;

    /** 是否模板 */
    private Integer isTemplate;

    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private LocalDateTime deletedAt;
}