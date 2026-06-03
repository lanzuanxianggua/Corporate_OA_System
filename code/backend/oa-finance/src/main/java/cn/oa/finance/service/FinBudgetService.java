package cn.oa.finance.service;

import cn.oa.finance.dto.FinBudgetCreateDTO;
import cn.oa.finance.dto.FinBudgetQueryDTO;
import cn.oa.finance.dto.FinBudgetUpdateDTO;
import cn.oa.finance.vo.FinBudgetVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.math.BigDecimal;

/**
 * 预算服务接口
 *
 * @author oa-finance
 */
public interface FinBudgetService {

    /**
     * 创建预算
     *
     * @param dto   预算创建DTO
     * @return 预算ID
     */
    Long createBudget(FinBudgetCreateDTO dto);

    /**
     * 更新预算
     *
     * @param dto 预算更新DTO
     */
    void updateBudget(FinBudgetUpdateDTO dto);

    /**
     * 分页查询预算
     *
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<FinBudgetVO> pageQuery(FinBudgetQueryDTO query);

    /**
     * 查询预算详情
     *
     * @param id 预算ID
     * @return 预算VO
     */
    FinBudgetVO getDetail(Long id);

    /**
     * 占用预算（乐观锁CAS重试）
     *
     * @param deptId   部门ID
     * @param category 费用类别
     * @param year     年份
     * @param month    月份
     * @param amount   占用金额
     * @return 是否成功
     */
    boolean occupyBudget(Long deptId, String category, Integer year, Integer month, BigDecimal amount);

    /**
     * 释放预算
     *
     * @param deptId   部门ID
     * @param category 费用类别
     * @param year     年份
     * @param month    月份
     * @param amount   释放金额
     * @return 是否成功
     */
    boolean releaseBudget(Long deptId, String category, Integer year, Integer month, BigDecimal amount);

    /**
     * 执行预算（确认消费）
     *
     * @param deptId   部门ID
     * @param category 费用类别
     * @param year     年份
     * @param month    月份
     * @param amount   执行金额
     * @return 是否成功
     */
    boolean executeBudget(Long deptId, String category, Integer year, Integer month, BigDecimal amount);

    /**
     * 删除预算
     *
     * @param id 预算ID
     */
    void deleteBudget(Long id);
}
