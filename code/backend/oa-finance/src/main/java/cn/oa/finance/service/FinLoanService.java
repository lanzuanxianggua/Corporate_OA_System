package cn.oa.finance.service;

import cn.oa.finance.dto.FinLoanCreateDTO;
import cn.oa.finance.dto.FinLoanRepayDTO;
import cn.oa.finance.vo.FinLoanVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.math.BigDecimal;

/**
 * 借款服务接口
 *
 * @author oa-finance
 */
public interface FinLoanService {

    /**
     * 创建借款申请
     *
     * @param dto   创建DTO
     * @param empId 员工ID
     * @return 借款ID
     */
    Long createLoan(FinLoanCreateDTO dto, Long empId);

    /**
     * 提交审批
     *
     * @param id    借款ID
     * @param empId 员工ID
     */
    void submitToWorkflow(Long id, Long empId);

    /**
     * 还款
     *
     * @param loanId 借款ID
     * @param dto    还款DTO
     * @param empId  员工ID
     */
    void repay(Long loanId, FinLoanRepayDTO dto, Long empId);

    /**
     * 分页查询
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param empId    当前用户ID
     * @param isAdmin  是否管理员
     * @param status   状态筛选
     * @return 分页结果
     */
    IPage<FinLoanVO> pageQuery(Integer pageNum, Integer pageSize, Long empId, boolean isAdmin, String status);

    /**
     * 查询详情
     *
     * @param id 借款ID
     * @return 借款VO
     */
    FinLoanVO getDetail(Long id);

    /**
     * 审批通过回调
     *
     * @param id 借款ID
     */
    void onWorkflowApproved(Long id);

    /**
     * 审批驳回回调
     *
     * @param id           借款ID
     * @param rejectReason 驳回原因
     */
    void onWorkflowRejected(Long id, String rejectReason);

    /**
     * 审批撤回回调
     *
     * @param id 借款ID
     */
    void onWorkflowWithdrawn(Long id);
}
