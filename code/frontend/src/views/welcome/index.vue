<template>
  <div class="welcome-container">
    <div class="greeting">
      <h1>早上好，{{ userInfo?.empName || '用户' }}</h1>
      <p>{{ currentDate }}</p>
    </div>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background-color: #e6f7ff">
            <el-icon size="24" color="#409EFF"><Calendar /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ dashboardData.monthAttendance || 0 }}</span>
            <span class="stat-label">本月出勤</span>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background-color: #f6ffed">
            <el-icon size="24" color="#67C23A"><Document /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ dashboardData.monthLeave || 0 }}</span>
            <span class="stat-label">请假次数</span>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background-color: #fff7e6">
            <el-icon size="24" color="#E6A23C"><Message /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ dashboardData.unreadMessage || 0 }}</span>
            <span class="stat-label">未读消息</span>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background-color: #f9f0ff">
            <el-icon size="24" color="#9254de"><Clock /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ dashboardData.todaySchedule || 0 }}</span>
            <span class="stat-label">今日日程</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="quick-entry-row">
      <el-col :span="6">
        <div class="quick-entry-card" @click="$router.push('/oa/attendance/clock')">
          <el-icon size="32" color="#409EFF"><Clock /></el-icon>
          <span>考勤打卡</span>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="quick-entry-card" @click="$router.push('/oa/leave/apply')">
          <el-icon size="32" color="#67C23A"><DocumentAdd /></el-icon>
          <span>请假申请</span>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="quick-entry-card" @click="$router.push('/oa/notice/list')">
          <el-icon size="32" color="#E6A23C"><Bell /></el-icon>
          <span>公告通知</span>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="quick-entry-card" @click="$router.push('/oa/message/list')">
          <el-icon size="32" color="#9254de"><ChatDotRound /></el-icon>
          <span>消息中心</span>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="16">
        <el-card class="notice-card">
          <template #header>
            <div class="card-header">
              <span>最近公告</span>
              <el-button type="primary" link @click="$router.push('/oa/notice/list')">查看更多</el-button>
            </div>
          </template>
          <div class="notice-list" v-if="noticeList.length > 0">
            <div v-for="item in noticeList" :key="item.id" class="notice-item" @click="handleViewNotice(item)">
              <div class="notice-title">
                <span class="unread-dot" v-if="!item.isRead"></span>
                {{ item.title }}
              </div>
              <div class="notice-meta">
                <span>{{ item.publisherName }}</span>
                <span>{{ item.publishTime }}</span>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无公告" :image-size="60" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="todo-card">
          <template #header>
            <span>待办事项</span>
          </template>
          <div class="todo-list" v-if="todoList.length > 0">
            <div v-for="item in todoList" :key="item.id" class="todo-item">
              <span class="todo-type">{{ item.type }}</span>
              <span class="todo-content">{{ item.content }}</span>
            </div>
          </div>
          <el-empty v-else description="暂无待办事项" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from "vue";
import dayjs from "dayjs";
import { getDashboardStats } from "@/api/statistics";
import { getNoticePage } from "@/api/notice";
import { getLeavePage } from "@/api/leave";

const currentDate = computed(() => {
  return dayjs().format("YYYY年MM月DD日 dddd");
});

const userInfo = ref<any>(null);

const dashboardData = reactive({
  monthAttendance: 0,
  monthLeave: 0,
  unreadMessage: 0,
  todaySchedule: 0
});

const noticeList = ref<any[]>([]);
const todoList = ref<any[]>([]);

onMounted(async () => {
  const token = localStorage.getItem("token");
  const stored = localStorage.getItem("userInfo");
  if (stored) {
    userInfo.value = JSON.parse(stored);
  }

  if (!token) {
    return;
  }

  try {
    const statsRes: any = await getDashboardStats("today");
    if (statsRes.data) {
      Object.assign(dashboardData, statsRes.data);
    }
  } catch (error) {
    console.error("获取仪表盘数据失败", error);
  }

  try {
    const noticeRes: any = await getNoticePage({ pageNum: 1, pageSize: 5 });
    if (noticeRes.data?.list) {
      noticeList.value = noticeRes.data.list;
    }
  } catch (error) {
    console.error("获取公告列表失败", error);
  }

  try {
    const leaveRes: any = await getLeavePage({ pageNum: 1, pageSize: 10, status: 0 });
    if (leaveRes.data?.list) {
      todoList.value = leaveRes.data.list.map((item: any) => ({
        id: item.id,
        type: item.type,
        content: `请假 ${item.days} 天`
      }));
    }
  } catch (error) {
    console.error("获取待办列表失败", error);
  }
});

const handleViewNotice = (item: any) => {
  // Handle notice view
};
</script>

<style scoped lang="scss">
.welcome-container {
  padding: 0;
}

.greeting {
  margin-bottom: 24px;

  h1 {
    font-size: 24px;
    font-weight: bold;
    color: #303133;
    margin-bottom: 8px;
  }

  p {
    font-size: 14px;
    color: #909399;
  }
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  background: #ffffff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.quick-entry-row {
  margin-bottom: 20px;
}

.quick-entry-card {
  background: #ffffff;
  border-radius: 8px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  }

  span {
    font-size: 14px;
    color: #606266;
  }
}

.notice-card,
.todo-card {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.notice-list {
  .notice-item {
    padding: 12px 0;
    border-bottom: 1px solid #ebeef5;
    cursor: pointer;

    &:last-child {
      border-bottom: none;
    }

    &:hover .notice-title {
      color: #409EFF;
    }
  }
}

.notice-title {
  font-size: 14px;
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

.notice-meta {
  font-size: 12px;
  color: #909399;
  display: flex;
  gap: 16px;
}

.todo-list {
  .todo-item {
    padding: 8px 0;
    border-bottom: 1px solid #ebeef5;
    display: flex;
    gap: 8px;

    &:last-child {
      border-bottom: none;
    }
  }

  .todo-type {
    color: #E6A23C;
    font-size: 13px;
  }

  .todo-content {
    color: #606266;
    font-size: 13px;
  }
}
</style>