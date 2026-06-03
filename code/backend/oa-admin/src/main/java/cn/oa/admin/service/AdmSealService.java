package cn.oa.admin.service;

import cn.oa.admin.dto.AdmSealUsageCreateDTO;
import cn.oa.admin.entity.AdmSeal;
import cn.oa.admin.vo.AdmSealUsageVO;
import cn.oa.admin.vo.AdmSealVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 印章管理服务接口
 *
 * @author oa-admin
 */
public interface AdmSealService {

    // ============ 印章基础管理 ============

    /**
     * 创建印章
     */
    Long createSeal(AdmSeal seal);

    /**
     * 更新印章
     */
    void updateSeal(AdmSeal seal);

    /**
     * 删除印章
     */
    void deleteSeal(Long id);

    /**
     * 分页查询印章
     */
    IPage<AdmSealVO> pageSeals(String keyword, String status, Integer pageNum, Integer pageSize);

    /**
     * 查询印章详情
     */
    AdmSealVO getSealDetail(Long id);

    // ============ 印章使用申请 ============

    /**
     * 创建印章使用申请
     */
    Long createUsage(AdmSealUsageCreateDTO dto, Long applicantId);

    /**
     * 审批印章使用申请
     */
    void approveUsage(Long usageId);

    /**
     * 驳回印章使用申请
     */
    void rejectUsage(Long usageId, String reason);

    /**
     * 分页查询印章使用记录
     */
    IPage<AdmSealUsageVO> pageUsages(Long sealId, String status, Integer pageNum, Integer pageSize);
}
