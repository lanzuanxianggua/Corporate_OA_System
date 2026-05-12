<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadFile } from "element-plus";
import {
  getDocumentPage,
  uploadDocument,
  deleteDocument,
  getDocumentCategories
} from "@/api/oa/document";

defineOptions({ name: "OaDocumentManage" });

/** 文档分类 */
interface Category {
  id: number;
  name: string;
}

/** 文档记录 */
interface DocumentRecord {
  id: number;
  docName: string;
  categoryName: string;
  downloadCount: number;
  createTime: string;
  categoryId: number;
}

const loading = ref(false);
const documents = ref<DocumentRecord[]>([]);
const categories = ref<Category[]>([]);
const dialogVisible = ref(false);
const uploadLoading = ref(false);
const formRef = ref<FormInstance>();
const fileList = ref<UploadFile[]>([]);

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
});

const form = reactive({
  docName: "",
  categoryId: undefined as number | undefined,
  file: null as File | null
});

const rules = reactive<FormRules>({
  docName: [{ required: true, message: "请输入文档名称", trigger: "blur" }],
  categoryId: [{ required: true, message: "请选择文档分类", trigger: "change" }],
  file: [{ required: true, message: "请上传文件", trigger: "change" }]
});

/** 加载文档分类 */
async function fetchCategories() {
  try {
    const res = await getDocumentCategories();
    categories.value = res.data ?? res ?? [];
  } catch {
    ElMessage.error("获取文档分类失败");
  }
}

/** 加载文档列表 */
async function fetchDocuments() {
  loading.value = true;
  try {
    const res = await getDocumentPage({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    });
    const data = res.data ?? res;
    documents.value = data.list ?? data.records ?? [];
    pagination.total = data.total ?? 0;
  } catch {
    ElMessage.error("获取文档列表失败");
  } finally {
    loading.value = false;
  }
}

/** 打开上传弹窗 */
function handleOpenDialog() {
  form.docName = "";
  form.categoryId = undefined;
  form.file = null;
  fileList.value = [];
  dialogVisible.value = true;
}

/** 文件选择变化 */
function handleFileChange(file: UploadFile) {
  form.file = file.raw as File;
  fileList.value = [file];
  if (!form.docName && file.name) {
    form.docName = file.name.replace(/\.[^/.]+$/, "");
  }
}

/** 文件移除 */
function handleFileRemove() {
  form.file = null;
  fileList.value = [];
}

/** 提交上传 */
async function handleUpload() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  uploadLoading.value = true;
  try {
    const formData = new FormData();
    formData.append("docName", form.docName);
    formData.append("categoryId", String(form.categoryId));
    if (form.file) {
      formData.append("file", form.file);
    }
    await uploadDocument(formData);
    ElMessage.success("上传成功");
    dialogVisible.value = false;
    fetchDocuments();
  } catch {
    ElMessage.error("上传失败");
  } finally {
    uploadLoading.value = false;
  }
}

/** 删除文档 */
async function handleDelete(row: DocumentRecord) {
  await ElMessageBox.confirm(`确认删除文档「${row.docName}」？`, "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning"
  });
  try {
    await deleteDocument(row.id);
    ElMessage.success("删除成功");
    fetchDocuments();
  } catch {
    ElMessage.error("删除失败");
  }
}

/** 分页变化 */
function handlePageChange(pageNum: number) {
  pagination.pageNum = pageNum;
  fetchDocuments();
}

function handleSizeChange(pageSize: number) {
  pagination.pageSize = pageSize;
  pagination.pageNum = 1;
  fetchDocuments();
}

onMounted(() => {
  fetchCategories();
  fetchDocuments();
});
</script>

<template>
  <div class="oa-document-manage">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span class="card-title">文档管理</span>
          <el-button type="primary" @click="handleOpenDialog">上传文档</el-button>
        </div>
      </template>

      <el-table
        :data="documents"
        stripe
        v-loading="loading"
        empty-text="暂无文档"
        style="width: 100%"
      >
        <el-table-column prop="docName" label="文档名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="所属分类" width="150" align="center" />
        <el-table-column prop="downloadCount" label="下载次数" width="100" align="center" />
        <el-table-column prop="createTime" label="上传时间" width="180" align="center" />
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" link size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- 上传文档弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="上传文档"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="文档名称" prop="docName">
          <el-input v-model="form.docName" placeholder="请输入文档名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="文档分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option
              v-for="cat in categories"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="上传文件" prop="file">
          <el-upload
            :auto-upload="false"
            :limit="1"
            :file-list="fileList"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
          >
            <el-button type="primary">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">支持常见文档格式，单个文件不超过 50MB</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploadLoading" @click="handleUpload">
          确认上传
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.oa-document-manage {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
