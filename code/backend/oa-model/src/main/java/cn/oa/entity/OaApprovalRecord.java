package cn.oa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("oa_approval_record")
public class OaApprovalRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long applyId;

    private Long approverId;

    private Integer approveStatus;

    private String remark;

    private LocalDateTime approveTime;
}
