<template>
  <div class="h-full">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[#303133]">知识库</span>
          <div class="flex gap-2">
            <el-input v-model="keyword" placeholder="搜索标题" clearable style="width: 220px" @keyup.enter="reload" />
            <el-button @click="reload">搜索</el-button>
            <el-button @click="openCategory">新增分类</el-button>
            <el-button type="primary" @click="openEntry">新增条目</el-button>
          </div>
        </div>
      </template>
      <el-table :data="entries" v-loading="loading" stripe>
        <el-table-column prop="title" label="标题" min-width="220" />
        <el-table-column prop="categoryId" label="分类ID" width="100" />
        <el-table-column prop="status" label="状态" width="110" />
        <el-table-column prop="viewCount" label="浏览" width="90" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="success" @click="publish(row.id)">发布</el-button>
            <el-button link @click="archive(row.id)">归档</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="categoryVisible" title="新增知识分类" width="420px">
      <el-form :model="categoryForm" label-width="90px">
        <el-form-item label="分类名称"><el-input v-model="categoryForm.categoryName" /></el-form-item>
        <el-form-item label="父级ID"><el-input-number v-model="categoryForm.parentId" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="categoryVisible = false">取消</el-button><el-button type="primary" @click="saveCategory">保存</el-button></template>
    </el-dialog>
    <el-dialog v-model="entryVisible" title="新增知识条目" width="680px">
      <el-form :model="entryForm" label-width="90px">
        <el-form-item label="标题"><el-input v-model="entryForm.title" /></el-form-item>
        <el-form-item label="分类ID"><el-input-number v-model="entryForm.categoryId" style="width: 100%" /></el-form-item>
        <el-form-item label="摘要"><el-input v-model="entryForm.summary" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="entryForm.content" type="textarea" :rows="8" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="entryVisible = false">取消</el-button><el-button type="primary" @click="saveEntry">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { knowledgeApi } from "@/api/businessModules";

const loading = ref(false);
const keyword = ref("");
const entries = ref<any[]>([]);
function rows(data: any) { return data?.records || data?.list || []; }
async function reload() {
  loading.value = true;
  try {
    const res: any = await knowledgeApi.entries({ pn: 1, ps: 30, keyword: keyword.value || undefined });
    entries.value = rows(res.data);
  } finally { loading.value = false; }
}
const categoryVisible = ref(false);
const categoryForm = reactive<any>({ categoryName: "", parentId: undefined });
function openCategory() { Object.assign(categoryForm, { categoryName: "", parentId: undefined }); categoryVisible.value = true; }
async function saveCategory() { await knowledgeApi.createCategory(categoryForm); ElMessage.success("分类已保存"); categoryVisible.value = false; }
const entryVisible = ref(false);
const entryForm = reactive<any>({ title: "", categoryId: undefined, summary: "", content: "" });
function openEntry() { Object.assign(entryForm, { title: "", categoryId: undefined, summary: "", content: "" }); entryVisible.value = true; }
async function saveEntry() { await knowledgeApi.createEntry(entryForm); ElMessage.success("条目已保存"); entryVisible.value = false; reload(); }
async function publish(id: number) { await knowledgeApi.publishEntry(id); ElMessage.success("已发布"); reload(); }
async function archive(id: number) { await knowledgeApi.archiveEntry(id); ElMessage.success("已归档"); reload(); }
onMounted(reload);
</script>
