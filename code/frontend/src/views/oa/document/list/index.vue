<template>
  <div>
    <div class="flex items-center justify-between mb-4">
      <el-input v-model="keyword" placeholder="搜索文档名称" clearable class="w-64" prefix-icon="Search" @input="fetchData" />
      <el-button type="primary" :icon="Upload" @click="uploadDialogVisible = true">上传文档</el-button>
    </div>
    <el-card>
      <el-table :data="docList" stripe>
        <el-table-column label="文档名称">
          <template #default="{ row }">
            <div class="flex items-center gap-2">
              <el-icon><Document /></el-icon>
              <span>{{ row.docName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="文件类型" prop="fileType" width="100" />
        <el-table-column label="上传者ID" prop="uploaderId" width="100" />
        <el-table-column label="上传时间" width="180">
          <template #default="{ row }">{{ row.createTime?.replace("T", " ").substring(0, 19) || "-" }}</template>
        </el-table-column>
        <el-table-column label="文件大小" width="100">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleDownload(row)">下载</el-button>
            <el-button type="danger" link @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="flex justify-end mt-4">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total, prev, pager, next" @current-change="fetchData" />
      </div>
    </el-card>

    <el-dialog v-model="uploadDialogVisible" title="上传文档" width="500px">
      <el-upload ref="uploadRef" :auto-upload="false" :limit="1" :on-change="handleFileChange" drag>
        <el-icon :size="48" class="text-[#909399]"><UploadFilled /></el-icon>
        <div class="text-sm text-[#606266]">将文件拖到此处，或点击上传</div>
      </el-upload>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Upload } from "@element-plus/icons-vue";
import { getDocumentPage, uploadDocument, deleteDocument, downloadDocument } from "@/api/document";
import { useUserStore } from "@/store/user";
import type { Document } from "@/types/api";

const userStore = useUserStore();
const keyword = ref("");
const docList = ref<Document[]>([]);
const page = ref(1);
const pageSize = ref(10);
const total = ref(0);
const uploadDialogVisible = ref(false);
const uploading = ref(false);
const selectedFile = ref<File | null>(null);

const formatSize = (bytes?: number) => {
  if (!bytes) return "-";
  if (bytes < 1024) return bytes + "B";
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + "KB";
  return (bytes / (1024 * 1024)).toFixed(1) + "MB";
};

const fetchData = async () => {
  try {
    const params: any = { pageNum: page.value, pageSize: pageSize.value };
    if (keyword.value) params.keyword = keyword.value;
    const res = await getDocumentPage(params as any);
    if (res.data?.list) { docList.value = res.data.list; total.value = res.data.total || 0; }
  } catch {}
};

const handleFileChange = (file: { raw?: File }) => { selectedFile.value = file.raw ?? null; };

const handleUpload = async () => {
  if (!selectedFile.value) { ElMessage.warning("请选择文件"); return; }
  uploading.value = true;
  try {
    const empId = userStore.userInfo?.empId || userStore.userInfo?.id || 0;
    await uploadDocument(selectedFile.value, empId);
    ElMessage.success("上传成功");
    uploadDialogVisible.value = false;
    selectedFile.value = null;
    await fetchData();
  } catch (e: any) { ElMessage.error(e.message || "上传失败"); }
  finally { uploading.value = false; }
};

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm("确定删除该文档？", "提示", { type: "warning" });
    await deleteDocument(id);
    ElMessage.success("删除成功");
    await fetchData();
  } catch {}
};

const handleDownload = async (row: Document) => {
  try {
    const res: any = await downloadDocument(row.id!);
    if (!res || (res.type && res.type.includes("json"))) {
      ElMessage.error("下载失败，文件不存在或已被删除");
      return;
    }
    const blob = res instanceof Blob ? res : new Blob([res.data ?? res]);
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = row.docName || "document";
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
  } catch {
    ElMessage.error("下载失败");
  }
};

onMounted(fetchData);
</script>
