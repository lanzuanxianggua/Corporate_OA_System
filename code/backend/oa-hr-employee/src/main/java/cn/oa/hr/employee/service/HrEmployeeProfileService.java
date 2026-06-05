package cn.oa.hr.employee.service;

import cn.oa.hr.employee.entity.HrEmployeeProfile;
import cn.oa.hr.employee.mapper.HrEmployeeProfileMapper;
import cn.oa.platform.common.api.R;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 员工档案 Service.
 */
@Service
@RequiredArgsConstructor
public class HrEmployeeProfileService {

    private final HrEmployeeProfileMapper mapper;

    public Long create(HrEmployeeProfile profile) {
        // 1) 验证 empId 必填
        if (profile.getEmpId() == null) {
            throw new BizException(RCode.BAD_REQUEST, "empId 必填");
        }
        // 2) 验证 workNo 唯一
        if (profile.getWorkNo() != null && !profile.getWorkNo().isBlank()) {
            // 简化: 不查重, 留给 DB UNIQUE 约束
        }
        // 3) 默认值
        if (profile.getStatus() == null) {
            profile.setStatus("ACTIVE");
        }
        // 4) 插入
        mapper.insert(profile);
        return profile.getId();
    }

    public void update(Long id, HrEmployeeProfile patch) {
        HrEmployeeProfile exist = mapper.selectById(id);
        if (exist == null) {
            throw new BizException(RCode.NOT_FOUND, "员工档案不存在");
        }
        patch.setId(id);
        // 保留 empId 不变
        patch.setEmpId(exist.getEmpId());
        mapper.updateById(patch);
    }

    public void delete(Long id) {
        // 1) 校验存在
        if (mapper.selectById(id) == null) {
            throw new BizException(RCode.NOT_FOUND, "员工档案不存在");
        }
        // 2) 物理删除 (del_flag 字段由 MetaObjectHandler 管, 这里走物理删简化)
        mapper.deleteById(id);
    }

    public List<Map<String, Object>> list(int limit) {
        return mapper.findAllWithJoins(Math.min(limit, 100));
    }

    public Map<String, Object> getDetail(Long id) {
        Map<String, Object> detail = mapper.findDetail(id);
        if (detail == null) {
            throw new BizException(RCode.NOT_FOUND, "员工档案不存在");
        }
        return detail;
    }
}
