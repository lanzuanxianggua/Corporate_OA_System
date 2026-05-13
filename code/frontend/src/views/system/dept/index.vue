<template>
  <div>
    <div class="flex items-center justify-between mb-4">
      <span class="text-lg font-medium">部门管理</span>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新增部门</el-button>
    </div>
    <el-card>
      <el-table :data="deptList" row-key="id" :tree-props="{ children: 'children' }" default-expand-all stripe>
        <el-table-column label="部门名称" min-width="200">
          <template #default="{ row }">{{ row.name || row.deptName }}</template>
        </el-table-column>
        <el-table-column label="负责人">
          <template #default="{ row }">{{ row.principal || row.leader || "-" }}</template>
        </el-table-column>
        <el-table-column label="联系电话" prop="phone" />
        <el-table-column label="排序" width="80">
          <template #default="{ row }">{{ row.sort ?? row.orderNum ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? "启用" : "禁用" }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="180" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDialog(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑部门' : '新增部门'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="部门名称"><el-input v-model="form.deptName" /></el-form-item>
        <el-form-item label="负责人"><el-input v-model="form.leader" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="上级部门">
          <el-tree-select v-model="form.parentId" :data="deptTreeData" :props="{ label: 'name', value: 'id', children: 'children' }" check-strictly clearable placeholder="无（顶级部门）" style="width: 100%" />
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
import { ref, reactive, computed, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus } from "@element-plus/icons-vue";
import { getDeptTree, addDept, updateDept, deleteDept } from "@/api/dept";

const deptList = ref<any[]>([]);
const dialogVisible = ref(false);
const isEdit = ref(false);
const submitting = ref(false);
const form = reactive({ id: undefined as number | undefined, deptName: "", leader: "", phone: "", sort: 0, parentId: undefined as number | undefined });

const deptTreeData = computed(() => deptList.value);

const openDialog = (row?: any) => {
  isEdit.value = !!row;
  if (row) {
    Object.assign(form, {
      id: row.id,
      deptName: row.name || row.deptName || "",
      leader: row.principal || row.leader || "",
      phone: row.phone || "",
      sort: row.sort ?? row.orderNum ?? 0,
      parentId: row.parentId
    });
  } else {
    Object.assign(form, { id: undefined, deptName: "", leader: "", phone: "", sort: 0, parentId: undefined });
  }
  dialogVisible.value = true;
};

const fetchData = async () => {
  try {
    const r: any = await getDeptTree();
    if (r.data) deptList.value = Array.isArray(r.data) ? r.data : [];
  } catch {}
};

const handleSubmit = async () => {
  submitting.value = true;
  try {
    const data = {
      id: form.id,
      deptName: form.deptName,
      leader: form.leader,
      phone: form.phone,
      sort: form.sort,
      parentId: form.parentId
    };
    if (isEdit.value) await updateDept(data);
    else await addDept(data);
    ElMessage.success("操作成功");
    dialogVisible.value = false;
    await fetchData();
  } catch (e: any) { ElMessage.error(e.message || "操作失败"); }
  finally { submitting.value = false; }
};

const handleDelete = async (id: number) => {
  try { await ElMessageBox.confirm("确定删除？", "提示", { type: "warning" }); await deleteDept(id); ElMessage.success("删除成功"); await fetchData(); } catch {}
};

onMounted(fetchData);
</script>
