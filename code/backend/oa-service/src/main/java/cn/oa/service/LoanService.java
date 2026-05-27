package cn.oa.service;

import cn.oa.entity.OaLoan;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface LoanService extends IService<OaLoan> {

    void submit(OaLoan loan);

    void approve(Long loanId, Long approverId, Integer status, String remark);

    IPage<OaLoan> pageList(int pageNum, int pageSize, Long empId, Integer status);

    void addRepayment(Long loanId, java.math.BigDecimal amount, String remark);

    void updateStatus(Long id, Integer status);
}