<template>
  <div>
    <div class="flex items-center justify-between mb-4">
      <span class="text-lg font-medium">角色管理</span>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新增角色</el-button>
    </div>
    <el-card>
      <el-table :data="roleList" stripe>
        <el-table-column label="角色名称" prop="name" />
        <el-table-column label="角色标识" prop="code" />
        <el-table-column label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? "启用" : "禁用" }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" />
        <el-table-column label="创建时间" prop="createTime" />
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="新增角色" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="角色名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="角色标识"><el-input v-model="form.code" placeholder="如 ADMIN、USER" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="dialogVisible = false">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { Plus } from "@element-plus/icons-vue";
import { getRolePage } from "@/api/system";

const roleList = ref<any[]>([]);
const dialogVisible = ref(false);
const form = reactive({ name: "", code: "", remark: "" });

const openDialog = () => { Object.assign(form, { name: "", code: "", remark: "" }); dialogVisible.value = true; };

onMounted(async () => {
  try { const r: any = await getRolePage(); if (r.data?.list) roleList.value = r.data.list; else if (Array.isArray(r.data)) roleList.value = r.data; } catch {}
});
</script>
