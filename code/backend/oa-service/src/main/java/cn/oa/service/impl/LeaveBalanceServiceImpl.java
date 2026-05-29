package cn.oa.service.impl;

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
        // Default leave types: 0-annual, 1-sick, 2-personal, 3-marriage, 4-maternity
        BigDecimal[] defaults = {new BigDecimal("10"), new BigDecimal("15"), new BigDecimal("5"), new BigDecimal("10"), new BigDecimal("158")};
        for (int i = 0; i < defaults.length; i++) {
            OaLeaveBalance existing = this.getOne(new LambdaQueryWrapper<OaLeaveBalance>()
                    .eq(OaLeaveBalance::getEmpId, empId)
                    .eq(OaLeaveBalance::getLeaveType, i)
                    .eq(OaLeaveBalance::getYear, year));
            if (existing != null) {
                continue;
            }
            OaLeaveBalance balance = new OaLeaveBalance();
            balance.setEmpId(empId);
            balance.setLeaveType(i);
            balance.setYear(year);
            balance.setTotalDays(defaults[i]);
            balance.setUsedDays(BigDecimal.ZERO);
            balance.setRemainingDays(defaults[i]);
            this.save(balance);
        }
    }

    @Override
    @Transactional
    public void deductBalance(Long empId, Integer leaveType, Integer year, BigDecimal days) {
        OaLeaveBalance balance = this.getOne(new LambdaQueryWrapper<OaLeaveBalance>()
                .eq(OaLeaveBalance::getEmpId, empId)
                .eq(OaLeaveBalance::getLeaveType, leaveType)
                .eq(OaLeaveBalance::getYear, year));
        if (balance == null) {
            throw new BusinessException("假期余额不存在");
        }
        if (balance.getRemainingDays().compareTo(days) < 0) {
            throw new BusinessException("假期余额不足");
        }
        balance.setUsedDays(balance.getUsedDays().add(days));
        balance.setRemainingDays(balance.getRemainingDays().subtract(days));
        this.updateById(balance);
    }

    @Override
    @Transactional
    public void restoreBalance(Long empId, Integer leaveType, Integer year, BigDecimal days) {
        OaLeaveBalance balance = this.getOne(new LambdaQueryWrapper<OaLeaveBalance>()
                .eq(OaLeaveBalance::getEmpId, empId)
                .eq(OaLeaveBalance::getLeaveType, leaveType)
                .eq(OaLeaveBalance::getYear, year));
        if (balance == null) {
            throw new BusinessException("假期余额不存在，无法归还");
        }
        // Reverse of deduct: decrement usedDays, increment remainingDays
        balance.setUsedDays(balance.getUsedDays().subtract(days));
        balance.setRemainingDays(balance.getRemainingDays().add(days));
        this.updateById(balance);
    }

    @Override
    @Transactional
    public void addCompensatoryBalance(Long empId, Integer year, BigDecimal days) {
        // leaveType=5 is compensatory leave (调休)
        OaLeaveBalance balance = this.getOne(new LambdaQueryWrapper<OaLeaveBalance>()
                .eq(OaLeaveBalance::getEmpId, empId)
                .eq(OaLeaveBalance::getLeaveType, 5)
                .eq(OaLeaveBalance::getYear, year));
        if (balance == null) {
            // Create new compensatory leave balance
            balance = new OaLeaveBalance();
            balance.setEmpId(empId);
            balance.setLeaveType(5);
            balance.setYear(year);
            balance.setTotalDays(days);
            balance.setUsedDays(BigDecimal.ZERO);
            balance.setRemainingDays(days);
            this.save(balance);
        } else {
            balance.setTotalDays(balance.getTotalDays().add(days));
            balance.setRemainingDays(balance.getRemainingDays().add(days));
            this.updateById(balance);
        }
    }
}
