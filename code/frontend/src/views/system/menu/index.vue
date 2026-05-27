<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <span class="text-base font-semibold text-[#303133]">菜单权限</span>
      </template>

      <div class="mb-4 flex justify-between">
        <el-input v-model="searchKey" placeholder="搜索菜单名称" style="width: 240px" clearable @clear="fetchMenuTree" @keyup.enter="fetchMenuTree">
          <template #append>
            <el-button @click="fetchMenuTree">
              <el-icon><Search /></el-icon>
            </el-button>
          </template>
        </el-input>
        <el-button type="primary" @click="openDialog()">新增菜单</el-button>
      </div>

      <el-table :data="menuTree" v-loading="loading" row-key="id" :tree-props="{ children: 'children' }" :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
        <el-table-column prop="name" label="菜单名称" min-width="150" />
        <el-table-column prop="path" label="路由路径" min-width="150" />
        <el-table-column prop="component" label="组件路径" min-width="180" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.type === 0 ? '' : row.type === 1 ? 'success' : 'warning'" size="small">
              {{ row.type === 0 ? "目录" : row.type === 1 ? "菜单" : "按钮" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template #default="{ row }">
            <el-button v-if="row.type !== 2" type="primary" link size="small" @click="openDialog(undefined, row.id)">新增子菜单</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑菜单' : '新增菜单'" width="560px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级菜单">
          <el-input v-model="form.parentId" placeholder="上级菜单ID（空为顶级）" />
        </el-form-item>
        <el-form-item label="菜单类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="0">目录</el-radio>
            <el-radio :value="1">菜单</el-radio>
            <el-radio :value="2">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入菜单名称" />
        </el-form-item>
        <el-form-item v-if="form.type !== 2" label="路由路径" prop="path">
          <el-input v-model="form.path" placeholder="请输入路由路径" />
        </el-form-item>
        <el-form-item v-if="form.type === 1" label="组件路径" prop="component">
          <el-input v-model="form.component" placeholder="请输入组件路径" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="权限标识">
          <el-input v-model="form.permission" placeholder="请输入权限标识" />
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
import { Search } from "@element-plus/icons-vue";
import { getMenuTree, addMenu, updateMenu, deleteMenu } from "@/api/menu";

const loading = ref(false);
const menuTree = ref<any[]>([]);
const searchKey = ref("");

const fetchMenuTree = async () => {
  loading.value = true;
  try {
    const res: any = await getMenuTree();
    menuTree.value = res.data || [];
  } finally {
    loading.value = false;
  }
};

const dialogVisible = ref(false);
const saving = ref(false);
const formRef = ref<FormInstance>();
const form = reactive({
  id: undefined as number | undefined,
  parentId: undefined as number | undefined,
  type: 1,
  name: "",
  path: "",
  component: "",
  sort: 0,
  permission: ""
});
const rules = reactive<FormRules>({
  name: [{ required: true, message: "请输入菜单名称", trigger: "blur" }],
  type: [{ required: true, message: "请选择菜单类型", trigger: "change" }]
});

const openDialog = (row?: any, parentId?: number) => {
  if (row) {
    Object.assign(form, {
      id: row.id, parentId: row.parentId, type: row.type ?? 1,
      name: row.name, path: row.path || "", component: row.component || "",
      sort: row.sort || 0, permission: row.permission || ""
    });
  } else {
    Object.assign(form, {
      id: undefined, parentId: parentId, type: 1, name: "",
      path: "", component: "", sort: 0, permission: ""
    });
  }
  dialogVisible.value = true;
};

const handleSave = async () => {
  if (!formRef.value) return;
  await formRef.value.validate();
  saving.value = true;
  try {
    if (form.id) {
      await updateMenu(form);
    } else {
      await addMenu(form);
    }
    ElMessage.success("保存成功");
    dialogVisible.value = false;
    fetchMenuTree();
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (id: number) => {
  await deleteMenu(id);
  ElMessage.success("删除成功");
  fetchMenuTree();
};

onMounted(() => { fetchMenuTree(); });
</script>
