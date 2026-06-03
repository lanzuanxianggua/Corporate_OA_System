package cn.oa.hr.service;

import cn.oa.hr.dto.HrLeaveBalanceAdjustDTO;
import cn.oa.hr.dto.HrLeaveBalanceInitDTO;
import cn.oa.hr.entity.HrLeaveBalance;
import cn.oa.hr.vo.HrLeaveBalanceVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.math.BigDecimal;
import java.util.List;

/**
 * HR假期余额服务接口
 *
 * @author oa-hr
 */
public interface HrLeaveBalanceService {

    /**
     * 查询员工假期余额列表
     *
     * @param empId 员工ID
     * @param year  年度（可选）
     * @return 余额列表
     */
    List<HrLeaveBalanceVO> getMyBalances(Long empId, Integer year);

    /**
     * 分页查询假期余额（管理端）
     *
     * @param empId    员工ID（可选）
     * @param year     年度（可选）
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    IPage<HrLeaveBalanceVO> pageQuery(Long empId, Integer year, int pageNum, int pageSize);

    /**
     * 初始化员工年度假期余额
     *
     * @param dto 初始化DTO
     */
    void initBalance(HrLeaveBalanceInitDTO dto);

    /**
     * 调整假期余额
     *
     * @param dto 调整DTO
     */
    void adjustBalance(HrLeaveBalanceAdjustDTO dto);

    /**
     * 冻结余额（提交申请时调用）
     * 使用原子SQL，并发安全
     *
     * @param empId     员工ID
     * @param leaveType 假期类型
     * @param year      年度
     * @param days      冻结天数
     * @return 是否成功
     */
    boolean freezeBalance(Long empId, String leaveType, Integer year, BigDecimal days);

    /**
     * 确认余额（审批通过时调用）
     * 将冻结转为已用
     *
     * @param empId     员工ID
     * @param leaveType 假期类型
     * @param year      年度
     * @param days      确认天数
     * @return 是否成功
     */
    boolean confirmBalance(Long empId, String leaveType, Integer year, BigDecimal days);

    /**
     * 释放冻结余额（驳回/撤回时调用）
     *
     * @param empId     员工ID
     * @param leaveType 假期类型
     * @param year      年度
     * @param days      释放天数
     * @return 是否成功
     */
    boolean releaseFrozenBalance(Long empId, String leaveType, Integer year, BigDecimal days);

    /**
     * 获取余额实体
     *
     * @param empId     员工ID
     * @param leaveType 假期类型
     * @param year      年度
     * @return 余额实体
     */
    HrLeaveBalance getBalance(Long empId, String leaveType, Integer year);

    /**
     * 检查余额是否充足
     *
     * @param empId     员工ID
     * @param leaveType 假期类型
     * @param year      年度
     * @param days      需要的天数
     * @return 是否充足
     */
    boolean hasEnoughBalance(Long empId, String leaveType, Integer year, BigDecimal days);
}
