package cn.oa.finance.entity;

import cn.oa.platform.common.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_contract")
public class FinContract extends BaseEntity {
    private String contractNo;
    private String contractName;
    private String counterparty;
    private BigDecimal amount;
    private LocalDate signedDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long ownerEmpId;
    private Long deptId;
    private String status;
    private String remark;
}
