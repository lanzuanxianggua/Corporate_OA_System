<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[var(--oa-text)]">岗位管理</span>
          <el-button type="primary" @click="openDialog()">新增岗位</el-button>
        </div>
      </template>

      <div class="mb-4">
        <el-input v-model="searchKey" placeholder="搜索岗位名称/编码" style="width: 280px" clearable @clear="fetchList" @keyup.enter="fetchList">
          <template #append>
            <el-button @click="fetchList">
              <el-icon><Search /></el-icon>
            </el-button>
          </template>
        </el-input>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe :header-cell-style="{ background: 'var(--oa-surface-soft)', color: 'var(--oa-muted)' }">
        <el-table-column prop="postName" label="岗位名称" min-width="120" />
        <el-table-column prop="postCode" label="岗位编码" min-width="120" />
        <el-table-column prop="postSort" label="排序" width="80" align="center" />
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

      <div class="mt-4 flex justify-end">
        <OaPagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" @change="fetchList" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑岗位' : '新增岗位'" width="500px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="岗位名称" prop="postName">
          <el-input v-model="form.postName" placeholder="请输入岗位名称" />
        </el-form-item>
        <el-form-item label="岗位编码" prop="postCode">
          <el-input v-model="form.postCode" placeholder="请输入岗位编码" />
        </el-form-item>
        <el-form-item label="排序" prop="postSort">
          <el-input-number v-model="form.postSort" :min="0" />
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
import { getPostPage, addPost, updatePost, deletePost } from "@/api/post";

const loading = ref(false);
const tableData = ref<any[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const searchKey = ref("");

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await getPostPage({ pageNum: pageNum.value, pageSize: pageSize.value, postName: searchKey.value || undefined });
    tableData.value = res.data?.list || [];
    total.value = res.data?.total || 0;
  } finally {
    loading.value = false;
  }
};

const dialogVisible = ref(false);
const saving = ref(false);
const formRef = ref<FormInstance>();
const form = reactive({ id: undefined as number | undefined, postName: "", postCode: "", postSort: 0 });
const rules = reactive<FormRules>({
  postName: [{ required: true, message: "请输入岗位名称", trigger: "blur" }],
  postCode: [{ required: true, message: "请输入岗位编码", trigger: "blur" }]
});

const openDialog = (row?: any) => {
  if (row) {
    Object.assign(form, { id: row.id, postName: row.postName, postCode: row.postCode, postSort: row.postSort || 0 });
  } else {
    Object.assign(form, { id: undefined, postName: "", postCode: "", postSort: 0 });
  }
  dialogVisible.value = true;
};

const handleSave = async () => {
  if (!formRef.value) return;
  await formRef.value.validate();
  saving.value = true;
  try {
    if (form.id) {
      await updatePost(form);
    } else {
      await addPost(form);
    }
    ElMessage.success("保存成功");
    dialogVisible.value = false;
    fetchList();
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (id: number) => {
  await deletePost(id);
  ElMessage.success("删除成功");
  fetchList();
};

onMounted(() => { fetchList(); });
</script>
