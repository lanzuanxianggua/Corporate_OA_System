<script setup lang="ts">
import { ref, onMounted } from "vue";
import { getTodayStatus } from "@/api/oa/attendance";
import { getUnreadCount } from "@/api/oa/message";
import { getNoticePage } from "@/api/oa/notice";

defineOptions({ name: "OaWorkbench" });

const todayStatus = ref<any>({});
const unreadCount = ref(0);
const notices = ref<any[]>([]);
const loading = ref(true);

onMounted(async () => {
  try {
    const [statusRes, msgRes, noticeRes] = await Promise.allSettled([
      getTodayStatus(),
      getUnreadCount(),
      getNoticePage({ pageNum: 1, pageSize: 5 })
    ]);
    if (statusRes.status === "fulfilled" && statusRes.value.data) {
      todayStatus.value = statusRes.value.data;
    }
    if (msgRes.status === "fulfilled" && msgRes.value.data != null) {
      unreadCount.value = msgRes.value.data;
    }
    if (noticeRes.status === "fulfilled" && noticeRes.value.data?.list) {
      notices.value = noticeRes.value.data.list;
    }
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div class="workbench-container">
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>考勤状态</template>
          <div class="stat-card">
            <el-icon size="32" color="#409EFF"><i class="ri-time-line" /></el-icon>
            <div>
              <p class="stat-value">{{ todayStatus.clockIn || '未打卡' }}</p>
              <p class="stat-label">上班打卡</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>未读消息</template>
          <div class="stat-card">
            <el-icon size="32" color="#67C23A"><i class="ri-chat-3-line" /></el-icon>
            <div>
              <p class="stat-value">{{ unreadCount }}</p>
              <p class="stat-label">条未读</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>待办事项</template>
          <div class="stat-card">
            <el-icon size="32" color="#E6A23C"><i class="ri-list-check" /></el-icon>
            <div>
              <p class="stat-value">0</p>
              <p class="stat-label">项待办</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>今日日程</template>
          <div class="stat-card">
            <el-icon size="32" color="#F56C6C"><i class="ri-calendar-line" /></el-icon>
            <div>
              <p class="stat-value">0</p>
              <p class="stat-label">项日程</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 16px">
      <template #header>最新公告</template>
      <el-table :data="notices" stripe v-loading="loading" empty-text="暂无公告">
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="createTime" label="发布时间" width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.stat-card { display: flex; align-items: center; gap: 16px; }
.stat-value { font-size: 24px; font-weight: bold; margin: 0; }
.stat-label { font-size: 12px; color: #999; margin: 4px 0 0; }
</style>
