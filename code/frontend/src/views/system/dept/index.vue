<template>
  <div>
    <div class="flex items-center justify-between mb-4 flex-wrap gap-3">
      <div class="flex items-center gap-3">
        <el-input v-model="searchKey" placeholder="搜索部门名称" clearable class="w-56" :prefix-icon="Search" @keyup.enter="handleSearch" @clear="handleSearch" />
        <el-select v-model="searchStatus" placeholder="状态" clearable class="w-32" @change="handleSearch">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button @click="toggleExpandAll">{{ isAllExpanded ? "全部折叠" : "全部展开" }}</el-button>
      </div>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新增部门</el-button>
    </div>
    <el-card>
      <el-table
        ref="tableRef"
        :data="filteredDeptList"
        row-key="id"
        :tree-props="{ children: 'children' }"
        :default-expand-all="isAllExpanded"
        v-loading="loading"
        stripe
        style="width: 100%"
      >
        <el-table-column label="部门名称" min-width="220">
          <template #default="{ row }">
            <span>{{ row.deptName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="负责人" prop="leader" width="120" />
        <el-table-column label="联系电话" prop="phone" width="140" />
        <el-table-column label="排序" prop="sort" width="80" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? "启用" : "禁用" }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="170" />
        <el-table-column label="操作" width="200" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDialog(undefined, row.id)">新增子部门</el-button>
            <el-button type="primary" link @click="openDialog(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无部门数据" />
        </template>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑部门' : '新增部门'" width="500px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="上级部门">
          <el-tree-select
            v-model="form.parentId"
            :data="deptTreeData"
            :props="{ label: 'deptName', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            placeholder="无（顶级部门）"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="部门名称" prop="deptName"><el-input v-model="form.deptName" placeholder="请输入部门名称" /></el-form-item>
        <el-form-item label="负责人"><el-input v-model="form.leader" placeholder="请输入负责人" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.phone" placeholder="请输入联系电话" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
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
import type { FormInstance, FormRules } from "element-plus";
import { Plus, Search } from "@element-plus/icons-vue";
import { getDeptTree, addDept, updateDept, deleteDept } from "@/api/dept";

const loading = ref(false);
const deptList = ref<any[]>([]);
const dialogVisible = ref(false);
const isEdit = ref(false);
const submitting = ref(false);
const formRef = ref<FormInstance>();
const tableRef = ref<any>();
const isAllExpanded = ref(true);
const searchKey = ref("");
const searchStatus = ref<number | undefined>(undefined);

const form = reactive({
  id: undefined as number | undefined,
  deptName: "",
  leader: "",
  phone: "",
  sort: 0,
  parentId: undefined as number | undefined,
  status: 1
});

const formRules = reactive<FormRules>({
  deptName: [{ required: true, message: "请输入部门名称", trigger: "blur" }]
});

const deptTreeData = computed(() => deptList.value);

const filteredDeptList = computed(() => {
  let list = deptList.value;
  if (searchStatus.value !== undefined) {
    list = filterByStatus(list, searchStatus.value);
  }
  if (searchKey.value.trim()) {
    list = filterByName(list, searchKey.value.trim().toLowerCase());
  }
  return list;
});

const filterByStatus = (list: any[], status: number): any[] => {
  return list
    .map(item => {
      const children = item.children?.length ? filterByStatus(item.children, status) : [];
      if (item.status === status || children.length > 0) {
        return { ...item, children: children.length > 0 ? children : item.children?.length ? children : undefined };
      }
      return null;
    })
    .filter(Boolean) as any[];
};

const filterByName = (list: any[], key: string): any[] => {
  return list
    .map(item => {
      const children = item.children?.length ? filterByName(item.children, key) : [];
      if (item.deptName?.toLowerCase().includes(key) || children.length > 0) {
        return { ...item, children: children.length > 0 ? children : item.children?.length ? children : undefined };
      }
      return null;
    })
    .filter(Boolean) as any[];
};

// After backend getDeptTree returns flat list, frontend builds tree
const buildTree = (list: any[], parentId = 0): any[] => {
  return list
    .filter(item => (item.parentId || 0) === parentId)
    .map(item => {
      const children = buildTree(list, item.id);
      return children.length > 0 ? { ...item, children } : { ...item };
    });
};

const toggleExpandAll = () => {
  isAllExpanded.value = !isAllExpanded.value;
  // Force re-render by swapping data
  const data = [...deptList.value];
  deptList.value = [];
  setTimeout(() => { deptList.value = data; }, 0);
};

const handleSearch = () => {
  // Filtering is reactive via computed
};

const openDialog = (row?: any, parentId?: number) => {
  isEdit.value = !!row;
  if (row) {
    Object.assign(form, {
      id: row.id,
      deptName: row.deptName || "",
      leader: row.leader || "",
      phone: row.phone || "",
      sort: row.sort ?? 0,
      parentId: row.parentId,
      status: row.status ?? 1
    });
  } else {
    Object.assign(form, { id: undefined, deptName: "", leader: "", phone: "", sort: 0, parentId: parentId, status: 1 });
  }
  dialogVisible.value = true;
};

const fetchData = async () => {
  loading.value = true;
  try {
    const r: any = await getDeptTree();
    if (r.data && Array.isArray(r.data)) {
      const hasChildren = r.data.some((item: any) => item.children && item.children.length > 0);
      deptList.value = hasChildren ? r.data : buildTree(r.data);
    }
  } catch { /* error handled by interceptor */ }
  finally { loading.value = false; }
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
  } catch {
    return;
  }
  submitting.value = true;
  try {
    const data = {
      id: form.id,
      deptName: form.deptName,
      leader: form.leader,
      phone: form.phone,
      sort: form.sort,
      parentId: form.parentId,
      status: form.status
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
  try {
    await ElMessageBox.confirm("确定删除该部门？删除后不可恢复。", "提示", { type: "warning" });
    await deleteDept(id);
    ElMessage.success("删除成功");
    await fetchData();
  } catch { /* cancelled */ }
};

onMounted(fetchData);
</script>
