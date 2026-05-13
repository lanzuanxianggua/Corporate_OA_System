<template>
  <div>
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-[#303133] mb-2">
        {{ greeting }}，{{ userStore.userInfo?.empName || "用户" }}
      </h1>
      <p class="text-sm text-[#909399]">{{ currentDate }}</p>
    </div>

    <el-row :gutter="20" class="mb-5">
      <el-col v-for="item in statsCards" :key="item.label" :span="6">
        <div
          class="bg-white rounded-lg p-5 flex items-center gap-4"
          style="box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06)"
        >
          <div
            class="w-14 h-14 rounded-lg flex items-center justify-center"
            :style="{ backgroundColor: item.bgColor }"
          >
            <el-icon :size="24" :color="item.color">
              <component :is="item.icon" />
            </el-icon>
          </div>
          <div class="flex flex-col">
            <span class="text-2xl font-bold text-[#303133]">
              {{ item.value }}
            </span>
            <span class="text-sm text-[#909399] mt-1">{{ item.label }}</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="mb-5">
      <el-col v-for="item in quickEntries" :key="item.label" :span="6">
        <div
          class="bg-white rounded-lg py-6 flex flex-col items-center gap-3 cursor-pointer transition-all duration-300 hover:-translate-y-1"
          style="box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06)"
          @click="$router.push(item.path)"
        >
          <el-icon :size="32" :color="item.color">
            <component :is="item.icon" />
          </el-icon>
          <span class="text-sm text-[#606266]">{{ item.label }}</span>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="flex justify-between items-center">
              <span class="font-medium">最近公告</span>
              <el-button
                type="primary"
                link
                @click="$router.push('/oa/notice/list')"
              >
                查看更多
              </el-button>
            </div>
          </template>
          <div v-if="noticeList.length > 0">
            <div
              v-for="item in noticeList"
              :key="item.id"
              class="py-3 border-b border-[#ebeef5] last:border-b-0 cursor-pointer hover:text-[#409EFF]"
              @click="handleViewNotice(item)"
            >
              <div class="text-sm font-medium text-[#303133] flex items-center gap-2 mb-2">
                <span
                  v-if="!item.isRead"
                  class="w-2 h-2 bg-[#409EFF] rounded-full"
                ></span>
                {{ item.title }}
              </div>
              <div class="text-xs text-[#909399] flex gap-4">
                <span>{{ item.publisher || "系统" }}</span>
                <span>{{ item.createTime }}</span>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无公告" :image-size="60" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>
            <span class="font-medium">待办事项</span>
          </template>
          <div v-if="todoList.length > 0">
            <div
              v-for="item in todoList"
              :key="item.id"
              class="py-2 border-b border-[#ebeef5] last:border-b-0 flex gap-2"
            >
              <span class="text-[#E6A23C] text-sm">{{ item.type }}</span>
              <span class="text-[#606266] text-sm">{{ item.content }}</span>
            </div>
          </div>
          <el-empty v-else description="暂无待办事项" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="noticeDialogVisible" :title="currentNotice?.title" width="600px">
      <div class="mb-4 text-sm text-[#909399] flex gap-4">
        <span>{{ currentNotice?.publisher || "系统" }}</span>
        <span>{{ currentNotice?.createTime }}</span>
        <el-tag v-if="currentNotice?.noticeType === 1" type="warning" size="small">重要</el-tag>
        <el-tag v-if="currentNotice?.noticeType === 2" type="danger" size="small">紧急</el-tag>
      </div>
      <el-divider />
      <div class="text-sm text-[#303133] leading-6">{{ currentNotice?.content }}</div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from "vue";
import dayjs from "dayjs";
import { useUserStore } from "@/store/user";
import { getDashboardStats } from "@/api/statistics";
import { getNoticePage, getNoticeById, markNoticeAsRead } from "@/api/notice";
import { getLeavePage } from "@/api/leave";
import { getUnreadCount } from "@/api/message";

const userStore = useUserStore();

const greeting = computed(() => {
  const h = new Date().getHours();
  if (h < 12) return "早上好";
  if (h < 18) return "下午好";
  return "晚上好";
});

const currentDate = computed(() => dayjs().format("YYYY年MM月DD日 dddd"));

const dashboardData = reactive({
  monthAttendance: 0,
  monthLeave: 0,
  unreadMessage: 0,
  todaySchedule: 0
});

const statsCards = computed(() => [
  {
    label: "本月出勤",
    value: dashboardData.monthAttendance,
    icon: "Calendar",
    color: "#409EFF",
    bgColor: "#e6f7ff"
  },
  {
    label: "请假次数",
    value: dashboardData.monthLeave,
    icon: "Document",
    color: "#67C23A",
    bgColor: "#f6ffed"
  },
  {
    label: "未读消息",
    value: dashboardData.unreadMessage,
    icon: "Message",
    color: "#E6A23C",
    bgColor: "#fff7e6"
  },
  {
    label: "今日日程",
    value: dashboardData.todaySchedule,
    icon: "Clock",
    color: "#9254de",
    bgColor: "#f9f0ff"
  }
]);

const quickEntries = [
  { label: "考勤打卡", icon: "Clock", color: "#409EFF", path: "/oa/attendance/clock" },
  { label: "请假申请", icon: "DocumentAdd", color: "#67C23A", path: "/oa/leave/apply" },
  { label: "公告通知", icon: "Bell", color: "#E6A23C", path: "/oa/notice/list" },
  { label: "消息中心", icon: "ChatDotRound", color: "#9254de", path: "/oa/message/list" }
];

const noticeList = ref<any[]>([]);
const todoList = ref<any[]>([]);
const noticeDialogVisible = ref(false);
const currentNotice = ref<any>(null);

const handleViewNotice = async (item: any) => {
  try {
    const res: any = await getNoticeById(item.id!);
    if (res.data) {
      currentNotice.value = res.data;
      noticeDialogVisible.value = true;
    }
    if (!item.isRead) {
      await markNoticeAsRead(item.id!);
      item.isRead = true;
    }
  } catch {
    // ignore
  }
};

onMounted(async () => {
  try {
    const res: any = await getDashboardStats("today");
    if (res.data) {
      const att = res.data.attendance || {};
      const lv = res.data.leave || {};
      dashboardData.monthAttendance = att.clockedIn || 0;
      dashboardData.monthLeave = lv.total || 0;
    }
  } catch {
    // ignore
  }
  try {
    const res: any = await getUnreadCount();
    if (res.data !== undefined) dashboardData.unreadMessage = res.data;
  } catch {
    // ignore
  }
  try {
    const res: any = await getNoticePage({ pageNum: 1, pageSize: 5 });
    if (res.data?.list) noticeList.value = res.data.list;
  } catch {
    // ignore
  }
  try {
    const res: any = await getLeavePage({ pageNum: 1, pageSize: 10, status: 0 });
    if (res.data?.list) {
      const leaveTypeMap: Record<number, string> = { 1: "事假", 2: "病假", 3: "年假", 4: "婚假", 5: "丧假", 6: "产假" };
      todoList.value = res.data.list.map((item: any) => {
        const start = item.startTime ? new Date(item.startTime) : null;
        const end = item.endTime ? new Date(item.endTime) : null;
        const days = start && end ? Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)) : 0;
        return {
          id: item.id,
          type: leaveTypeMap[item.leaveType] || "请假",
          content: `请假 ${days} 天 - ${item.reason || "无原因"}`
        };
      });
    }
  } catch {
    // ignore
  }
});
</script>
