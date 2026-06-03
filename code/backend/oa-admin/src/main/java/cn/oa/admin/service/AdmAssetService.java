package cn.oa.admin.service;

import cn.oa.admin.dto.AdmAssetCreateDTO;
import cn.oa.admin.dto.AdmAssetOperateDTO;
import cn.oa.admin.vo.AdmAssetLogVO;
import cn.oa.admin.vo.AdmAssetVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 固定资产管理服务接口
 *
 * @author oa-admin
 */
public interface AdmAssetService {

    /**
     * 创建资产
     */
    Long createAsset(AdmAssetCreateDTO dto);

    /**
     * 更新资产
     */
    void updateAsset(Long id, AdmAssetCreateDTO dto);

    /**
     * 删除资产
     */
    void deleteAsset(Long id);

    /**
     * 分页查询资产
     */
    IPage<AdmAssetVO> pageAssets(String keyword, String status, String category, Integer pageNum, Integer pageSize);

    /**
     * 查询资产详情
     */
    AdmAssetVO getAssetDetail(Long id);

    // ============ 资产操作 ============

    /**
     * 资产操作（领用/归还/维修/报废）
     *
     * @param dto        操作DTO
     * @param operatorId 操作人ID
     */
    void operateAsset(AdmAssetOperateDTO dto, Long operatorId);

    // ============ 资产日志 ============

    /**
     * 分页查询资产操作日志
     */
    IPage<AdmAssetLogVO> pageAssetLogs(Long assetId, Integer pageNum, Integer pageSize);
}
