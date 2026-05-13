<template>
  <div class="h-full">
    <el-card shadow="never">
      <!-- 搜索栏 -->
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-base font-semibold text-[#303133]">公告通知</span>
          <el-input
            v-model="searchKey"
            placeholder="搜索公告标题"
            :prefix-icon="Search"
            clearable
            style="width: 280px"
            @input="handleSearch"
          />
        </div>
      </template>

      <!-- 公告卡片列表 -->
      <div v-loading="loading">
        <template v-if="noticeList.length > 0">
          <div
            v-for="item in noticeList"
            :key="item.id"
            class="relative px-4 py-3 mb-2 rounded-lg cursor-pointer border border-transparent hover:bg-[#f5f7fa] hover:border-[#e4e7ed] transition-all duration-200"
            @click="openDetail(item)"
          >
            <div class="flex items-start gap-3">
              <!-- 未读标记 -->
              <span
                v-if="!item.isRead"
                class="shrink-0 w-2 h-2 mt-2 rounded-full bg-[#409EFF]"
              />
              <span v-else class="shrink-0 w-2 mt-2" />

              <div class="flex-1 min-w-0">
                <div class="text-[14px] font-bold text-[#303133] leading-6 mb-1">
                  {{ item.title }}
                </div>
                <div class="text-[13px] text-[#909399] leading-5 line-clamp-2">
                  {{ stripHtml(item.content).substring(0, 100) }}
                </div>
              </div>
            </div>

            <div class="flex items-center gap-3 mt-2 ml-5 text-xs text-[#c0c4cc]">
              <span>{{ item.publisher || "系统" }}</span>
              <span>{{ formatTime(item.createTime) }}</span>
              <el-tag
                v-if="item.noticeType === 2"
                type="danger"
                size="small"
                effect="light"
                class="ml-1"
              >
                紧急
              </el-tag>
              <el-tag
                v-else-if="item.noticeType === 1"
                type="warning"
                size="small"
                effect="light"
                class="ml-1"
              >
                重要
              </el-tag>
            </div>
          </div>
        </template>

        <el-empty v-else description="暂无公告" />

        <!-- 分页 -->
        <div class="mt-4 flex justify-end">
          <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            background
            small
            @change="fetchList"
          />
        </div>
      </div>
    </el-card>

    <!-- 公告详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      :title="currentNotice?.title"
      width="640px"
      destroy-on-close
    >
      <template v-if="currentNotice">
        <div class="text-center text-lg font-bold text-[#303133] mb-3">
          {{ currentNotice.title }}
        </div>

        <div class="flex items-center justify-center gap-4 text-sm text-[#909399] mb-4">
          <span>{{ currentNotice.publisher || "系统" }}</span>
          <span>{{ formatTime(currentNotice.createTime) }}</span>
          <el-tag
            v-if="currentNotice.noticeType === 2"
            type="danger"
            size="small"
          >
            紧急
          </el-tag>
          <el-tag
            v-else-if="currentNotice.noticeType === 1"
            type="warning"
            size="small"
          >
            重要
          </el-tag>
          <el-tag v-else type="info" size="small">普通</el-tag>
        </div>

        <el-divider />

        <div class="text-[#303133] leading-7 whitespace-pre-wrap break-words">
          {{ stripHtml(currentNotice.content) }}
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { Search } from "@element-plus/icons-vue";
import {
  getNoticePage,
  getNoticeById,
  markNoticeAsRead,
  type NoticeVO
} from "@/api/notice";

// --- 列表 ---
const loading = ref(false);
const noticeList = ref<NoticeVO[]>([]);
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
    // 前端标题搜索过滤
    if (searchKey.value.trim()) {
      const key = searchKey.value.trim().toLowerCase();
      list = list.filter((n) => n.title?.toLowerCase().includes(key));
    }
    noticeList.value = list;
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

// --- 详情 ---
const detailVisible = ref(false);
const currentNotice = ref<NoticeVO | null>(null);

const openDetail = async (item: NoticeVO) => {
  try {
    const res: any = await getNoticeById(item.id!);
    currentNotice.value = res.data || item;
    detailVisible.value = true;

    // 标记已读
    if (!item.isRead) {
      try {
        await markNoticeAsRead(item.id!);
        item.isRead = true;
      } catch {
        // ignore
      }
    }
  } catch {
    // error handled by interceptor
  }
};

// --- 工具 ---
const formatTime = (time?: string) => {
  if (!time) return "-";
  return time.replace("T", " ").substring(0, 16);
};

const stripHtml = (html?: string) => {
  if (!html) return "";
  return html.replace(/<[^>]*>/g, "");
};

onMounted(() => {
  fetchList();
});
</script>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
