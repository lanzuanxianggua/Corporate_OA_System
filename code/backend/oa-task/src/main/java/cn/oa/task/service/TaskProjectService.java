package cn.oa.task.service;

import cn.oa.task.dto.TaskProjectCreateDTO;
import cn.oa.task.dto.TaskProjectUpdateDTO;
import cn.oa.task.entity.TaskProject;
import cn.oa.task.entity.TaskProjectMember;
import cn.oa.task.vo.TaskProjectVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface TaskProjectService extends IService<TaskProject> {

    /**
     * 分页查询项目列表
     */
    IPage<TaskProject> pageList(Integer pageNum, Integer pageSize, String name, String status, Long ownerId);

    /**
     * 获取项目详情（含成员信息）
     */
    TaskProjectVO getDetail(Long id);

    /**
     * 创建项目
     */
    Long create(TaskProjectCreateDTO dto, Long ownerId);

    /**
     * 更新项目
     */
    void update(TaskProjectUpdateDTO dto);

    /**
     * 删除项目
     */
    void deleteProject(Long id);

    /**
     * 添加项目成员
     */
    void addMember(Long projectId, Long empId, String role);

    /**
     * 移除项目成员
     */
    void removeMember(Long projectId, Long empId);

    /**
     * 获取项目成员列表
     */
    List<TaskProjectMember> getMembers(Long projectId);

    /**
     * 更新项目进度（汇总所有子任务进度均值）
     */
    void updateProgress(Long projectId);
}
