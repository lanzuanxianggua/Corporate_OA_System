<template>
  <div class="document-list-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>文档中心</span>
          <div class="header-actions">
            <el-input v-model="keyword" placeholder="搜索文档名称" style="width: 200px; margin-right: 10px" />
            <el-button type="primary">
              <el-icon><Upload /></el-icon>
              上传文档
            </el-button>
          </div>
        </div>
      </template>
      <el-table :data="tableData" stripe>
        <el-table-column prop="name" label="文档名称">
          <template #default="{ row }">
            <div class="doc-name">
              <el-icon><Document /></el-icon>
              {{ row.name }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="文件类型" width="100" />
        <el-table-column prop="uploader" label="上传者" width="100" />
        <el-table-column prop="uploadTime" label="上传时间" width="160" />
        <el-table-column prop="size" label="文件大小" width="100" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
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
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";

const keyword = ref("");
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(5);

const tableData = ref([
  { id: 1, name: "员工手册2026版.pdf", type: "PDF", uploader: "管理员", uploadTime: "2026-05-10 10:00", size: "2.5MB" },
  { id: 2, name: "考勤管理制度.docx", type: "DOCX", uploader: "人事部", uploadTime: "2026-05-08 14:30", size: "156KB" },
  { id: 3, name: "会议室使用规范.xlsx", type: "XLSX", uploader: "行政部", uploadTime: "2026-05-05 09:15", size: "89KB" },
  { id: 4, name: "公司组织架构图.vsdx", type: "VSDX", uploader: "管理员", uploadTime: "2026-04-28 16:00", size: "1.2MB" },
  { id: 5, name: "节假日安排通知.pdf", type: "PDF", uploader: "管理员", uploadTime: "2026-04-20 11:00", size: "520KB" }
]);

const handleDelete = (row: any) => {
  ElMessageBox.confirm("确定要删除这个文档吗？", "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning"
  }).then(() => {
    ElMessage.success("删除成功");
  });
};
</script>

<style scoped lang="scss">
.document-list-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .doc-name {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>