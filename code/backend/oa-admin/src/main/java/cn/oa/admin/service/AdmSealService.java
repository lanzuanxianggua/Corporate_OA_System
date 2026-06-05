package cn.oa.admin.service;

import cn.oa.admin.dto.AdmSealCreateDTO;
import cn.oa.admin.entity.AdmSeal;
import cn.oa.admin.mapper.AdmSealMapper;
import cn.oa.admin.vo.AdmSealVO;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import cn.oa.system.entity.SysDept;
import cn.oa.system.entity.SysEmp;
import cn.oa.system.mapper.SysDeptMapper;
import cn.oa.system.mapper.SysEmpMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 印章管理 Service.
 */
@Service
@RequiredArgsConstructor
public class AdmSealService {

    private final AdmSealMapper sealMapper;
    private final SysEmpMapper empMapper;
    private final SysDeptMapper deptMapper;

    /**
     * 新增印章.
     */
    public Long create(AdmSealCreateDTO dto) {
        AdmSeal seal = new AdmSeal();
        seal.setSealName(dto.getSealName());
        seal.setSealType(dto.getSealType());
        seal.setCustodian(dto.getCustodian());
        seal.setDeptId(dto.getDeptId());
        seal.setLocation(dto.getLocation());
        seal.setStatus("ACTIVE");
        sealMapper.insert(seal);
        return seal.getId();
    }

    /**
     * 修改印章.
     */
    public void update(Long id, AdmSealCreateDTO dto) {
        AdmSeal exist = sealMapper.selectById(id);
        if (exist == null) {
            throw new BizException(RCode.NOT_FOUND, "印章不存在");
        }
        AdmSeal seal = new AdmSeal();
        seal.setId(id);
        seal.setSealName(dto.getSealName());
        seal.setSealType(dto.getSealType());
        seal.setCustodian(dto.getCustodian());
        seal.setDeptId(dto.getDeptId());
        seal.setLocation(dto.getLocation());
        sealMapper.updateById(seal);
    }

    /**
     * 删除印章.
     */
    public void delete(Long id) {
        if (sealMapper.selectById(id) == null) {
            throw new BizException(RCode.NOT_FOUND, "印章不存在");
        }
        sealMapper.deleteById(id);
    }

    /**
     * 印章详情 (关联 custodianName/deptName).
     */
    public AdmSealVO getById(Long id) {
        AdmSeal seal = sealMapper.selectById(id);
        if (seal == null) {
            throw new BizException(RCode.NOT_FOUND, "印章不存在");
        }
        return toVO(seal);
    }

    /**
     * 印章分页列表.
     */
    public Map<String, Object> list(Long deptId, int pageNum, int pageSize) {
        LambdaQueryWrapper<AdmSeal> wrapper = new LambdaQueryWrapper<>();
        if (deptId != null) {
            wrapper.eq(AdmSeal::getDeptId, deptId);
        }
        wrapper.orderByDesc(AdmSeal::getCreateTime);

        Page<AdmSeal> page = sealMapper.selectPage(Page.of(pageNum, pageSize), wrapper);

        List<AdmSealVO> records = page.getRecords().stream()
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
     * Entity -> VO (关联查询 custodianName, deptName).
     */
    private AdmSealVO toVO(AdmSeal seal) {
        AdmSealVO vo = new AdmSealVO();
        vo.setId(seal.getId());
        vo.setSealName(seal.getSealName());
        vo.setSealType(seal.getSealType());
        vo.setCustodian(seal.getCustodian());
        vo.setDeptId(seal.getDeptId());
        vo.setStatus(seal.getStatus());
        vo.setLocation(seal.getLocation());
        vo.setCreateTime(seal.getCreateTime());

        // 关联保管人姓名
        if (seal.getCustodian() != null) {
            SysEmp emp = empMapper.selectById(seal.getCustodian());
            if (emp != null) {
                vo.setCustodianName(emp.getRealName());
            }
        }
        // 关联部门名称
        if (seal.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(seal.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getDeptName());
            }
        }
        return vo;
    }
}
