<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[var(--oa-text)]">会议室管理</span>
          <el-button type="primary" @click="openDialog()">新增会议室</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe :header-cell-style="{ background: 'var(--oa-surface-soft)', color: 'var(--oa-muted)' }">
        <el-table-column prop="roomName" label="会议室名称" min-width="120" />
        <el-table-column prop="location" label="位置" min-width="120" />
        <el-table-column prop="capacity" label="容纳人数" width="100" align="center" />
        <el-table-column label="设备" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.equipment || "-" }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.available ? 'success' : 'danger'" size="small">{{ row.available ? "可用" : "占用" }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除?" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑会议室' : '新增会议室'" width="500px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="名称" prop="roomName">
          <el-input v-model="form.roomName" placeholder="请输入会议室名称" />
        </el-form-item>
        <el-form-item label="位置" prop="location">
          <el-input v-model="form.location" placeholder="请输入位置" />
        </el-form-item>
        <el-form-item label="容纳人数" prop="capacity">
          <el-input-number v-model="form.capacity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="设备">
          <el-input v-model="form.equipment" placeholder="请输入设备信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import { getRooms, addRoom, updateRoom, deleteRoom } from "@/api/meeting";

const loading = ref(false);
const tableData = ref<any[]>([]);

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await getRooms();
    tableData.value = res.data || [];
  } finally {
    loading.value = false;
  }
};

const dialogVisible = ref(false);
const saving = ref(false);
const formRef = ref<FormInstance>();
const form = reactive({ id: undefined as number | undefined, roomName: "", location: "", capacity: 10, equipment: "" });
const rules = reactive<FormRules>({
  roomName: [{ required: true, message: "请输入名称", trigger: "blur" }],
  location: [{ required: true, message: "请输入位置", trigger: "blur" }]
});

const openDialog = (row?: any) => {
  if (row) {
    Object.assign(form, { id: row.id, roomName: row.roomName, location: row.location, capacity: row.capacity || 10, equipment: row.equipment || "" });
  } else {
    Object.assign(form, { id: undefined, roomName: "", location: "", capacity: 10, equipment: "" });
  }
  dialogVisible.value = true;
};

const handleSave = async () => {
  if (!formRef.value) return;
  await formRef.value.validate();
  saving.value = true;
  try {
    if (form.id) {
      await updateRoom(form);
    } else {
      await addRoom(form);
    }
    ElMessage.success("保存成功");
    dialogVisible.value = false;
    fetchList();
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (id: number) => {
  try {
    await deleteRoom(id);
    ElMessage.success("删除成功");
    fetchList();
  } catch {
    ElMessage.error("删除失败");
  }
};

onMounted(() => { fetchList(); });
</script>
