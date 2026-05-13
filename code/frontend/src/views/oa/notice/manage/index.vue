<template>
  <div class="notice-manage-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>公告管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            发布公告
          </el-button>
        </div>
      </template>
      <el-table :data="tableData" stripe>
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="publisher" label="发布人" width="100" />
        <el-table-column prop="publishTime" label="发布时间" width="160" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '已发布' ? 'success' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑公告' : '发布公告'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入公告内容" />
        </el-form-item>
        <el-form-item label="紧急程度" prop="urgent">
          <el-select v-model="form.urgent" style="width: 100%">
            <el-option label="普通" value="普通" />
            <el-option label="重要" value="重要" />
            <el-option label="紧急" value="紧急" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";

const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(3);
const dialogVisible = ref(false);
const isEdit = ref(false);
const formRef = ref<FormInstance>();

const form = reactive({
  id: null as number | null,
  title: "",
  content: "",
  urgent: "普通"
});

const rules: FormRules = {
  title: [{ required: true, message: "请输入标题", trigger: "blur" }],
  content: [{ required: true, message: "请输入内容", trigger: "blur" }]
};

const tableData = ref([
  { id: 1, title: "关于2026年端午节放假安排的通知", publisher: "管理员", publishTime: "2026-05-10 10:00", status: "已发布" },
  { id: 2, title: "关于开展2026年度员工体检的通知", publisher: "人事部", publishTime: "2026-05-08 09:00", status: "已发布" },
  { id: 3, title: "关于启用新考勤系统的通知", publisher: "技术部", publishTime: "2026-05-05 14:00", status: "已发布" }
]);

const handleAdd = () => {
  isEdit.value = false;
  form.id = null;
  form.title = "";
  form.content = "";
  form.urgent = "普通";
  dialogVisible.value = true;
};

const handleEdit = (row: any) => {
  isEdit.value = true;
  form.id = row.id;
  form.title = row.title;
  form.content = row.content;
  form.urgent = "普通";
  dialogVisible.value = true;
};

const handleDelete = (row: any) => {
  ElMessageBox.confirm("确定要删除这条公告吗？", "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning"
  }).then(() => {
    const index = tableData.value.findIndex((item) => item.id === row.id);
    if (index > -1) {
      tableData.value.splice(index, 1);
      ElMessage.success("删除成功");
    }
  });
};

const handleSubmit = () => {
  formRef.value?.validate((valid) => {
    if (valid) {
      if (isEdit.value) {
        ElMessage.success("修改成功");
      } else {
        ElMessage.success("发布成功");
      }
      dialogVisible.value = false;
    }
  });
};
</script>

<style scoped lang="scss">
.notice-manage-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>