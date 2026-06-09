package cn.oa.admin.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("adm_supply_request")
public class AdmSupplyRequest extends BaseEntity {
    private String requestNo;
    private String requestType;
    private Long empId;
    private Long deptId;
    private String reason;
    private String status;
    private LocalDateTime approveTime;
    private String rejectReason;
}
