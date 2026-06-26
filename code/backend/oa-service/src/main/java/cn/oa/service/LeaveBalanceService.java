package cn.oa.service;

import cn.oa.entity.OaLeaveBalance;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

public interface LeaveBalanceService extends IService<OaLeaveBalance> {

    IPage<OaLeaveBalance> pageList(int pageNum, int pageSize, Long empId, Integer year, String searchKey);

    List<OaLeaveBalance> myBalances(Long empId);

    void initYearBalance(Long empId, Integer year);

    void deductBalance(Long empId, Integer leaveType, Integer year, BigDecimal days);

    void restoreBalance(Long empId, Integer leaveType, Integer year, BigDecimal days);

    void addCompensatoryBalance(Long empId, Integer year, BigDecimal days);

    void assertSufficientBalance(Long empId, Integer leaveType, Integer year, BigDecimal days);
}
