<template>
  <div>
    <div class="flex items-center justify-between mb-4">
      <span class="text-lg font-medium">角色管理</span>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新增角色</el-button>
    </div>
    <el-card>
      <el-table :data="roleList" stripe style="width: 100%">
        <el-table-column label="角色名称" prop="name" />
        <el-table-column label="角色标识" prop="code" />
        <el-table-column label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? "启用" : "禁用" }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" />
        <el-table-column label="创建时间" prop="createTime" />
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除该角色?" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑角色' : '新增角色'" width="500px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="角色名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="角色标识"><el-input v-model="form.code" placeholder="如 ADMIN、USER" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" /></el-form-item>
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
import { Plus } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import { getRolePage, addRole, updateRole, deleteRole } from "@/api/system";

const formRef = ref<FormInstance>();
const formRules: FormRules = {
  name: [{ required: true, message: "请输入角色名称", trigger: "blur" }],
  code: [{ required: true, message: "请输入角色标识", trigger: "blur" }]
};

const roleList = ref<any[]>([]);
const dialogVisible = ref(false);
const saving = ref(false);
const form = reactive({ id: undefined as number | undefined, name: "", code: "", remark: "", status: 1 });

const fetchRoles = async () => {
  try {
    const r: any = await getRolePage();
    if (r.data?.list) roleList.value = r.data.list;
    else if (Array.isArray(r.data)) roleList.value = r.data;
  } catch { /* ignore */ }
};

const openDialog = (row?: any) => {
  if (row) {
    Object.assign(form, { id: row.id, name: row.name, code: row.code, remark: row.remark || "", status: row.status ?? 1 });
  } else {
    Object.assign(form, { id: undefined, name: "", code: "", remark: "", status: 1 });
  }
  dialogVisible.value = true;
};

const handleSave = async () => {
  if (!formRef.value) return;
  await formRef.value.validate();
  saving.value = true;
  try {
    const payload: any = {
      ...form,
      roleName: form.name,
      roleKey: form.code
    };
    if (form.id) {
      payload.id = form.id;
      await updateRole(payload);
    } else {
      await addRole(payload);
    }
    ElMessage.success("保存成功");
    dialogVisible.value = false;
    fetchRoles();
  } catch {
    ElMessage.error("保存失败");
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (id: number) => {
  try {
    await deleteRole(id);
    ElMessage.success("删除成功");
    fetchRoles();
  } catch {
    ElMessage.error("删除失败");
  }
};

onMounted(fetchRoles);
</script>
