package cn.oa.hr.employee.service;

import cn.oa.hr.employee.dto.HrEmployeeProfileCreateDTO;
import cn.oa.hr.employee.dto.HrEmployeeProfileQueryDTO;
import cn.oa.hr.employee.dto.HrEmployeeProfileUpdateDTO;
import cn.oa.hr.employee.entity.HrEmployeeProfile;
import cn.oa.hr.employee.mapper.HrEmployeeProfileMapper;
import cn.oa.hr.employee.vo.HrEmployeeProfileVO;
import cn.oa.platform.common.api.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 员工档案 Service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HrEmployeeProfileService {

    private final HrEmployeeProfileMapper mapper;

    /**
     * 新增员工档案.
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(HrEmployeeProfileCreateDTO dto) {
        // 1) 检查 empId 是否已存在档案
        LambdaQueryWrapper<HrEmployeeProfile> existsWrapper = new LambdaQueryWrapper<HrEmployeeProfile>()
                .eq(HrEmployeeProfile::getEmpId, dto.getEmpId());
        if (mapper.selectCount(existsWrapper) > 0) {
            throw new BizException(RCode.BAD_REQUEST, "该员工已存在档案");
        }

        // 2) DTO -> Entity
        HrEmployeeProfile profile = new HrEmployeeProfile();
        profile.setEmpId(dto.getEmpId());
        profile.setWorkNo(dto.getWorkNo());
        profile.setHireDate(dto.getHireDate());
        profile.setContractType(dto.getContractType());
        profile.setContractEndDate(dto.getContractEndDate());
        profile.setEmergencyContact(dto.getEmergencyContact());
        profile.setEmergencyPhone(dto.getEmergencyPhone());
        profile.setBankName(dto.getBankName());
        profile.setBankAccount(dto.getBankAccount());
        profile.setStatus("ACTIVE");

        // 3) 插入
        mapper.insert(profile);
        return profile.getId();
    }

    /**
     * 修改员工档案.
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, HrEmployeeProfileUpdateDTO dto) {
        HrEmployeeProfile exist = mapper.selectById(id);
        if (exist == null) {
            throw new BizException(RCode.NOT_FOUND, "员工档案不存在");
        }

        // 保留 empId 不变, 只更新 DTO 中非 null 的字段
        HrEmployeeProfile patch = new HrEmployeeProfile();
        patch.setId(id);
        patch.setEmpId(exist.getEmpId());
        patch.setWorkNo(dto.getWorkNo());
        patch.setHireDate(dto.getHireDate());
        patch.setContractType(dto.getContractType());
        patch.setContractEndDate(dto.getContractEndDate());
        patch.setEmergencyContact(dto.getEmergencyContact());
        patch.setEmergencyPhone(dto.getEmergencyPhone());
        patch.setBankName(dto.getBankName());
        patch.setBankAccount(dto.getBankAccount());
        patch.setStatus(dto.getStatus());

        mapper.updateById(patch);
    }

    /**
     * 删除员工档案.
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (mapper.selectById(id) == null) {
            throw new BizException(RCode.NOT_FOUND, "员工档案不存在");
        }
        mapper.deleteById(id);
    }

    /**
     * 员工档案列表 (不分页, 向后兼容).
     */
    public List<Map<String, Object>> list(int limit) {
        return mapper.findAllWithJoins(Math.min(limit, 100));
    }

    /**
     * 员工档案详情.
     */
    public Map<String, Object> getDetail(Long id) {
        Map<String, Object> detail = mapper.findDetail(id);
        if (detail == null) {
            throw new BizException(RCode.NOT_FOUND, "员工档案不存在");
        }
        return detail;
    }

    /**
     * 分页查询员工档案列表.
     */
    @SuppressWarnings("unchecked")
    public PageResult<HrEmployeeProfileVO> listPage(HrEmployeeProfileQueryDTO query) {
        Page<Map<String, Object>> page = new Page<>(query.getPageNum(), query.getPageSize());

        Page<Map<String, Object>> result = mapper.findPageWithJoins(
                page, query.getKeyword(), query.getStatus(), query.getContractType()
        );

        List<HrEmployeeProfileVO> voList = result.getRecords().stream()
                .map(this::mapToVO)
                .toList();

        return PageResult.of(voList, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 根据员工ID查档案.
     */
    public HrEmployeeProfile getByEmpId(Long empId) {
        LambdaQueryWrapper<HrEmployeeProfile> wrapper = new LambdaQueryWrapper<HrEmployeeProfile>()
                .eq(HrEmployeeProfile::getEmpId, empId)
                .last("LIMIT 1");
        HrEmployeeProfile profile = mapper.selectOne(wrapper);
        if (profile == null) {
            throw new BizException(RCode.NOT_FOUND, "该员工不存在档案");
        }
        return profile;
    }

    /**
     * Map -> VO 转换.
     */
    private HrEmployeeProfileVO mapToVO(Map<String, Object> map) {
        HrEmployeeProfileVO vo = new HrEmployeeProfileVO();
        vo.setId(toLong(map.get("id")));
        vo.setEmpId(toLong(map.get("emp_id")));
        vo.setEmpName(toStr(map.get("emp_name")));
        vo.setUsername(toStr(map.get("username")));
        vo.setDeptName(toStr(map.get("dept_name")));
        vo.setWorkNo(toStr(map.get("work_no")));
        vo.setHireDate(toLocalDate(map.get("hire_date")));
        vo.setContractType(toStr(map.get("contract_type")));
        vo.setContractEndDate(toLocalDate(map.get("contract_end_date")));
        vo.setEmergencyContact(toStr(map.get("emergency_contact")));
        vo.setEmergencyPhone(toStr(map.get("emergency_phone")));
        vo.setBankName(toStr(map.get("bank_name")));
        vo.setBankAccount(toStr(map.get("bank_account")));
        vo.setStatus(toStr(map.get("status")));
        vo.setCreateTime(toLocalDateTime(map.get("create_time")));
        return vo;
    }

    private static Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Long l) return l;
        if (obj instanceof Number n) return n.longValue();
        try { return Long.parseLong(obj.toString()); } catch (Exception e) { return null; }
    }

    private static String toStr(Object obj) {
        return obj == null ? null : obj.toString();
    }

    private static java.time.LocalDate toLocalDate(Object obj) {
        if (obj == null) return null;
        if (obj instanceof java.time.LocalDate ld) return ld;
        if (obj instanceof java.sql.Date sd) return sd.toLocalDate();
        try { return java.time.LocalDate.parse(obj.toString()); } catch (Exception e) { return null; }
    }

    private static java.time.LocalDateTime toLocalDateTime(Object obj) {
        if (obj == null) return null;
        if (obj instanceof java.time.LocalDateTime ldt) return ldt;
        if (obj instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        try { return java.time.LocalDateTime.parse(obj.toString()); } catch (Exception e) { return null; }
    }
}
