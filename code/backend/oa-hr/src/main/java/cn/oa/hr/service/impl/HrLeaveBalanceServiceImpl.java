package cn.oa.hr.service.impl;

import cn.oa.hr.dto.HrLeaveBalanceAdjustDTO;
import cn.oa.hr.dto.HrLeaveBalanceInitDTO;
import cn.oa.hr.entity.HrLeaveBalance;
import cn.oa.hr.enums.HrLeaveType;
import cn.oa.hr.mapper.HrLeaveBalanceMapper;
import cn.oa.hr.service.HrLeaveBalanceService;
import cn.oa.hr.vo.HrLeaveBalanceVO;
import cn.oa.platform.core.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * HR假期余额服务实现
 *
 * @author oa-hr
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrLeaveBalanceServiceImpl implements HrLeaveBalanceService {

    private final HrLeaveBalanceMapper balanceMapper;

    @Override
    public List<HrLeaveBalanceVO> getMyBalances(Long empId, Integer year) {
        LambdaQueryWrapper<HrLeaveBalance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HrLeaveBalance::getEmpId, empId)
                .eq(HrLeaveBalance::getStatus, "ACTIVE");
        if (year != null) {
            wrapper.eq(HrLeaveBalance::getYear, year);
        }
        wrapper.orderByDesc(HrLeaveBalance::getYear);

        List<HrLeaveBalance> balances = balanceMapper.selectList(wrapper);
        return balances.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public IPage<HrLeaveBalanceVO> pageQuery(Long empId, Integer year, int pageNum, int pageSize) {
        LambdaQueryWrapper<HrLeaveBalance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HrLeaveBalance::getStatus, "ACTIVE");
        if (empId != null) {
            wrapper.eq(HrLeaveBalance::getEmpId, empId);
        }
        if (year != null) {
            wrapper.eq(HrLeaveBalance::getYear, year);
        }
        wrapper.orderByDesc(HrLeaveBalance::getYear);

        IPage<HrLeaveBalance> page = balanceMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return page.convert(this::toVO);
    }

    @Override
    @Transactional
    public void initBalance(HrLeaveBalanceInitDTO dto) {
        // 检查是否已存在
        HrLeaveBalance existing = getBalance(dto.getEmpId(), dto.getLeaveType(), dto.getYear());
        if (existing != null) {
            throw new BusinessException("该员工年度假期余额已存在");
        }

        HrLeaveBalance balance = new HrLeaveBalance();
        balance.setEmpId(dto.getEmpId());
        balance.setLeaveType(dto.getLeaveType());
        balance.setYear(dto.getYear());
        balance.setTotalDays(dto.getTotalDays());
        balance.setUsedDays(BigDecimal.ZERO);
        balance.setFrozenDays(BigDecimal.ZERO);
        balance.setRemainingDays(dto.getTotalDays());
        balance.setStatus("ACTIVE");

        if (dto.getExpireDate() != null) {
            balance.setExpireDate(LocalDate.parse(dto.getExpireDate()));
        }

        balanceMapper.insert(balance);
        log.info("Initialized leave balance: empId={}, type={}, year={}, total={}",
                dto.getEmpId(), dto.getLeaveType(), dto.getYear(), dto.getTotalDays());
    }

    @Override
    @Transactional
    public void adjustBalance(HrLeaveBalanceAdjustDTO dto) {
        HrLeaveBalance balance = balanceMapper.selectById(dto.getId());
        if (balance == null) {
            throw new BusinessException("假期余额不存在");
        }

        BigDecimal newTotal;
        switch (dto.getAdjustType()) {
            case "ADD":
                newTotal = balance.getTotalDays().add(dto.getAdjustDays());
                balance.setRemainingDays(balance.getRemainingDays().add(dto.getAdjustDays()));
                break;
            case "SUB":
                if (balance.getRemainingDays().compareTo(dto.getAdjustDays()) < 0) {
                    throw new BusinessException("余额不足，无法扣减");
                }
                newTotal = balance.getTotalDays().subtract(dto.getAdjustDays());
                balance.setRemainingDays(balance.getRemainingDays().subtract(dto.getAdjustDays()));
                break;
            case "SET":
                newTotal = dto.getAdjustDays();
                balance.setRemainingDays(dto.getAdjustDays().subtract(balance.getUsedDays()).subtract(balance.getFrozenDays()));
                break;
            default:
                throw new BusinessException("无效的调整类型: " + dto.getAdjustType());
        }

        if (newTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("调整后总额不能为负数");
        }
        if (balance.getRemainingDays().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("调整后剩余余额不能为负数");
        }

        balance.setTotalDays(newTotal);
        balanceMapper.updateById(balance);
        log.info("Adjusted leave balance: id={}, type={}, days={}, reason={}",
                dto.getId(), dto.getAdjustType(), dto.getAdjustDays(), dto.getReason());
    }

    @Override
    @Transactional
    public boolean freezeBalance(Long empId, String leaveType, Integer year, BigDecimal days) {
        // 使用原子SQL进行冻结：只增加frozenDays
        // 条件：remainingDays - frozenDays >= days（可用余额充足）
        int updated = balanceMapper.freezeBalance(empId, leaveType, year, days);
        if (updated > 0) {
            log.info("Frozen balance: empId={}, type={}, year={}, days={}", empId, leaveType, year, days);
            return true;
        }
        log.warn("Failed to freeze balance: empId={}, type={}, year={}, days={}", empId, leaveType, year, days);
        return false;
    }

    @Override
    @Transactional
    public boolean confirmBalance(Long empId, String leaveType, Integer year, BigDecimal days) {
        // 使用原子SQL确认余额：
        // frozenDays减少，usedDays增加，remainingDays减少
        // remainingDays = totalDays - usedDays，所以usedDays增加时remainingDays必须减少
        int updated = balanceMapper.confirmBalance(empId, leaveType, year, days);
        if (updated > 0) {
            log.info("Confirmed balance: empId={}, type={}, year={}, days={}", empId, leaveType, year, days);
            return true;
        }
        log.warn("Failed to confirm balance: empId={}, type={}, year={}, days={}", empId, leaveType, year, days);
        return false;
    }

    @Override
    @Transactional
    public boolean releaseFrozenBalance(Long empId, String leaveType, Integer year, BigDecimal days) {
        // 使用原子SQL释放冻结：只减少frozenDays
        // remainingDays = totalDays - usedDays，驳回/撤回时usedDays没变，所以remainingDays不变
        int updated = balanceMapper.releaseFrozenBalance(empId, leaveType, year, days);
        if (updated > 0) {
            log.info("Released frozen balance: empId={}, type={}, year={}, days={}", empId, leaveType, year, days);
            return true;
        }
        log.warn("Failed to release frozen balance: empId={}, type={}, year={}, days={}", empId, leaveType, year, days);
        return false;
    }

    @Override
    public HrLeaveBalance getBalance(Long empId, String leaveType, Integer year) {
        return balanceMapper.selectOne(new LambdaQueryWrapper<HrLeaveBalance>()
                .eq(HrLeaveBalance::getEmpId, empId)
                .eq(HrLeaveBalance::getLeaveType, leaveType)
                .eq(HrLeaveBalance::getYear, year)
                .eq(HrLeaveBalance::getStatus, "ACTIVE"));
    }

    @Override
    public boolean hasEnoughBalance(Long empId, String leaveType, Integer year, BigDecimal days) {
        HrLeaveBalance balance = getBalance(empId, leaveType, year);
        if (balance == null) {
            return false;
        }
        // 可用余额 = remainingDays - frozenDays
        BigDecimal available = balance.getRemainingDays().subtract(
                balance.getFrozenDays() != null ? balance.getFrozenDays() : BigDecimal.ZERO);
        return available.compareTo(days) >= 0;
    }

    private HrLeaveBalanceVO toVO(HrLeaveBalance balance) {
        HrLeaveBalanceVO vo = new HrLeaveBalanceVO();
        vo.setId(balance.getId());
        vo.setEmpId(balance.getEmpId());
        vo.setLeaveType(balance.getLeaveType());
        vo.setLeaveTypeName(getLeaveTypeName(balance.getLeaveType()));
        vo.setYear(balance.getYear());
        vo.setTotalDays(balance.getTotalDays());
        vo.setUsedDays(balance.getUsedDays() != null ? balance.getUsedDays() : BigDecimal.ZERO);
        vo.setFrozenDays(balance.getFrozenDays() != null ? balance.getFrozenDays() : BigDecimal.ZERO);
        vo.setRemainingDays(balance.getRemainingDays());
        vo.setExpireDate(balance.getExpireDate());
        vo.setStatus(balance.getStatus());
        vo.setUpdateTime(balance.getUpdateTime());

        // 计算可用余额
        BigDecimal frozen = vo.getFrozenDays();
        vo.setAvailableDays(balance.getRemainingDays().subtract(frozen));

        return vo;
    }

    private String getLeaveTypeName(String leaveType) {
        HrLeaveType type = HrLeaveType.fromCode(leaveType);
        return type != null ? type.getName() : leaveType;
    }
}
