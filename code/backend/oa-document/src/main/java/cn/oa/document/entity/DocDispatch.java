package cn.oa.document.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 发文.
 *
 * <p>对应表 doc_dispatches.
 * 状态: DRAFT / PENDING / APPROVED / PUBLISHED / ARCHIVED
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_dispatches")
@Schema(description = "发文")
public class DocDispatch extends BaseEntity {

    @Schema(description = "标题")
    @TableField("title")
    private String title;

    @Schema(description = "主题词")
    @TableField("subject_word")
    private String subjectWord;

    @Schema(description = "发送部门")
    @TableField("send_to_dept")
    private String sendToDept;

    @Schema(description = "抄送部门")
    @TableField("copy_to_dept")
    private String copyToDept;

    @Schema(description = "紧急程度: URGENT/EMERGENCY/NORMAL")
    @TableField("urgency")
    private String urgency;

    @Schema(description = "密级: TOP_SECRET/SECRET/CONFIDENTIAL/NORMAL")
    @TableField("security_level")
    private String securityLevel;

    @Schema(description = "正文内容(富文本)")
    @TableField("content")
    private String content;

    @Schema(description = "状态: DRAFT/PENDING/APPROVED/PUBLISHED/ARCHIVED")
    @TableField("status")
    private String status;

    @Schema(description = "申请人 emp_id")
    @TableField("emp_id")
    private Long empId;

    @Schema(description = "所属部门 dept_id")
    @TableField("dept_id")
    private Long deptId;

    @Schema(description = "流程实例 ID")
    @TableField("wf_instance_id")
    private Long wfInstanceId;
}
