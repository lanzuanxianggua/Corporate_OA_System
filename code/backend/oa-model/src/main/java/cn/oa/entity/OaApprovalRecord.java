package cn.oa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("oa_approval_record")
public class OaApprovalRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long applyId;

    private String businessType;

    private Long approverId;

    private Integer approveStatus;

    private String remark;

    private LocalDateTime approveTime;

    private Long taskId;

    private String nodeName;

    /** 审批人姓名（非数据库字段，查询时填充） */
    @TableField(exist = false)
    private String assigneeName;
}
