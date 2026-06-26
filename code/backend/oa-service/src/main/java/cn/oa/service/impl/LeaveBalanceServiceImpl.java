package cn.oa.service.impl;

import cn.oa.common.constant.LeaveType;
import cn.oa.common.exception.BusinessException;
import cn.oa.entity.OaLeaveBalance;
import cn.oa.mapper.OaLeaveBalanceMapper;
import cn.oa.service.LeaveBalanceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class LeaveBalanceServiceImpl extends ServiceImpl<OaLeaveBalanceMapper, OaLeaveBalance> implements LeaveBalanceService {

    @Override
    public IPage<OaLeaveBalance> pageList(int pageNum, int pageSize, Long empId, Integer year, String searchKey) {
        Page<OaLeaveBalance> page = new Page<>(pageNum, pageSize);
        return baseMapper.pageWithEmpInfo(page, empId, year, searchKey);
    }

    @Override
    public List<OaLeaveBalance> myBalances(Long empId) {
        return this.list(new LambdaQueryWrapper<OaLeaveBalance>()
                .eq(OaLeaveBalance::getEmpId, empId)
                .orderByDesc(OaLeaveBalance::getYear));
    }

    @Override
    @Transactional
    public void initYearBalance(Long empId, Integer year) {
        int[] leaveTypes = {LeaveType.ANNUAL, LeaveType.PERSONAL, LeaveType.SICK, LeaveType.MARRIAGE, LeaveType.MATERNITY, LeaveType.COMPENSATORY};
        BigDecimal[] defaults = {
                new BigDecimal("10"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("10"),
                new BigDecimal("158"),
                BigDecimal.ZERO
        };
        for (int i = 0; i < leaveTypes.length; i++) {
            int leaveType = leaveTypes[i];
            BigDecimal total = defaults[i];
            OaLeaveBalance existing = this.getOne(new LambdaQueryWrapper<OaLeaveBalance>()
                    .eq(OaLeaveBalance::getEmpId, empId)
                    .eq(OaLeaveBalance::getLeaveType, leaveType)
                    .eq(OaLeaveBalance::getYear, year));
            if (existing != null) {
                continue;
            }
            OaLeaveBalance balance = new OaLeaveBalance();
            balance.setEmpId(empId);
            balance.setLeaveType(leaveType);
            balance.setYear(year);
            balance.setTotalDays(total);
            balance.setUsedDays(BigDecimal.ZERO);
            balance.setRemainingDays(total);
            this.save(balance);
        }
    }

    @Override
    @Transactional
    public void deductBalance(Long empId, Integer leaveType, Integer year, BigDecimal days) {
        boolean strictBalance = Integer.valueOf(LeaveType.ANNUAL).equals(leaveType)
                || Integer.valueOf(LeaveType.COMPENSATORY).equals(leaveType);

        if (strictBalance) {
            boolean updated = this.lambdaUpdate()
                    .setSql("used_days = used_days + " + days)
                    .setSql("remaining_days = remaining_days - " + days)
                    .eq(OaLeaveBalance::getEmpId, empId)
                    .eq(OaLeaveBalance::getLeaveType, leaveType)
                    .eq(OaLeaveBalance::getYear, year)
                    .ge(OaLeaveBalance::getRemainingDays, days)
                    .update();
            if (!updated) {
                boolean exists = this.exists(new LambdaQueryWrapper<OaLeaveBalance>()
                        .eq(OaLeaveBalance::getEmpId, empId)
                        .eq(OaLeaveBalance::getLeaveType, leaveType)
                        .eq(OaLeaveBalance::getYear, year));
                if (!exists) {
                    throw new BusinessException(LeaveType.text(leaveType) + "余额不存在");
                }
                throw new BusinessException(LeaveType.text(leaveType) + "余额不足");
            }
            return;
        }

        boolean updated = this.lambdaUpdate()
                .setSql("used_days = used_days + " + days)
                .setSql("remaining_days = remaining_days - " + days)
                .eq(OaLeaveBalance::getEmpId, empId)
                .eq(OaLeaveBalance::getLeaveType, leaveType)
                .eq(OaLeaveBalance::getYear, year)
                .update();
        if (!updated) {
            OaLeaveBalance balance = new OaLeaveBalance();
            balance.setEmpId(empId);
            balance.setLeaveType(leaveType);
            balance.setYear(year);
            balance.setTotalDays(BigDecimal.ZERO);
            balance.setUsedDays(days);
            balance.setRemainingDays(days.negate());
            this.save(balance);
        }
    }

    @Override
    @Transactional
    public void restoreBalance(Long empId, Integer leaveType, Integer year, BigDecimal days) {
        boolean updated = this.lambdaUpdate()
                .setSql("used_days = used_days - " + days)
                .setSql("remaining_days = remaining_days + " + days)
                .eq(OaLeaveBalance::getEmpId, empId)
                .eq(OaLeaveBalance::getLeaveType, leaveType)
                .eq(OaLeaveBalance::getYear, year)
                .update();
        if (!updated) {
            throw new BusinessException("假期余额不存在，无法归还");
        }
    }

    @Override
    @Transactional
    public void addCompensatoryBalance(Long empId, Integer year, BigDecimal days) {
        boolean updated = this.lambdaUpdate()
                .setSql("total_days = total_days + " + days)
                .setSql("remaining_days = remaining_days + " + days)
                .eq(OaLeaveBalance::getEmpId, empId)
                .eq(OaLeaveBalance::getLeaveType, LeaveType.COMPENSATORY)
                .eq(OaLeaveBalance::getYear, year)
                .update();
        if (!updated) {
            OaLeaveBalance balance = new OaLeaveBalance();
            balance.setEmpId(empId);
            balance.setLeaveType(LeaveType.COMPENSATORY);
            balance.setYear(year);
            balance.setTotalDays(days.max(BigDecimal.ZERO));
            balance.setUsedDays(BigDecimal.ZERO);
            balance.setRemainingDays(days);
            this.save(balance);
        }
    }

    @Override
    public void assertSufficientBalance(Long empId, Integer leaveType, Integer year, BigDecimal days) {
        if (days == null || days.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        OaLeaveBalance balance = this.getOne(new LambdaQueryWrapper<OaLeaveBalance>()
                .eq(OaLeaveBalance::getEmpId, empId)
                .eq(OaLeaveBalance::getLeaveType, leaveType)
                .eq(OaLeaveBalance::getYear, year));
        if (balance == null || balance.getRemainingDays() == null || balance.getRemainingDays().compareTo(days) < 0) {
            throw new BusinessException(LeaveType.text(leaveType) + "余额不足");
        }
    }
}
