<template>
  <div class="notice-list-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>公告列表</span>
          <el-input v-model="keyword" placeholder="搜索公告标题" style="width: 200px" clearable />
        </div>
      </template>
      <div class="notice-list" v-loading="loading">
        <div v-for="item in noticeList" :key="item.id" class="notice-card" @click="handleView(item)">
          <div class="notice-content">
            <div class="notice-title">
              <span class="unread-dot" v-if="!item.isRead"></span>
              {{ item.title }}
              <el-tag v-if="item.urgent" type="danger" size="small" style="margin-left: 8px">紧急</el-tag>
            </div>
            <div class="notice-preview">{{ item.content }}</div>
            <div class="notice-meta">
              <span>{{ item.publisherName }}</span>
              <span>{{ item.publishTime }}</span>
            </div>
          </div>
        </div>
        <el-empty v-if="!loading && noticeList.length === 0" description="暂无公告" :image-size="60" />
      </div>
      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="currentNotice?.title" width="600px">
      <el-divider />
      <div class="notice-detail">
        <div class="notice-info">
          <span>发布人：{{ currentNotice?.publisherName }}</span>
          <span>发布时间：{{ currentNotice?.publishTime }}</span>
          <el-tag v-if="currentNotice?.urgent" type="danger">紧急</el-tag>
        </div>
        <el-divider />
        <div class="notice-body">{{ currentNotice?.content }}</div>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from "vue";
import { getNoticePage, markNoticeAsRead } from "@/api/notice";

const keyword = ref("");
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const loading = ref(false);
const dialogVisible = ref(false);
const currentNotice = ref<any>(null);

const noticeList = ref<any[]>([]);

const loadData = async () => {
  try {
    loading.value = true;
    const res: any = await getNoticePage({
      pageNum: currentPage.value,
      pageSize: pageSize.value
    });
    if (res.data?.list) {
      noticeList.value = res.data.list;
      total.value = res.data.total;
    }
  } catch (error) {
    console.error("获取公告列表失败", error);
  } finally {
    loading.value = false;
  }
};

const handleView = async (item: any) => {
  currentNotice.value = item;
  dialogVisible.value = true;

  if (!item.isRead) {
    try {
      await markNoticeAsRead(item.id);
      item.isRead = true;
    } catch (error) {
      console.error("标记已读失败", error);
    }
  }
};

watch(keyword, () => {
  currentPage.value = 1;
  loadData();
});

onMounted(() => {
  loadData();
});
</script>

<style scoped lang="scss">
.notice-list-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .notice-list {
    .notice-card {
      padding: 16px 0;
      border-bottom: 1px solid #ebeef5;
      cursor: pointer;
      transition: background-color 0.3s;

      &:hover {
        background-color: #f5f7fa;
        margin: 0 -20px;
        padding: 16px 20px;
      }

      &:last-child {
        border-bottom: none;
      }
    }
  }

  .notice-title {
    font-size: 14px;
    font-weight: bold;
    color: #303133;
    margin-bottom: 8px;
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .unread-dot {
    width: 8px;
    height: 8px;
    background-color: #409EFF;
    border-radius: 50%;
  }

  .notice-preview {
    font-size: 13px;
    color: #909399;
    margin-bottom: 8px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .notice-meta {
    font-size: 12px;
    color: #c0c4cc;
    display: flex;
    gap: 16px;
  }

  .notice-info {
    display: flex;
    gap: 20px;
    align-items: center;
    font-size: 14px;
    color: #606266;
  }

  .notice-body {
    font-size: 14px;
    color: #303133;
    line-height: 1.8;
    white-space: pre-wrap;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>