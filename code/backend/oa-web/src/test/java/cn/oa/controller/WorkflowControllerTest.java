package cn.oa.controller;

import cn.oa.entity.WfCcRecord;
import cn.oa.entity.WfDelegation;
import cn.oa.entity.WfProcessDefinition;
import cn.oa.entity.WfTask;
import cn.oa.mapper.WfCcRecordMapper;
import cn.oa.service.DelegationService;
import cn.oa.service.WorkflowService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkflowController.class)
@DisplayName("工作流管理 - WorkflowController")
class WorkflowControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WorkflowService workflowService;

    @MockitoBean
    private WfCcRecordMapper ccRecordMapper;

    @MockitoBean
    private DelegationService delegationService;

    @Test
    @DisplayName("查询所有流程定义")
    void listDefinitions() throws Exception {
        WfProcessDefinition def = new WfProcessDefinition();
        def.setId(1L);
        def.setProcessName("请假审批");
        def.setProcessType("leave");
        def.setStatus("0");

        when(workflowService.listDefinitions()).thenReturn(List.of(def));

        mockMvc.perform(get("/api/workflow/definition/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].processName").value("请假审批"));
    }

    @Test
    @DisplayName("创建/更新流程定义")
    void saveDefinition() throws Exception {
        doNothing().when(workflowService).saveDefinition(any(WfProcessDefinition.class));

        WfProcessDefinition def = new WfProcessDefinition();
        def.setProcessName("出差审批");
        def.setProcessType("trip");
        def.setNodeConfig("[]");

        mockMvc.perform(post("/api/workflow/definition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(def)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(workflowService, times(1)).saveDefinition(any(WfProcessDefinition.class));
    }

    @Test
    @DisplayName("我的待办任务")
    void pendingTasks() throws Exception {
        WfTask task = new WfTask();
        task.setId(1L);
        task.setInstanceId(100L);
        task.setAssigneeId(1L);
        task.setStatus("0");

        IPage<WfTask> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(task));

        when(workflowService.myPendingTasks(1L, 1, 10)).thenReturn(page);

        mockMvc.perform(get("/api/workflow/task/pending")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .requestAttr("empId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].instanceId").value(100));
    }

    @Test
    @DisplayName("处理任务 - 通过")
    void handleTaskApprove() throws Exception {
        doNothing().when(workflowService).handleTask(anyLong(), anyLong(), any(Integer.class), anyString());

        Map<String, Object> params = Map.of("taskId", 1, "status", 1, "remark", "同意");

        mockMvc.perform(post("/api/workflow/task/handle")
                        .requestAttr("empId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(workflowService, times(1)).handleTask(1L, 1L, 1, "同意");
    }

    @Test
    @DisplayName("处理任务 - 使用action字段")
    void handleTaskWithAction() throws Exception {
        doNothing().when(workflowService).handleTask(anyLong(), anyLong(), any(Integer.class), any());

        Map<String, Object> params = Map.of("taskId", 2, "action", 2, "remark", "驳回");

        mockMvc.perform(post("/api/workflow/task/handle")
                        .requestAttr("empId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(workflowService, times(1)).handleTask(2L, 1L, 2, "驳回");
    }

    @Test
    @DisplayName("撤回申请")
    void withdraw() throws Exception {
        doNothing().when(workflowService).withdrawProcess(anyString(), anyLong(), anyLong());

        Map<String, Object> params = Map.of("businessType", "leave", "businessId", 100);

        mockMvc.perform(post("/api/workflow/withdraw")
                        .requestAttr("empId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(workflowService, times(1)).withdrawProcess("leave", 100L, 1L);
    }

    @Test
    @DisplayName("查询审批历史")
    void approvalHistory() throws Exception {
        WfTask task = new WfTask();
        task.setId(1L);
        task.setStatus("1");

        when(workflowService.getApprovalHistory("leave", 100L)).thenReturn(List.of(task));

        mockMvc.perform(get("/api/workflow/history")
                        .param("businessType", "leave")
                        .param("businessId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].status").value("1"));
    }

    @Test
    @DisplayName("查找待处理任务")
    void findTask() throws Exception {
        WfTask task = new WfTask();
        task.setId(5L);
        task.setAssigneeId(1L);

        when(workflowService.findPendingTask("leave", 100L, 1L)).thenReturn(task);

        mockMvc.perform(get("/api/workflow/task/find")
                        .param("businessType", "leave")
                        .param("businessId", "100")
                        .requestAttr("empId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(5));
    }

    @Test
    @DisplayName("转办任务")
    void transferTask() throws Exception {
        doNothing().when(workflowService).transferTask(anyLong(), anyLong(), anyLong(), any());

        Map<String, Object> params = Map.of("taskId", 1, "toAssigneeId", 2, "reason", "出差委托");

        mockMvc.perform(post("/api/workflow/task/transfer")
                        .requestAttr("empId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(workflowService, times(1)).transferTask(1L, 1L, 2L, "出差委托");
    }

    @Test
    @DisplayName("退回任务到指定节点")
    void returnTask() throws Exception {
        doNothing().when(workflowService).returnTask(anyLong(), anyLong(), anyString(), any());

        Map<String, Object> params = Map.of("taskId", 1, "returnTarget", "node_0", "remark", "退回修改");

        mockMvc.perform(post("/api/workflow/task/return")
                        .requestAttr("empId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(workflowService, times(1)).returnTask(1L, 1L, "node_0", "退回修改");
    }

    @Test
    @DisplayName("设置审批委托")
    void setDelegation() throws Exception {
        doNothing().when(delegationService).setDelegation(any(WfDelegation.class));

        WfDelegation delegation = new WfDelegation();
        delegation.setDelegateToId(2L);

        mockMvc.perform(post("/api/workflow/delegation/set")
                        .requestAttr("empId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(delegation)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(delegationService, times(1)).setDelegation(any(WfDelegation.class));
    }

    @Test
    @DisplayName("我的审批委托")
    void myDelegations() throws Exception {
        WfDelegation delegation = new WfDelegation();
        delegation.setId(1L);
        delegation.setDelegatorId(1L);
        delegation.setDelegateToId(2L);
        delegation.setStatus("0");

        when(delegationService.getMyDelegations(1L)).thenReturn(List.of(delegation));

        mockMvc.perform(get("/api/workflow/delegation/my")
                        .requestAttr("empId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].delegateToId").value(2));
    }

    @Test
    @DisplayName("取消审批委托")
    void cancelDelegation() throws Exception {
        doNothing().when(delegationService).cancelDelegation(anyLong(), anyLong());

        mockMvc.perform(post("/api/workflow/delegation/cancel/1")
                        .requestAttr("empId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(delegationService, times(1)).cancelDelegation(1L, 1L);
    }

    @Test
    @DisplayName("标记抄送已读")
    void readCc() throws Exception {
        WfCcRecord record = new WfCcRecord();
        record.setId(1L);
        record.setStatus("0");

        when(ccRecordMapper.selectById(1L)).thenReturn(record);
        when(ccRecordMapper.updateById(any(WfCcRecord.class))).thenReturn(1);

        mockMvc.perform(post("/api/workflow/cc/read/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(ccRecordMapper, times(1)).updateById(any(WfCcRecord.class));
    }
}
