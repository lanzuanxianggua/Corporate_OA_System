package cn.oa.task.service;

import cn.oa.task.dto.TaskDependencyDTO;
import cn.oa.task.dto.TaskItemCreateDTO;
import cn.oa.task.dto.TaskItemQueryDTO;
import cn.oa.task.dto.TaskItemUpdateDTO;
import cn.oa.task.entity.TaskDependency;
import cn.oa.task.entity.TaskItem;
import cn.oa.task.vo.TaskItemVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface TaskItemService extends IService<TaskItem> {

    /**
     * 分页查询任务
     */
    IPage<TaskItemVO> pageQuery(TaskItemQueryDTO dto);

    /**
     * 创建任务
     */
    Long create(TaskItemCreateDTO dto, Long creatorId);

    /**
     * 更新任务
     */
    void updateItem(Long id, TaskItemUpdateDTO dto);

    /**
     * 更新任务状态
     */
    void updateStatus(Long id, String status);

    /**
     * 删除任务
     */
    void deleteItem(Long id);

    /**
     * 查询子任务
     */
    List<TaskItemVO> getSubtasks(Long parentId);

    /**
     * 获取甘特图数据
     */
    Map<String, Object> getGanttData(Long projectId);

    /**
     * 检查循环依赖
     */
    boolean checkDependency(Long taskId, Long dependsOnTaskId);

    /**
     * 添加依赖
     */
    void addDependency(TaskDependencyDTO dto);

    /**
     * 删除依赖
     */
    void removeDependency(Long id);

    /**
     * 获取任务详情
     */
    TaskItemVO getDetail(Long id);
}
