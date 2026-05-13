<template>
  <div class="h-full">
    <el-card shadow="never">
      <!-- 顶部操作栏 -->
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[#303133]">公告管理</span>
          <div class="flex items-center gap-3">
            <el-input
              v-model="searchKey"
              placeholder="搜索公告标题"
              :prefix-icon="Search"
              clearable
              style="width: 240px"
              @input="handleSearch"
            />
            <el-button type="primary" :icon="Plus" @click="openDialog()">
              发布公告
            </el-button>
          </div>
        </div>
      </template>

      <!-- 公告表格 -->
      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        style="width: 100%"
        :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
      >
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="发布人" width="100">
          <template #default="{ row }">{{ row.publisher || "-" }}</template>
        </el-table-column>
        <el-table-column label="发布时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="紧急程度" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              :type="urgentTagType(row.noticeType)"
              size="small"
              effect="light"
            >
              {{ urgentText(row.noticeType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="openDialog(row)">
              编辑
            </el-button>
            <el-button type="danger" size="small" text @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="mt-4 flex justify-end">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @change="fetchList"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑公告' : '发布公告'"
      width="560px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="90px"
        label-position="right"
      >
        <el-form-item label="标题" prop="title">
          <el-input
            v-model="form.title"
            placeholder="请输入公告标题"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            placeholder="请输入公告内容"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="紧急程度" prop="noticeType">
          <el-select v-model="form.noticeType" placeholder="请选择" style="width: 100%">
            <el-option label="普通" :value="0" />
            <el-option label="重要" :value="1" />
            <el-option label="紧急" :value="2" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Search, Plus } from "@element-plus/icons-vue";
import type { FormInstance, FormRules } from "element-plus";
import {
  getNoticePage,
  addNotice,
  updateNotice,
  deleteNotice,
  type NoticeVO
} from "@/api/notice";

// --- 列表 ---
const loading = ref(false);
const tableData = ref<NoticeVO[]>([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const searchKey = ref("");

let searchTimer: ReturnType<typeof setTimeout> | null = null;

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await getNoticePage({
      pageNum: pageNum.value,
      pageSize: pageSize.value
    });
    let list: NoticeVO[] = res.data?.list || [];
    if (searchKey.value.trim()) {
      const key = searchKey.value.trim().toLowerCase();
      list = list.filter((n) => n.title?.toLowerCase().includes(key));
    }
    tableData.value = list;
    total.value = res.data?.total || 0;
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  if (searchTimer) clearTimeout(searchTimer);
  searchTimer = setTimeout(() => {
    pageNum.value = 1;
    fetchList();
  }, 300);
};

// --- 弹窗 ---
const dialogVisible = ref(false);
const submitting = ref(false);
const isEdit = ref(false);
const formRef = ref<FormInstance>();

const form = reactive({
  id: undefined as number | undefined,
  title: "",
  content: "",
  noticeType: 0
});

const rules = reactive<FormRules>({
  title: [{ required: true, message: "请输入公告标题", trigger: "blur" }],
  content: [{ required: true, message: "请输入公告内容", trigger: "blur" }],
  noticeType: [{ required: true, message: "请选择紧急程度", trigger: "change" }]
});

const openDialog = (row?: NoticeVO) => {
  if (row) {
    isEdit.value = true;
    form.id = row.id;
    form.title = row.title;
    form.content = row.content;
    form.noticeType = row.noticeType ?? 0;
  } else {
    isEdit.value = false;
    form.id = undefined;
    form.title = "";
    form.content = "";
    form.noticeType = 0;
  }
  dialogVisible.value = true;
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  await formRef.value.validate();

  submitting.value = true;
  try {
    const data: Partial<NoticeVO> = {
      title: form.title,
      content: form.content,
      noticeType: form.noticeType
    };
    if (isEdit.value && form.id) {
      data.id = form.id;
      await updateNotice(data);
      ElMessage.success("公告已更新");
    } else {
      await addNotice(data);
      ElMessage.success("公告已发布");
    }
    dialogVisible.value = false;
    fetchList();
  } catch {
    // error handled by interceptor
  } finally {
    submitting.value = false;
  }
};

// --- 删除 ---
const handleDelete = async (row: NoticeVO) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除公告「${row.title}」吗？`,
      "提示",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }
    );
    await deleteNotice(row.id!);
    ElMessage.success("公告已删除");
    fetchList();
  } catch {
    // cancelled or error
  }
};

// --- 工具 ---
const urgentText = (urgent?: number) => {
  const map: Record<number, string> = { 0: "普通", 1: "重要", 2: "紧急" };
  return map[urgent ?? 0] || "普通";
};

const urgentTagType = (urgent?: number) => {
  const map: Record<number, string> = { 0: "info", 1: "warning", 2: "danger" };
  return map[urgent ?? 0] || "info";
};

const formatTime = (time?: string) => {
  if (!time) return "-";
  return time.replace("T", " ").substring(0, 16);
};

onMounted(() => {
  fetchList();
});
</script>
