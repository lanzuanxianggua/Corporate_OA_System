package cn.oa.workflow.service;

import cn.oa.platform.common.exception.BizException;
import cn.oa.workflow.engine.WfEngine;
import cn.oa.workflow.entity.WfTask;
import cn.oa.workflow.mapper.WfTaskMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WfTaskServiceTest {

    @Mock private WfEngine engine;
    @Mock private WfTaskMapper taskMapper;

    private WfTaskService service;

    @BeforeEach
    void setUp() {
        service = new WfTaskService(engine, taskMapper);
    }

    @Test
    void shouldReturnPendingTasks() {
        WfTask task1 = new WfTask();
        task1.setId(1L);
        task1.setAssigneeId(100L);
        task1.setStatus("PENDING");

        WfTask task2 = new WfTask();
        task2.setId(2L);
        task2.setAssigneeId(100L);
        task2.setStatus("PENDING");

        when(taskMapper.findPendingByAssignee(100L)).thenReturn(List.of(task1, task2));

        List<WfTask> result = service.myPending(100L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);
        verify(taskMapper).findPendingByAssignee(100L);
    }

    @Test
    void shouldReturnEmptyWhenNoPendingTasks() {
        when(taskMapper.findPendingByAssignee(999L)).thenReturn(List.of());

        List<WfTask> result = service.myPending(999L);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnTaskById() {
        WfTask task = new WfTask();
        task.setId(1L);
        task.setStatus("PENDING");

        when(taskMapper.selectById(1L)).thenReturn(task);

        WfTask result = service.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void shouldThrowWhenTaskNotFound() {
        when(taskMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.getById(999L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("任务不存在");
    }

    @Test
    void shouldDelegateApproveToEngine() {
        service.approve(1L, 100L, "APPROVE", "同意");

        verify(engine).approve(1L, 100L, "APPROVE", "同意");
    }

    @Test
    void shouldDelegateRejectToEngine() {
        service.approve(2L, 100L, "REJECT", "不同意");

        verify(engine).approve(2L, 100L, "REJECT", "不同意");
    }
}
