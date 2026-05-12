<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage } from "element-plus";
import {
  getDocumentPage,
  downloadDocument,
  getDocumentCategories
} from "@/api/oa/document";

defineOptions({ name: "OaDocumentList" });

/** 文档分类 */
interface Category {
  id: number;
  name: string;
  children?: Category[];
}

/** 文档记录 */
interface DocumentRecord {
  id: number;
  docName: string;
  description: string;
  createTime: string;
  downloadCount: number;
  categoryId: number;
  categoryName: string;
}

const loading = ref(false);
const categories = ref<Category[]>([]);
const documents = ref<DocumentRecord[]>([]);
const activeCategory = ref<number | null>(null);

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
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
    const params: any = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    };
    if (activeCategory.value !== null) {
      params.categoryId = activeCategory.value;
    }
    const res = await getDocumentPage(params);
    const data = res.data ?? res;
    documents.value = data.list ?? data.records ?? [];
    pagination.total = data.total ?? 0;
  } catch {
    ElMessage.error("获取文档列表失败");
  } finally {
    loading.value = false;
  }
}

/** 选择分类 */
function handleCategorySelect(categoryId: number | null) {
  activeCategory.value = categoryId;
  pagination.pageNum = 1;
  fetchDocuments();
}

/** 下载文档 */
async function handleDownload(row: DocumentRecord) {
  try {
    const res = await downloadDocument(row.id);
    const blob = new Blob([res], { type: "application/octet-stream" });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = row.docName;
    link.click();
    window.URL.revokeObjectURL(url);
    ElMessage.success("下载成功");
  } catch {
    ElMessage.error("下载失败");
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
  <div class="oa-document-list">
    <el-row :gutter="16">
      <!-- 左侧分类侧边栏 -->
      <el-col :span="5">
        <el-card shadow="hover" class="category-card">
          <template #header>
            <span class="card-title">文档分类</span>
          </template>
          <el-menu
            :default-active="String(activeCategory)"
            @select="(index: string) => handleCategorySelect(index === 'all' ? null : Number(index))"
          >
            <el-menu-item index="all" @click="handleCategorySelect(null)">
              <span>全部分类</span>
            </el-menu-item>
            <el-menu-item
              v-for="cat in categories"
              :key="cat.id"
              :index="String(cat.id)"
            >
              <span>{{ cat.name }}</span>
            </el-menu-item>
          </el-menu>
        </el-card>
      </el-col>

      <!-- 右侧文档列表 -->
      <el-col :span="19">
        <el-card shadow="hover">
          <template #header>
            <span class="card-title">文档浏览</span>
          </template>
          <el-table
            :data="documents"
            stripe
            v-loading="loading"
            empty-text="暂无文档"
            style="width: 100%"
          >
            <el-table-column prop="docName" label="文档名称" min-width="200" show-overflow-tooltip />
            <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
            <el-table-column prop="createTime" label="上传时间" width="180" align="center" />
            <el-table-column prop="downloadCount" label="下载次数" width="100" align="center" />
            <el-table-column label="操作" width="100" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="handleDownload(row)">
                  下载
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
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.oa-document-list {
  padding: 20px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.category-card .el-menu {
  border-right: none;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
