package cn.oa.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("oa_expense")
public class OaExpense {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long empId;

    private String title;

    private BigDecimal amount;

    private String category;

    private String description;

    private Integer status = 0;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private String delFlag;

    private String createBy;

    private String updateBy;

    @TableField(exist = false)
    private String empName;

    @TableField(exist = false)
    private String remark;
}
