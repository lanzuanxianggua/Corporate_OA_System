package cn.oa.document.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 签报审批记录明细.
 *
 * <p>对应表 doc_sign_report_items.
 * 每条记录: 审批人 + 审批意见 + 审批顺序 + 状态
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_sign_report_items")
@Schema(description = "签报审批记录")
public class DocSignReportItem extends BaseEntity {

    @Schema(description = "签报 ID")
    @TableField("report_id")
    private Long reportId;

    @Schema(description = "审批意见")
    @TableField("opinion")
    private String opinion;

    @Schema(description = "审批人 emp_id")
    @TableField("approver_id")
    private Long approverId;

    @Schema(description = "审批顺序 (1-based)")
    @TableField("approve_order")
    private Integer approveOrder;

    @Schema(description = "状态: PENDING/APPROVED/REJECTED")
    @TableField("status")
    private String status;
}
