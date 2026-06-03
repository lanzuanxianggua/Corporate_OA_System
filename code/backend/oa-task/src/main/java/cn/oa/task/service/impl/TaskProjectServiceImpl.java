package cn.oa.task.service.impl;

import cn.oa.platform.core.exception.BusinessException;
import cn.oa.task.dto.TaskProjectCreateDTO;
import cn.oa.task.dto.TaskProjectUpdateDTO;
import cn.oa.task.entity.TaskItem;
import cn.oa.task.entity.TaskProject;
import cn.oa.task.entity.TaskProjectMember;
import cn.oa.task.mapper.TaskItemMapper;
import cn.oa.task.mapper.TaskProjectMapper;
import cn.oa.task.mapper.TaskProjectMemberMapper;
import cn.oa.task.service.TaskProjectService;
import cn.oa.task.vo.TaskProjectVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 项目管理服务实现
 */
@Service
public class TaskProjectServiceImpl extends ServiceImpl<TaskProjectMapper, TaskProject> implements TaskProjectService {

    @Autowired
    private TaskProjectMapper projectMapper;

    @Autowired
    private TaskProjectMemberMapper memberMapper;

    @Autowired
    private TaskItemMapper itemMapper;

    @Override
    public IPage<TaskProject> pageList(Integer pageNum, Integer pageSize, String name, String status, Long ownerId) {
        Page<TaskProject> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TaskProject> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like(TaskProject::getName, name);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(TaskProject::getStatus, status);
        }
        if (ownerId != null) {
            wrapper.eq(TaskProject::getOwnerId, ownerId);
        }
        wrapper.orderByDesc(TaskProject::getCreateTime);
        return projectMapper.selectPage(page, wrapper);
    }

    /**
     * 查询项目详情（含成员信息）
     */
    public TaskProjectVO getDetail(Long id) {
        TaskProject project = projectMapper.selectById(id);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }
        TaskProjectVO vo = new TaskProjectVO();
        BeanUtils.copyProperties(project, vo);
        vo.setStatusName(getStatusName(project.getStatus()));

        // 查询成员
        List<TaskProjectMember> members = getMembers(id);
        vo.setMemberCount(members.size());
        List<TaskProjectVO.MemberVO> memberVOs = members.stream().map(m -> {
            TaskProjectVO.MemberVO mv = new TaskProjectVO.MemberVO();
            BeanUtils.copyProperties(m, mv);
            mv.setRoleName(getMemberRoleName(m.getRole()));
            return mv;
        }).toList();
        vo.setMembers(memberVOs);

        return vo;
    }

    /**
     * 创建项目
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(TaskProjectCreateDTO dto, Long ownerId) {
        TaskProject project = new TaskProject();
        BeanUtils.copyProperties(dto, project);
        project.setStatus("PLANNING");
        project.setProgress(0);
        project.setOwnerId(ownerId);
        projectMapper.insert(project);

        // 添加创建者为项目拥有者成员
        TaskProjectMember member = new TaskProjectMember();
        member.setProjectId(project.getId());
        member.setEmpId(ownerId);
        member.setRole("OWNER");
        memberMapper.insert(member);

        return project.getId();
    }

    /**
     * 更新项目
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(TaskProjectUpdateDTO dto) {
        TaskProject project = projectMapper.selectById(dto.getId());
        if (project == null) {
            throw new BusinessException("项目不存在");
        }
        BeanUtils.copyProperties(dto, project);
        projectMapper.updateById(project);
    }

    /**
     * 删除项目
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteProject(Long id) {
        TaskProject project = projectMapper.selectById(id);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }
        projectMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMember(Long projectId, Long empId, String role) {
        // 校验项目存在
        TaskProject project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }
        // 检查是否已是成员
        Long count = memberMapper.selectCount(new LambdaQueryWrapper<TaskProjectMember>()
                .eq(TaskProjectMember::getProjectId, projectId)
                .eq(TaskProjectMember::getEmpId, empId));
        if (count > 0) {
            throw new BusinessException("该成员已在项目中");
        }
        TaskProjectMember member = new TaskProjectMember();
        member.setProjectId(projectId);
        member.setEmpId(empId);
        member.setRole(role != null ? role : "MEMBER");
        memberMapper.insert(member);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long projectId, Long empId) {
        LambdaQueryWrapper<TaskProjectMember> wrapper = new LambdaQueryWrapper<TaskProjectMember>()
                .eq(TaskProjectMember::getProjectId, projectId)
                .eq(TaskProjectMember::getEmpId, empId);
        TaskProjectMember member = memberMapper.selectOne(wrapper);
        if (member == null) {
            throw new BusinessException("成员不在项目中");
        }
        memberMapper.deleteById(member.getId());
    }

    @Override
    public List<TaskProjectMember> getMembers(Long projectId) {
        return memberMapper.selectList(new LambdaQueryWrapper<TaskProjectMember>()
                .eq(TaskProjectMember::getProjectId, projectId)
                .orderByDesc(TaskProjectMember::getJoinedAt));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProgress(Long projectId) {
        // 查询项目下所有任务进度，取平均值
        List<TaskItem> items = itemMapper.selectList(new LambdaQueryWrapper<TaskItem>()
                .eq(TaskItem::getProjectId, projectId)
                .isNull(TaskItem::getParentTaskId));
        if (items.isEmpty()) {
            return;
        }
        int total = items.stream().mapToInt(i -> i.getProgress() != null ? i.getProgress() : 0).sum();
        int avg = total / items.size();

        TaskProject project = new TaskProject();
        project.setId(projectId);
        project.setProgress(avg);
        projectMapper.updateById(project);
    }

    // ============ 内部方法 ============

    private String getStatusName(String status) {
        if (status == null) return "";
        return switch (status) {
            case "PLANNING" -> "规划中";
            case "IN_PROGRESS" -> "进行中";
            case "COMPLETED" -> "已完成";
            case "ON_HOLD" -> "已暂停";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }

    private String getMemberRoleName(String role) {
        if (role == null) return "";
        return switch (role) {
            case "OWNER" -> "拥有者";
            case "ADMIN" -> "管理员";
            case "MEMBER" -> "成员";
            default -> role;
        };
    }
}
