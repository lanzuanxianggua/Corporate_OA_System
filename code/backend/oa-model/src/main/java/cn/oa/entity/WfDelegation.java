package cn.oa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("wf_delegation")
public class WfDelegation {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long delegatorId;
    @TableField("delegate_id")
    private Long delegateToId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status; // 0=active, 1=cancelled
    private LocalDateTime createTime;
}
