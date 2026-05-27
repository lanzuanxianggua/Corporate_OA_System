package cn.oa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wf_cc_record")
public class WfCcRecord {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long instanceId;
    private Long taskId;
    private Long ccEmpId;
    private String status; // 0=unread, 1=read
    private LocalDateTime createTime;
}
