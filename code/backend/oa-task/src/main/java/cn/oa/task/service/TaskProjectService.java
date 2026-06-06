package cn.oa.task.service;

import cn.oa.task.dto.TaskProjectCreateDTO;
import cn.oa.task.dto.TaskProjectQueryDTO;
import cn.oa.task.entity.TaskProject;
import cn.oa.task.mapper.TaskProjectMapper;
import cn.oa.task.vo.TaskProjectVO;
import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j @Service @RequiredArgsConstructor
public class TaskProjectService {
    private final TaskProjectMapper mapper;

    @Transactional public Long create(TaskProjectCreateDTO dto, Long empId) {
        TaskProject p = new TaskProject(); p.setProjectName(dto.getProjectName());
        p.setDescription(dto.getDescription()); p.setStartDate(dto.getStartDate()); p.setEndDate(dto.getEndDate());
        p.setDeptId(dto.getDeptId()); p.setOwnerEmpId(empId); p.setStatus("ACTIVE");
        mapper.insert(p); log.info("项目创建成功: id={}", p.getId()); return p.getId();
    }

    @Transactional public void update(Long id, TaskProjectCreateDTO dto) {
        TaskProject p = mapper.selectById(id);
        if (p == null) throw new BizException(RCode.NOT_FOUND, "项目不存在");
        p.setProjectName(dto.getProjectName()); p.setDescription(dto.getDescription());
        p.setStartDate(dto.getStartDate()); p.setEndDate(dto.getEndDate()); p.setDeptId(dto.getDeptId());
        mapper.updateById(p);
    }

    @Transactional public void delete(Long id) { mapper.deleteById(id); }

    public TaskProjectVO getById(Long id) {
        TaskProject p = mapper.selectById(id);
        if (p == null) throw new BizException(RCode.NOT_FOUND, "项目不存在");
        TaskProjectVO vo = new TaskProjectVO(); vo.setId(p.getId()); vo.setProjectName(p.getProjectName());
        vo.setProjectCode(p.getProjectCode()); vo.setDescription(p.getDescription());
        vo.setStatus(p.getStatus()); vo.setStartDate(p.getStartDate()); vo.setEndDate(p.getEndDate());
        vo.setDeptId(p.getDeptId()); vo.setOwnerEmpId(p.getOwnerEmpId()); vo.setCreateTime(p.getCreateTime());
        return vo;
    }

    public PageResult<TaskProject> listPage(TaskProjectQueryDTO query, Long deptId) {
        Page<TaskProject> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<TaskProject> w = new LambdaQueryWrapper<TaskProject>()
                .eq(query.getStatus() != null, TaskProject::getStatus, query.getStatus())
                .eq(deptId != null, TaskProject::getDeptId, deptId)
                .orderByDesc(TaskProject::getCreateTime);
        Page<TaskProject> result = mapper.selectPage(page, w);
        return PageResult.of(result.getRecords(), result.getTotal(), query.getPageNum(), query.getPageSize());
    }
}