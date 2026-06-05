package cn.oa.hr.leave.service;

import cn.oa.hr.leave.entity.HrLeaveBalance;
import cn.oa.hr.leave.mapper.HrLeaveBalanceMapper;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 假期余额 Service.
 *
 * <p>管理员工各类假期的额度、冻结、扣减、归还.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrLeaveBalanceService {

    private final HrLeaveBalanceMapper balanceMapper;

    /**
     * 查询余额.
     *
     * @return 余额记录, 不存在返回 null
     */
    public HrLeaveBalance getBalance(Long empId, String leaveType, int year) {
        return balanceMapper.findByEmpAndTypeAndYear(empId, leaveType, year);
    }

    /**
     * 初始化余额记录.
     */
    @Transactional
    public void initBalance(Long empId, String leaveType, int year, BigDecimal totalDays) {
        HrLeaveBalance existing = balanceMapper.findByEmpAndTypeAndYear(empId, leaveType, year);
        if (existing != null) {
            throw new BizException(RCode.BAD_REQUEST, "余额记录已存在: empId=" + empId + ", type=" + leaveType + ", year=" + year);
        }
        HrLeaveBalance balance = new HrLeaveBalance();
        balance.setEmpId(empId);
        balance.setLeaveType(leaveType);
        balance.setYear(year);
        balance.setTotalDays(totalDays);
        balance.setUsedDays(BigDecimal.ZERO);
        balance.setFrozenDays(BigDecimal.ZERO);
        balance.setRemainingDays(totalDays);
        balance.setStatus("ACTIVE");
        balance.setCreateBy(String.valueOf(empId));
        balanceMapper.insert(balance);
        log.info("初始化假期余额: empId={}, type={}, year={}, totalDays={}", empId, leaveType, year, totalDays);
    }

    /**
     * 调整余额 (管理员操作).
     */
    @Transactional
    public void adjustBalance(Long id, BigDecimal adjustDays, String reason) {
        HrLeaveBalance balance = balanceMapper.selectById(id);
        if (balance == null) {
            throw new BizException(RCode.NOT_FOUND, "余额记录不存在: " + id);
        }
        BigDecimal newTotal = balance.getTotalDays().add(adjustDays);
        if (newTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException(RCode.BAD_REQUEST, "调整后总额度不能为负数");
        }
        BigDecimal newRemaining = balance.getRemainingDays().add(adjustDays);
        if (newRemaining.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException(RCode.BAD_REQUEST, "调整后剩余天数不能为负数");
        }
        balance.setTotalDays(newTotal);
        balance.setRemainingDays(newRemaining);
        balanceMapper.updateById(balance);
        log.info("调整假期余额: id={}, adjustDays={}, reason={}, newTotal={}, newRemaining={}",
                id, adjustDays, reason, newTotal, newRemaining);
    }

    /**
     * 审批通过后扣减余额.
     * <p>将冻结天数转为已用天数.
     */
    @Transactional
    public void deductOnApprove(Long empId, String leaveType, int year, BigDecimal days) {
        HrLeaveBalance balance = balanceMapper.findByEmpAndTypeAndYear(empId, leaveType, year);
        if (balance == null) {
            throw new BizException(RCode.NOT_FOUND, "余额记录不存在: empId=" + empId + ", type=" + leaveType + ", year=" + year);
        }
        // 冻结 -> 已用
        BigDecimal newFrozen = balance.getFrozenDays().subtract(days);
        if (newFrozen.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("冻结天数不足: empId={}, type={}, frozen={}, deduct={}", empId, leaveType, balance.getFrozenDays(), days);
            newFrozen = BigDecimal.ZERO;
        }
        BigDecimal newUsed = balance.getUsedDays().add(days);
        balance.setFrozenDays(newFrozen);
        balance.setUsedDays(newUsed);
        balanceMapper.updateById(balance);
        log.info("审批通过扣减余额: empId={}, type={}, days={}, used={}, frozen={}",
                empId, leaveType, days, newUsed, newFrozen);
    }

    /**
     * 提交请假时冻结余额.
     */
    @Transactional
    public void freezeOnSubmit(Long empId, String leaveType, int year, BigDecimal days) {
        HrLeaveBalance balance = balanceMapper.findByEmpAndTypeAndYear(empId, leaveType, year);
        if (balance == null) {
            throw new BizException(RCode.NOT_FOUND, "余额记录不存在, 请先初始化: empId=" + empId + ", type=" + leaveType);
        }
        if (balance.getRemainingDays().compareTo(days) < 0) {
            throw new BizException(RCode.BAD_REQUEST, "余额不足: remaining=" + balance.getRemainingDays() + ", required=" + days);
        }
        BigDecimal newFrozen = balance.getFrozenDays().add(days);
        BigDecimal newRemaining = balance.getRemainingDays().subtract(days);
        balance.setFrozenDays(newFrozen);
        balance.setRemainingDays(newRemaining);
        balanceMapper.updateById(balance);
        log.info("冻结余额: empId={}, type={}, days={}, frozen={}, remaining={}",
                empId, leaveType, days, newFrozen, newRemaining);
    }

    /**
     * 审批拒绝或撤回时解冻余额.
     */
    @Transactional
    public void unfreezeOnReject(Long empId, String leaveType, int year, BigDecimal days) {
        HrLeaveBalance balance = balanceMapper.findByEmpAndTypeAndYear(empId, leaveType, year);
        if (balance == null) {
            log.warn("解冻余额时记录不存在: empId={}, type={}, year={}", empId, leaveType, year);
            return;
        }
        BigDecimal newFrozen = balance.getFrozenDays().subtract(days);
        if (newFrozen.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("冻结天数为负, 重置为0: empId={}, type={}", empId, leaveType);
            newFrozen = BigDecimal.ZERO;
        }
        BigDecimal newRemaining = balance.getRemainingDays().add(days);
        balance.setFrozenDays(newFrozen);
        balance.setRemainingDays(newRemaining);
        balanceMapper.updateById(balance);
        log.info("解冻余额: empId={}, type={}, days={}, frozen={}, remaining={}",
                empId, leaveType, days, newFrozen, newRemaining);
    }
}
