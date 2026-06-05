package cn.oa.admin.service;

import cn.oa.admin.dto.AdmAssetCreateDTO;
import cn.oa.admin.entity.AdmAsset;
import cn.oa.admin.mapper.AdmAssetMapper;
import cn.oa.admin.vo.AdmAssetVO;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import cn.oa.system.entity.SysDept;
import cn.oa.system.entity.SysEmp;
import cn.oa.system.mapper.SysDeptMapper;
import cn.oa.system.mapper.SysEmpMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 资产管理 Service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdmAssetService {

    private final AdmAssetMapper assetMapper;
    private final SysEmpMapper empMapper;
    private final SysDeptMapper deptMapper;

    /** 序列计数器 (同一天内递增). */
    private static final AtomicInteger SEQ = new AtomicInteger(0);

    /**
     * 新增资产 (自动生成 assetCode: "AST" + yyyyMMdd + 4位序列).
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(AdmAssetCreateDTO dto) {
        AdmAsset asset = new AdmAsset();
        asset.setAssetName(dto.getAssetName());
        asset.setAssetCode(generateAssetCode());
        asset.setCategory(dto.getCategory());
        asset.setBrand(dto.getBrand());
        asset.setModel(dto.getModel());
        asset.setPurchaseDate(dto.getPurchaseDate());
        asset.setPurchasePrice(dto.getPurchasePrice());
        asset.setDeptId(dto.getDeptId());
        asset.setCustodian(dto.getCustodian());
        asset.setLocation(dto.getLocation());
        asset.setStatus("IDLE");
        assetMapper.insert(asset);
        return asset.getId();
    }

    /**
     * 修改资产.
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AdmAssetCreateDTO dto) {
        AdmAsset exist = assetMapper.selectById(id);
        if (exist == null) {
            throw new BizException(RCode.NOT_FOUND, "资产不存在");
        }
        AdmAsset asset = new AdmAsset();
        asset.setId(id);
        asset.setAssetName(dto.getAssetName());
        asset.setCategory(dto.getCategory());
        asset.setBrand(dto.getBrand());
        asset.setModel(dto.getModel());
        asset.setPurchaseDate(dto.getPurchaseDate());
        asset.setPurchasePrice(dto.getPurchasePrice());
        asset.setDeptId(dto.getDeptId());
        asset.setCustodian(dto.getCustodian());
        asset.setLocation(dto.getLocation());
        assetMapper.updateById(asset);
    }

    /**
     * 删除资产.
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (assetMapper.selectById(id) == null) {
            throw new BizException(RCode.NOT_FOUND, "资产不存在");
        }
        assetMapper.deleteById(id);
    }

    /**
     * 资产详情 (关联 custodianName/deptName).
     */
    public AdmAssetVO getById(Long id) {
        AdmAsset asset = assetMapper.selectById(id);
        if (asset == null) {
            throw new BizException(RCode.NOT_FOUND, "资产不存在");
        }
        return toVO(asset);
    }

    /**
     * 资产分页列表.
     */
    public Map<String, Object> list(String category, String status, int pageNum, int pageSize) {
        LambdaQueryWrapper<AdmAsset> wrapper = new LambdaQueryWrapper<>();
        if (category != null && !category.isBlank()) {
            wrapper.eq(AdmAsset::getCategory, category);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(AdmAsset::getStatus, status);
        }
        wrapper.orderByDesc(AdmAsset::getCreateTime);

        Page<AdmAsset> page = assetMapper.selectPage(Page.of(pageNum, pageSize), wrapper);

        List<AdmAssetVO> records = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("list", records);
        data.put("total", page.getTotal());
        data.put("pageNum", page.getCurrent());
        data.put("pageSize", page.getSize());
        return data;
    }

    /**
     * 生成资产编号: AST + yyyyMMdd + 4位序列.
     */
    private String generateAssetCode() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int seq = SEQ.incrementAndGet() % 10000;
        return "AST" + datePart + String.format("%04d", seq);
    }

    /**
     * Entity -> VO (关联查询 custodianName, deptName).
     */
    private AdmAssetVO toVO(AdmAsset asset) {
        AdmAssetVO vo = new AdmAssetVO();
        vo.setId(asset.getId());
        vo.setAssetCode(asset.getAssetCode());
        vo.setAssetName(asset.getAssetName());
        vo.setCategory(asset.getCategory());
        vo.setBrand(asset.getBrand());
        vo.setModel(asset.getModel());
        vo.setPurchaseDate(asset.getPurchaseDate());
        vo.setPurchasePrice(asset.getPurchasePrice());
        vo.setDeptId(asset.getDeptId());
        vo.setCustodian(asset.getCustodian());
        vo.setStatus(asset.getStatus());
        vo.setLocation(asset.getLocation());
        vo.setCreateTime(asset.getCreateTime());

        // 关联保管人姓名
        if (asset.getCustodian() != null) {
            SysEmp emp = empMapper.selectById(asset.getCustodian());
            if (emp != null) {
                vo.setCustodianName(emp.getRealName());
            }
        }
        // 关联部门名称
        if (asset.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(asset.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getDeptName());
            }
        }
        return vo;
    }
}
