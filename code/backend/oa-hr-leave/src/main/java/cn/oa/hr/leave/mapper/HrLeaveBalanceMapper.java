package cn.oa.hr.leave.mapper;

import cn.oa.hr.leave.entity.HrLeaveBalance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface HrLeaveBalanceMapper extends BaseMapper<HrLeaveBalance> {

    @Select("SELECT * FROM hr_leave_balance WHERE del_flag = '0' AND emp_id = #{empId} AND leave_type = #{leaveType} AND year = #{year}")
    HrLeaveBalance findByEmpAndTypeAndYear(@Param("empId") Long empId, @Param("leaveType") String leaveType, @Param("year") int year);

    @Select("SELECT * FROM hr_leave_balance WHERE del_flag = '0' AND emp_id = #{empId} AND status = 'ACTIVE' ORDER BY leave_type, year DESC")
    List<HrLeaveBalance> findAllByEmpId(@Param("empId") Long empId);
}
