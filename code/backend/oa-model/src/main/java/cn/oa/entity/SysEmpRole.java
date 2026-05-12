package cn.oa.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_emp_role")
public class SysEmpRole {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long empId;

    private Long roleId;
}
