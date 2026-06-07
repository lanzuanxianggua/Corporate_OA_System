package cn.oa.admin.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 印章使用申请.
 *
 * <p>对应表 adm_seal_applys.
 * 状态: DRAFT/PENDING/APPROVED/REJECTED/USED/ARCHIVED
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("adm_seal_applys")
@Schema(description = "印章使用申请")
public class AdmSealApply extends BaseEntity {

    @Schema(description = "申请单号")
    @TableField("apply_no")
    private String applyNo;

    @Schema(description = "印章 ID")
    @TableField("seal_id")
    private Long sealId;

    @Schema(description = "申请人 emp_id")
    @TableField("emp_id")
    private Long empId;

    @Schema(description = "申请用途")
    @TableField("purpose")
    private String purpose;

    @Schema(description = "用印文件名称")
    @TableField("doc_name")
    private String docName;

    @Schema(description = "文件份数")
    @TableField("doc_count")
    private Integer docCount;

    @Schema(description = "期望用印日期")
    @TableField("expect_date")
    private LocalDate expectDate;

    @Schema(description = "实际用印日期")
    @TableField("use_date")
    private LocalDate useDate;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;

    @Schema(description = "状态: DRAFT/PENDING/APPROVED/REJECTED/USED/ARCHIVED")
    @TableField("status")
    private String status;

    @Schema(description = "流程实例 ID")
    @TableField("wf_instance_id")
    private Long wfInstanceId;
}
