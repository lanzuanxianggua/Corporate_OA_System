package cn.oa.finance.service;

import cn.oa.finance.dto.FinExpenseCreateDTO;
import cn.oa.finance.dto.FinExpenseQueryDTO;
import cn.oa.finance.vo.FinExpenseVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 费用报销服务接口
 *
 * @author oa-finance
 */
public interface FinExpenseService {

    /**
     * 创建报销单
     *
     * @param dto   创建DTO
     * @param empId 员工ID
     * @return 报销单ID
     */
    Long createExpense(FinExpenseCreateDTO dto, Long empId);

    /**
     * 提交审批
     *
     * @param id    报销单ID
     * @param empId 员工ID
     */
    void submitToWorkflow(Long id, Long empId);

    /**
     * 撤回报销单
     *
     * @param id     报销单ID
     * @param empId  当前用户ID
     * @param isAdmin 是否管理员
     */
    void revoke(Long id, Long empId, boolean isAdmin);

    /**
     * 分页查询
     *
     * @param query   查询条件
     * @param empId   当前用户ID
     * @param isAdmin 是否管理员
     * @return 分页结果
     */
    IPage<FinExpenseVO> pageQuery(FinExpenseQueryDTO query, Long empId, boolean isAdmin);

    /**
     * 查询详情
     *
     * @param id 报销单ID
     * @return 报销VO
     */
    FinExpenseVO getDetail(Long id);

    /**
     * 审批通过回调
     *
     * @param id 报销单ID
     */
    void onWorkflowApproved(Long id);

    /**
     * 审批驳回回调
     *
     * @param id           报销单ID
     * @param rejectReason 驳回原因
     */
    void onWorkflowRejected(Long id, String rejectReason);

    /**
     * 审批撤回回调
     *
     * @param id 报销单ID
     */
    void onWorkflowWithdrawn(Long id);
}
