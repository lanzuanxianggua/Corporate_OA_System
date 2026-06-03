package cn.oa.admin.service;

import cn.oa.admin.entity.AdmSupply;
import cn.oa.admin.vo.AdmSupplyVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 办公用品管理服务接口
 *
 * @author oa-admin
 */
public interface AdmSupplyService {

    // ============ 用品基础管理 ============

    /**
     * 创建用品
     */
    Long createSupply(AdmSupply supply);

    /**
     * 更新用品
     */
    void updateSupply(AdmSupply supply);

    /**
     * 删除用品
     */
    void deleteSupply(Long id);

    /**
     * 分页查询用品（含库存信息）
     */
    IPage<AdmSupplyVO> pageSupplies(String keyword, String category, Integer pageNum, Integer pageSize);

    /**
     * 查询用品详情（含库存）
     */
    AdmSupplyVO getSupplyDetail(Long id);

    // ============ 库存操作（乐观锁） ============

    /**
     * 入库操作
     *
     * @param supplyId 用品ID
     * @param quantity 入库数量
     * @param operator 操作人
     */
    void inbound(Long supplyId, Integer quantity, String operator);

    /**
     * 出库操作（使用乐观锁防止超卖）
     *
     * @param supplyId 用品ID
     * @param quantity 出库数量
     * @param operator 操作人
     */
    void outbound(Long supplyId, Integer quantity, String operator);

    // ============ 库存预警 ============

    /**
     * 查询低库存用品列表（可用数量 <= 预警阈值）
     *
     * @param threshold 预警阈值
     * @return 低库存用品列表
     */
    IPage<AdmSupplyVO> pageLowStockSupplies(Integer threshold, Integer pageNum, Integer pageSize);
}
