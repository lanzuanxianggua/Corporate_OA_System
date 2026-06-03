package cn.oa.hr.mapper;

import cn.oa.hr.entity.HrLeaveBalance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * HR假期余额Mapper
 *
 * 余额语义：
 * - remainingDays = 账面剩余余额 = totalDays - usedDays（包含冻结部分）
 * - frozenDays = 审批中冻结天数
 * - availableDays = remainingDays - frozenDays（可申请新请假）
 *
 * @author oa-hr
 */
@Mapper
public interface HrLeaveBalanceMapper extends BaseMapper<HrLeaveBalance> {

    /**
     * 冻结余额（原子操作）
     * 只增加 frozenDays，不改变 remainingDays
     * 条件：remainingDays - frozenDays >= days（可用余额充足）
     *
     * @param empId     员工ID
     * @param leaveType 假期类型
     * @param year      年度
     * @param days      冻结天数
     * @return 影响行数
     */
    @Update("UPDATE hr_leave_balance " +
            "SET frozen_days = frozen_days + #{days}, " +
            "    update_time = NOW() " +
            "WHERE emp_id = #{empId} " +
            "  AND leave_type = #{leaveType} " +
            "  AND year = #{year} " +
            "  AND status = 'ACTIVE' " +
            "  AND del_flag = '0' " +
            "  AND remaining_days - frozen_days >= #{days}")
    int freezeBalance(@Param("empId") Long empId,
                      @Param("leaveType") String leaveType,
                      @Param("year") Integer year,
                      @Param("days") BigDecimal days);

    /**
     * 确认余额（原子操作）
     * 审批通过：frozenDays减少，usedDays增加，remainingDays减少
     * remainingDays = totalDays - usedDays，所以 usedDays 增加 → remainingDays 减少
     *
     * @param empId     员工ID
     * @param leaveType 假期类型
     * @param year      年度
     * @param days      确认天数
     * @return 影响行数
     */
    @Update("UPDATE hr_leave_balance " +
            "SET used_days = used_days + #{days}, " +
            "    frozen_days = frozen_days - #{days}, " +
            "    remaining_days = remaining_days - #{days}, " +
            "    update_time = NOW() " +
            "WHERE emp_id = #{empId} " +
            "  AND leave_type = #{leaveType} " +
            "  AND year = #{year} " +
            "  AND status = 'ACTIVE' " +
            "  AND del_flag = '0' " +
            "  AND frozen_days >= #{days}")
    int confirmBalance(@Param("empId") Long empId,
                       @Param("leaveType") String leaveType,
                       @Param("year") Integer year,
                       @Param("days") BigDecimal days);

    /**
     * 释放冻结余额（原子操作）
     * 驳回/撤回：只减少 frozenDays，不改变 remainingDays
     * 因为 remainingDays = totalDays - usedDays，usedDays 没变所以 remainingDays 不变
     *
     * @param empId     员工ID
     * @param leaveType 假期类型
     * @param year      年度
     * @param days      释放天数
     * @return 影响行数
     */
    @Update("UPDATE hr_leave_balance " +
            "SET frozen_days = frozen_days - #{days}, " +
            "    update_time = NOW() " +
            "WHERE emp_id = #{empId} " +
            "  AND leave_type = #{leaveType} " +
            "  AND year = #{year} " +
            "  AND status = 'ACTIVE' " +
            "  AND del_flag = '0' " +
            "  AND frozen_days >= #{days}")
    int releaseFrozenBalance(@Param("empId") Long empId,
                             @Param("leaveType") String leaveType,
                             @Param("year") Integer year,
                             @Param("days") BigDecimal days);
}
