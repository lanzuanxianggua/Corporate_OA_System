package cn.oa.admin.service.impl;

import cn.oa.admin.entity.AdmSupply;
import cn.oa.admin.entity.AdmSupplyStock;
import cn.oa.admin.mapper.AdmSupplyMapper;
import cn.oa.admin.mapper.AdmSupplyStockMapper;
import cn.oa.admin.service.AdmSupplyService;
import cn.oa.admin.vo.AdmSupplyVO;
import cn.oa.platform.core.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 办公用品管理服务实现
 *
 * @author oa-admin
 */
@Slf4j
@Service
public class AdmSupplyServiceImpl implements AdmSupplyService {

    private static final int MAX_RETRY_TIMES = 3;

    @Autowired
    private AdmSupplyMapper admSupplyMapper;

    @Autowired
    private AdmSupplyStockMapper admSupplyStockMapper;

    // ============ 用品基础管理 ============

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSupply(AdmSupply supply) {
        admSupplyMapper.insert(supply);
        // 同时创建库存记录
        AdmSupplyStock stock = new AdmSupplyStock();
        stock.setSupplyId(supply.getId());
        stock.setTotalQty(0);
        stock.setAvailableQty(0);
        stock.setLockedQty(0);
        admSupplyStockMapper.insert(stock);
        return supply.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSupply(AdmSupply supply) {
        AdmSupply existing = admSupplyMapper.selectById(supply.getId());
        if (existing == null) {
            throw new BusinessException("用品不存在");
        }
        admSupplyMapper.updateById(supply);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSupply(Long id) {
        AdmSupply existing = admSupplyMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("用品不存在");
        }
        admSupplyMapper.deleteById(id);
        // 同时删除库存记录
        LambdaQueryWrapper<AdmSupplyStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdmSupplyStock::getSupplyId, id);
        admSupplyStockMapper.delete(wrapper);
    }

    @Override
    public IPage<AdmSupplyVO> pageSupplies(String keyword, String category, Integer pageNum, Integer pageSize) {
        Page<AdmSupply> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AdmSupply> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(AdmSupply::getSupplyName, keyword);
        }
        if (category != null && !category.isEmpty()) {
            wrapper.eq(AdmSupply::getCategory, category);
        }
        wrapper.orderByDesc(AdmSupply::getCreateTime);

        IPage<AdmSupply> entityPage = admSupplyMapper.selectPage(page, wrapper);

        IPage<AdmSupplyVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toSupplyVO).toList());
        return voPage;
    }

    @Override
    public AdmSupplyVO getSupplyDetail(Long id) {
        AdmSupply entity = admSupplyMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("用品不存在");
        }
        return toSupplyVO(entity);
    }

    // ============ 库存操作（乐观锁） ============

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inbound(Long supplyId, Integer quantity, String operator) {
        AdmSupplyStock stock = getStockBySupplyId(supplyId);
        stock.setTotalQty(stock.getTotalQty() + quantity);
        stock.setAvailableQty(stock.getAvailableQty() + quantity);
        int rows = admSupplyStockMapper.updateById(stock);
        if (rows == 0) {
            throw new BusinessException("库存更新失败，请重试");
        }
        log.info("入库成功: supplyId={}, quantity={}, operator={}", supplyId, quantity, operator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void outbound(Long supplyId, Integer quantity, String operator) {
        // 乐观锁重试机制
        for (int attempt = 1; attempt <= MAX_RETRY_TIMES; attempt++) {
            AdmSupplyStock stock = getStockBySupplyId(supplyId);
            if (stock.getAvailableQty() < quantity) {
                throw new BusinessException("库存不足: 可用数量 " + stock.getAvailableQty() + ", 需要数量 " + quantity);
            }
            stock.setTotalQty(stock.getTotalQty() - quantity);
            stock.setAvailableQty(stock.getAvailableQty() - quantity);

            int rows = admSupplyStockMapper.updateById(stock);
            if (rows > 0) {
                log.info("出库成功: supplyId={}, quantity={}, operator={}", supplyId, quantity, operator);
                return;
            }
            // 更新失败（乐观锁冲突），重试
            log.warn("出库乐观锁冲突，第{}次重试: supplyId={}", attempt, supplyId);
        }
        throw new BusinessException("系统繁忙，出库操作失败，请稍后重试");
    }

    // ============ 库存预警 ============

    @Override
    public IPage<AdmSupplyVO> pageLowStockSupplies(Integer threshold, Integer pageNum, Integer pageSize) {
        Page<AdmSupply> page = new Page<>(pageNum, pageSize);

        // 查询可用数量 <= 阈值的库存记录
        LambdaQueryWrapper<AdmSupplyStock> stockWrapper = new LambdaQueryWrapper<>();
        stockWrapper.le(AdmSupplyStock::getAvailableQty, threshold);
        stockWrapper.select(AdmSupplyStock::getSupplyId);
        java.util.List<Long> lowStockSupplyIds = admSupplyStockMapper.selectList(stockWrapper)
                .stream().map(AdmSupplyStock::getSupplyId).toList();

        if (lowStockSupplyIds.isEmpty()) {
            IPage<AdmSupplyVO> emptyPage = new Page<>(pageNum, pageSize, 0);
            emptyPage.setRecords(java.util.Collections.emptyList());
            return emptyPage;
        }

        // 根据这些ID查询用品信息
        LambdaQueryWrapper<AdmSupply> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(AdmSupply::getId, lowStockSupplyIds);
        wrapper.orderByDesc(AdmSupply::getCreateTime);

        IPage<AdmSupply> entityPage = admSupplyMapper.selectPage(page, wrapper);

        IPage<AdmSupplyVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toSupplyVO).toList());
        return voPage;
    }

    // ============ 内部方法 ============

    private AdmSupplyStock getStockBySupplyId(Long supplyId) {
        LambdaQueryWrapper<AdmSupplyStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdmSupplyStock::getSupplyId, supplyId);
        AdmSupplyStock stock = admSupplyStockMapper.selectOne(wrapper);
        if (stock == null) {
            throw new BusinessException("库存记录不存在");
        }
        return stock;
    }

    private AdmSupplyVO toSupplyVO(AdmSupply entity) {
        AdmSupplyVO vo = new AdmSupplyVO();
        BeanUtils.copyProperties(entity, vo);

        // 填充库存信息
        LambdaQueryWrapper<AdmSupplyStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdmSupplyStock::getSupplyId, entity.getId());
        AdmSupplyStock stock = admSupplyStockMapper.selectOne(wrapper);
        if (stock != null) {
            vo.setTotalQty(stock.getTotalQty());
            vo.setAvailableQty(stock.getAvailableQty());
            vo.setLockedQty(stock.getLockedQty());
            vo.setLowStock(stock.getAvailableQty() <= 10); // 默认预警阈值10
        }
        return vo;
    }
}
