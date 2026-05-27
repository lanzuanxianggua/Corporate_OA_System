<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[#303133]">会议管理</span>
          <el-button type="primary" @click="openDialog()">发起会议</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
        <el-table-column prop="title" label="会议主题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="roomName" label="会议室" width="100" />
        <el-table-column label="开始时间" min-width="140">
          <template #default="{ row }">{{ formatTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" min-width="140">
          <template #default="{ row }">{{ formatTime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column prop="organizerName" label="发起人" width="90" />
        <el-table-column prop="participants" label="参会人数" width="90" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'warning' : row.status === '1' ? 'success' : 'info'" size="small">
              {{ row.status === '0' ? "待开始" : row.status === '1' ? "进行中" : "已结束" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ row }">
            <el-button v-if="row.status === '0'" type="danger" link size="small" @click="handleCancel(row)">取消</el-button>
            <span v-else class="text-[#c0c4cc] text-xs">-</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-4 flex justify-end">
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" background @change="fetchList" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" title="发起会议" width="560px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="会议主题" prop="title">
          <el-input v-model="form.title" placeholder="请输入会议主题" />
        </el-form-item>
        <el-form-item label="会议室" prop="roomId">
          <el-select v-model="form.roomId" placeholder="请选择会议室" style="width: 100%">
            <el-option v-for="room in rooms" :key="room.id" :label="room.roomName" :value="room.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker v-model="form.startTime" type="datetime" placeholder="请选择开始时间" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker v-model="form.endTime" type="datetime" placeholder="请选择结束时间" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import { getRooms, submitMeeting, getMeetingPage, cancelMeeting } from "@/api/meeting";

const loading = ref(false);
const tableData = ref<any[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const rooms = ref<any[]>([]);

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await getMeetingPage({ pageNum: pageNum.value, pageSize: pageSize.value });
    tableData.value = res.data?.list || [];
    total.value = res.data?.total || 0;
  } finally {
    loading.value = false;
  }
};

const fetchRooms = async () => {
  try {
    const res: any = await getRooms();
    rooms.value = res.data || [];
  } catch {}
};

const dialogVisible = ref(false);
const submitting = ref(false);
const formRef = ref<FormInstance>();
const form = reactive({ title: "", roomId: undefined as number | undefined, startTime: "", endTime: "", description: "" });
const rules = reactive<FormRules>({
  title: [{ required: true, message: "请输入会议主题", trigger: "blur" }],
  roomId: [{ required: true, message: "请选择会议室", trigger: "change" }],
  startTime: [{ required: true, message: "请选择开始时间", trigger: "change" }],
  endTime: [{ required: true, message: "请选择结束时间", trigger: "change" }]
});

const openDialog = () => {
  form.title = "";
  form.roomId = undefined;
  form.startTime = "";
  form.endTime = "";
  form.description = "";
  formRef.value?.resetFields();
  dialogVisible.value = true;
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  await formRef.value.validate();
  submitting.value = true;
  try {
    await submitMeeting(form);
    ElMessage.success("会议已创建");
    dialogVisible.value = false;
    fetchList();
  } finally {
    submitting.value = false;
  }
};

const handleCancel = async (row: any) => {
  await ElMessageBox.confirm("确定要取消该会议吗？", "提示", { type: "warning" });
  await cancelMeeting(row.id);
  ElMessage.success("已取消会议");
  fetchList();
};

const formatTime = (time?: string) => {
  if (!time) return "-";
  return time.replace("T", " ").substring(0, 16);
};

onMounted(() => { fetchList(); fetchRooms(); });
</script>
