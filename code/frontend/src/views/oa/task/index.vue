<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[#303133]">任务协作</span>
          <div class="flex gap-2">
            <el-button type="primary" @click="openProject">新增项目</el-button>
            <el-button type="success" @click="openTask">新增任务</el-button>
          </div>
        </div>
      </template>
      <el-tabs v-model="activeTab" @tab-change="reload">
        <el-tab-pane label="项目" name="projects">
          <el-table :data="projects" v-loading="loading" stripe>
            <el-table-column prop="projectName" label="项目名称" min-width="180" />
            <el-table-column prop="ownerId" label="负责人" width="100" />
            <el-table-column prop="deptId" label="部门ID" width="100" />
            <el-table-column prop="status" label="状态" width="110" />
            <el-table-column prop="createTime" label="创建时间" width="180" />
            <el-table-column label="操作" width="140"><template #default="{ row }"><el-button link type="success" @click="projectStatus(row.id, 'ACTIVE')">启动</el-button><el-button link @click="projectStatus(row.id, 'CLOSED')">关闭</el-button></template></el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="任务" name="items">
          <el-table :data="items" v-loading="loading" stripe>
            <el-table-column prop="taskName" label="任务名称" min-width="180" />
            <el-table-column prop="projectId" label="项目ID" width="100" />
            <el-table-column prop="assigneeId" label="负责人" width="100" />
            <el-table-column prop="priority" label="优先级" width="100" />
            <el-table-column prop="progress" label="进度" width="140">
              <template #default="{ row }"><el-progress :percentage="row.progress || 0" /></template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="110" />
            <el-table-column label="操作" width="160"><template #default="{ row }"><el-button link type="success" @click="itemStatus(row.id, 'DONE')">完成</el-button><el-button link @click="setProgress(row.id)">进度</el-button></template></el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="projectVisible" title="新增项目" width="520px">
      <el-form :model="projectForm" label-width="90px">
        <el-form-item label="项目名称"><el-input v-model="projectForm.projectName" /></el-form-item>
        <el-form-item label="部门ID"><el-input-number v-model="projectForm.deptId" style="width: 100%" /></el-form-item>
        <el-form-item label="负责人ID"><el-input-number v-model="projectForm.ownerId" style="width: 100%" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="projectForm.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="projectVisible = false">取消</el-button><el-button type="primary" @click="saveProject">保存</el-button></template>
    </el-dialog>
    <el-dialog v-model="taskVisible" title="新增任务" width="520px">
      <el-form :model="taskForm" label-width="90px">
        <el-form-item label="项目ID"><el-input-number v-model="taskForm.projectId" style="width: 100%" /></el-form-item>
        <el-form-item label="任务名称"><el-input v-model="taskForm.taskName" /></el-form-item>
        <el-form-item label="负责人ID"><el-input-number v-model="taskForm.assigneeId" style="width: 100%" /></el-form-item>
        <el-form-item label="优先级"><el-select v-model="taskForm.priority" style="width: 100%"><el-option label="高" value="HIGH" /><el-option label="中" value="MEDIUM" /><el-option label="低" value="LOW" /></el-select></el-form-item>
        <el-form-item label="描述"><el-input v-model="taskForm.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="taskVisible = false">取消</el-button><el-button type="primary" @click="saveTask">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { taskCollabApi } from "@/api/businessModules";

const activeTab = ref("projects");
const loading = ref(false);
const projects = ref<any[]>([]);
const items = ref<any[]>([]);
function rows(data: any) { return data?.records || data?.list || []; }
async function reload() {
  loading.value = true;
  try {
    const res: any = activeTab.value === "items" ? await taskCollabApi.items({ pageNum: 1, pageSize: 30 }) : await taskCollabApi.projects({ pageNum: 1, pageSize: 30 });
    if (activeTab.value === "items") items.value = rows(res.data);
    else projects.value = rows(res.data);
  } finally { loading.value = false; }
}
const projectVisible = ref(false);
const projectForm = reactive<any>({ projectName: "", deptId: undefined, ownerId: undefined, description: "" });
function openProject() { Object.assign(projectForm, { projectName: "", deptId: undefined, ownerId: undefined, description: "" }); projectVisible.value = true; }
async function saveProject() { await taskCollabApi.createProject(projectForm); ElMessage.success("项目已保存"); projectVisible.value = false; reload(); }
async function projectStatus(id: number, status: string) { await taskCollabApi.projectStatus(id, status); ElMessage.success("项目状态已更新"); reload(); }
const taskVisible = ref(false);
const taskForm = reactive<any>({ projectId: undefined, taskName: "", assigneeId: undefined, priority: "MEDIUM", description: "" });
function openTask() { Object.assign(taskForm, { projectId: undefined, taskName: "", assigneeId: undefined, priority: "MEDIUM", description: "" }); taskVisible.value = true; }
async function saveTask() { await taskCollabApi.createItem(taskForm); ElMessage.success("任务已保存"); taskVisible.value = false; activeTab.value = "items"; reload(); }
async function itemStatus(id: number, status: string) { await taskCollabApi.itemStatus(id, status); ElMessage.success("任务状态已更新"); reload(); }
async function setProgress(id: number) {
  const result = await ElMessageBox.prompt("请输入进度 0-100", "更新进度", { inputValue: "50" });
  await taskCollabApi.itemProgress(id, Number(result.value));
  ElMessage.success("任务进度已更新");
  reload();
}
onMounted(reload);
</script>
