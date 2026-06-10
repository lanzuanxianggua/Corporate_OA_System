<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[var(--oa-text)]">菜单权限</span>
          <div class="flex items-center gap-3">
            <el-button @click="toggleExpandAll">{{ isAllExpanded ? "全部折叠" : "全部展开" }}</el-button>
            <el-button type="primary" @click="openDialog()">新增菜单</el-button>
          </div>
        </div>
      </template>

      <div class="mb-4">
        <el-input v-model="searchKey" placeholder="搜索菜单名称" style="width: 240px" clearable :prefix-icon="Search" @clear="handleSearch" @keyup.enter="handleSearch" />
      </div>

      <el-table
        ref="tableRef"
        :data="filteredMenuTree"
        v-loading="loading"
        row-key="id"
        :tree-props="{ children: 'children' }"
        :default-expand-all="isAllExpanded"
        :header-cell-style="{ background: 'var(--oa-surface-soft)', color: 'var(--oa-muted)' }"
      >
        <el-table-column prop="name" label="菜单名称" min-width="180" />
        <el-table-column prop="path" label="路由路径" min-width="150" show-overflow-tooltip />
        <el-table-column prop="component" label="组件路径" min-width="180" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="(row.type === 0 ? 'info' : row.type === 1 ? 'success' : 'warning') as any" size="small">
              {{ row.type === 0 ? "目录" : row.type === 1 ? "菜单" : "按钮" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="权限标识" prop="permission" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="220" align="center">
          <template #default="{ row }">
            <el-button v-if="row.type !== 2" type="primary" link size="small" @click="openDialog(undefined, row.id)">新增子菜单</el-button>
            <el-button type="primary" link size="small" @click="openDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除该菜单？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无菜单数据" />
        </template>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑菜单' : '新增菜单'" width="560px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级菜单">
          <el-tree-select
            v-model="form.parentId"
            :data="menuTreeData"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            placeholder="无（顶级菜单）"
            style="width: 100%" />
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
          <el-input-number v-model="form.sort" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="form.type === 2" label="权限标识">
          <el-input v-model="form.permission" placeholder="请输入权限标识，如 system:user:add" />
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
import { ref, reactive, computed, onMounted } from "vue";
import { ElMessage } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import { Search } from "@element-plus/icons-vue";
import { getMenuTree, addMenu, updateMenu, deleteMenu } from "@/api/menu";

const loading = ref(false);
const menuTree = ref<any[]>([]);
const searchKey = ref("");
const isAllExpanded = ref(true);
const tableRef = ref<any>();

const menuTreeData = computed(() => menuTree.value);

const filteredMenuTree = computed(() => {
  if (!searchKey.value.trim()) return menuTree.value;
  return filterByName(menuTree.value, searchKey.value.trim().toLowerCase());
});

const filterByName = (list: any[], key: string): any[] => {
  return list
    .map(item => {
      const children = item.children?.length ? filterByName(item.children, key) : [];
      if (item.name?.toLowerCase().includes(key) || children.length > 0) {
        return { ...item, children: children.length > 0 ? children : item.children?.length ? children : undefined };
      }
      return null;
    })
    .filter(Boolean) as any[];
};

const fetchMenuTree = async () => {
  loading.value = true;
  try {
    const res: any = await getMenuTree();
    menuTree.value = res.data || [];
  } catch {
    ElMessage.error("获取菜单树失败");
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  // Filtering is reactive via computed
};

const toggleExpandAll = () => {
  isAllExpanded.value = !isAllExpanded.value;
  const data = [...menuTree.value];
  menuTree.value = [];
  setTimeout(() => { menuTree.value = data; }, 0);
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
  } catch {
    ElMessage.error("保存失败");
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (id: number) => {
  try {
    await deleteMenu(id);
    ElMessage.success("删除成功");
    fetchMenuTree();
  } catch {
    ElMessage.error("删除失败");
  }
};

onMounted(() => { fetchMenuTree(); });
</script>
