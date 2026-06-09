package cn.oa.admin.service;

import cn.oa.admin.entity.AdmSupply;
import cn.oa.admin.entity.AdmSupplyCategory;
import cn.oa.admin.entity.AdmSupplyRequest;
import cn.oa.admin.entity.AdmSupplyRequestItem;
import cn.oa.admin.entity.AdmSupplyStock;
import cn.oa.admin.mapper.AdmSupplyCategoryMapper;
import cn.oa.admin.mapper.AdmSupplyMapper;
import cn.oa.admin.mapper.AdmSupplyRequestItemMapper;
import cn.oa.admin.mapper.AdmSupplyRequestMapper;
import cn.oa.admin.mapper.AdmSupplyStockMapper;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class AdmSupplyService {

    private static final AtomicInteger SUPPLY_SEQ = new AtomicInteger();
    private static final AtomicInteger REQUEST_SEQ = new AtomicInteger();

    private final AdmSupplyCategoryMapper categoryMapper;
    private final AdmSupplyMapper supplyMapper;
    private final AdmSupplyStockMapper stockMapper;
    private final AdmSupplyRequestMapper requestMapper;
    private final AdmSupplyRequestItemMapper requestItemMapper;

    @Transactional(rollbackFor = Exception.class)
    public Long createCategory(AdmSupplyCategory category) {
        if (category.getStatus() == null) category.setStatus("ACTIVE");
        categoryMapper.insert(category);
        return category.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(AdmSupplyCategory category) {
        categoryMapper.updateById(category);
    }

    public List<AdmSupplyCategory> listCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<AdmSupplyCategory>()
                .orderByAsc(AdmSupplyCategory::getParentId)
                .orderByDesc(AdmSupplyCategory::getCreateTime));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createSupply(AdmSupply supply) {
        if (supply.getSupplyCode() == null || supply.getSupplyCode().isBlank()) {
            supply.setSupplyCode(nextCode("SUP", SUPPLY_SEQ));
        }
        if (supply.getStatus() == null) supply.setStatus("ACTIVE");
        if (supply.getSafetyStock() == null) supply.setSafetyStock(0);
        supplyMapper.insert(supply);
        AdmSupplyStock stock = new AdmSupplyStock();
        stock.setSupplyId(supply.getId());
        stock.setQuantity(0);
        stock.setLockedQuantity(0);
        stockMapper.insert(stock);
        return supply.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateSupply(AdmSupply supply) {
        supplyMapper.updateById(supply);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteSupply(Long id) {
        supplyMapper.deleteById(id);
    }

    public Page<AdmSupply> listSupplies(String keyword, String status, int pn, int ps) {
        LambdaQueryWrapper<AdmSupply> wrapper = new LambdaQueryWrapper<AdmSupply>()
                .like(keyword != null && !keyword.isBlank(), AdmSupply::getSupplyName, keyword)
                .eq(status != null && !status.isBlank(), AdmSupply::getStatus, status)
                .orderByDesc(AdmSupply::getCreateTime);
        return supplyMapper.selectPage(new Page<>(pn, ps), wrapper);
    }

    public AdmSupply getSupply(Long id) {
        AdmSupply supply = supplyMapper.selectById(id);
        if (supply == null) throw new BizException(RCode.NOT_FOUND, "Supply not found: " + id);
        return supply;
    }

    public AdmSupplyStock getStock(Long supplyId) {
        AdmSupplyStock stock = stockMapper.selectOne(new LambdaQueryWrapper<AdmSupplyStock>()
                .eq(AdmSupplyStock::getSupplyId, supplyId)
                .last("LIMIT 1"));
        if (stock == null) {
            stock = new AdmSupplyStock();
            stock.setSupplyId(supplyId);
            stock.setQuantity(0);
            stock.setLockedQuantity(0);
        }
        return stock;
    }

    @Transactional(rollbackFor = Exception.class)
    public void adjustStock(Long supplyId, int quantity, String location) {
        AdmSupplyStock stock = ensureStock(supplyId);
        int next = nz(stock.getQuantity()) + quantity;
        if (next < 0) throw new BizException(RCode.BAD_REQUEST, "Insufficient stock");
        stock.setQuantity(next);
        if (location != null) stock.setLocation(location);
        stockMapper.updateById(stock);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createRequest(AdmSupplyRequest request, List<AdmSupplyRequestItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BizException(RCode.BAD_REQUEST, "Request items are required");
        }
        if (request.getRequestNo() == null || request.getRequestNo().isBlank()) {
            request.setRequestNo(nextCode("SR", REQUEST_SEQ));
        }
        if (request.getRequestType() == null) request.setRequestType("OUT");
        request.setStatus("PENDING");
        requestMapper.insert(request);
        for (AdmSupplyRequestItem item : items) {
            item.setRequestId(request.getId());
            requestItemMapper.insert(item);
        }
        return request.getId();
    }

    public Page<AdmSupplyRequest> listRequests(String status, int pn, int ps) {
        LambdaQueryWrapper<AdmSupplyRequest> wrapper = new LambdaQueryWrapper<AdmSupplyRequest>()
                .eq(status != null && !status.isBlank(), AdmSupplyRequest::getStatus, status)
                .orderByDesc(AdmSupplyRequest::getCreateTime);
        return requestMapper.selectPage(new Page<>(pn, ps), wrapper);
    }

    public List<AdmSupplyRequestItem> listRequestItems(Long requestId) {
        return requestItemMapper.selectList(new LambdaQueryWrapper<AdmSupplyRequestItem>()
                .eq(AdmSupplyRequestItem::getRequestId, requestId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void approveRequest(Long id) {
        AdmSupplyRequest request = requestMapper.selectById(id);
        if (request == null) throw new BizException(RCode.NOT_FOUND, "Request not found: " + id);
        if (!"PENDING".equals(request.getStatus())) {
            throw new BizException(RCode.BAD_REQUEST, "Only PENDING request can be approved");
        }
        List<AdmSupplyRequestItem> items = listRequestItems(id);
        for (AdmSupplyRequestItem item : items) {
            int quantity = nz(item.getQuantity());
            if ("OUT".equals(request.getRequestType())) {
                adjustStock(item.getSupplyId(), -quantity, null);
            } else {
                adjustStock(item.getSupplyId(), quantity, null);
            }
        }
        request.setStatus("APPROVED");
        request.setApproveTime(LocalDateTime.now());
        requestMapper.updateById(request);
    }

    @Transactional(rollbackFor = Exception.class)
    public void rejectRequest(Long id, String reason) {
        AdmSupplyRequest request = requestMapper.selectById(id);
        if (request == null) throw new BizException(RCode.NOT_FOUND, "Request not found: " + id);
        request.setStatus("REJECTED");
        request.setRejectReason(reason);
        requestMapper.updateById(request);
    }

    /**
     * V1010: Workflow callback hook for the supply-request approval flow.
     * <ul>
     *   <li>1 → APPROVED (approved; stock already adjusted in approveRequest; this
     *       just ensures the status sticks in case of a manual override)</li>
     *   <li>2 → REJECTED (rejected; stock is NOT reversed because approveRequest was
     *       not called for this path)</li>
     *   <li>3 → REJECTED (withdrawn — same handling as rejected)</li>
     * </ul>
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        AdmSupplyRequest request = requestMapper.selectById(id);
        if (request == null) return;
        if (status == null) return;
        switch (status) {
            case 1: request.setStatus("APPROVED"); break;
            case 2: request.setStatus("REJECTED"); break;
            case 3: request.setStatus("REJECTED"); break;
            default: return;
        }
        requestMapper.updateById(request);
    }

    private AdmSupplyStock ensureStock(Long supplyId) {
        AdmSupplyStock stock = stockMapper.selectOne(new LambdaQueryWrapper<AdmSupplyStock>()
                .eq(AdmSupplyStock::getSupplyId, supplyId)
                .last("LIMIT 1"));
        if (stock != null) return stock;
        stock = new AdmSupplyStock();
        stock.setSupplyId(supplyId);
        stock.setQuantity(0);
        stock.setLockedQuantity(0);
        stockMapper.insert(stock);
        return stock;
    }

    private static String nextCode(String prefix, AtomicInteger seq) {
        return prefix + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + String.format("%04d", seq.incrementAndGet() % 10000);
    }

    private static int nz(Integer value) {
        return value == null ? 0 : value;
    }
}
