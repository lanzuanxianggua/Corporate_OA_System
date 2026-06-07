package cn.oa.document.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 签报.
 *
 * <p>对应表 doc_sign_reports.
 * 状态: DRAFT / PENDING / APPROVED / REJECTED
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_sign_reports")
@Schema(description = "签报")
public class DocSignReport extends BaseEntity {

    @Schema(description = "签报编号 (唯一)")
    @TableField("report_no")
    private String reportNo;

    @Schema(description = "标题")
    @TableField("title")
    private String title;

    @Schema(description = "签报类型: GENERAL/URGENT/SPECIAL")
    @TableField("report_type")
    private String reportType;

    @Schema(description = "正文")
    @TableField("content")
    private String content;

    @Schema(description = "附件 ID 列表 (JSON 数组)")
    @TableField("attachment_ids")
    private String attachmentIds;

    @Schema(description = "状态: DRAFT/PENDING/APPROVED/REJECTED/ARCHIVED")
    @TableField("status")
    private String status;

    @Schema(description = "申请人 emp_id (关联 sys_employee.id)")
    @TableField("create_emp_id")
    private Long empId;

    @Schema(description = "所属部门 dept_id")
    @TableField("dept_id")
    private Long deptId;

    @Schema(description = "流程实例 ID")
    @TableField("wf_instance_id")
    private Long wfInstanceId;
}
