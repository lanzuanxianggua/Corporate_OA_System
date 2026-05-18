<template>
  <div>
    <!-- 问候 + 时间 -->
    <el-row :gutter="20" class="mb-5">
      <el-col :span="16">
        <div class="bg-white rounded-lg p-6" style="box-shadow:0 2px 12px rgba(0,0,0,.06)">
          <h1 class="text-2xl font-bold text-[#303133] mb-2">
            {{ greeting }}，{{ userStore.userInfo?.empName || "用户" }}
          </h1>
          <p class="text-sm text-[#909399]">{{ currentDate }}</p>
          <div class="mt-4 flex gap-3">
            <el-button type="primary" @click="$router.push('/oa/leave/apply')">
              <el-icon class="mr-1"><DocumentAdd /></el-icon>请假申请
            </el-button>
            <el-button @click="$router.push('/oa/schedule/index')">
              <el-icon class="mr-1"><Calendar /></el-icon>我的日程
            </el-button>
            <el-button @click="$router.push('/oa/message/list')">
              <el-icon class="mr-1"><ChatDotRound /></el-icon>消息中心
            </el-button>
          </div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="bg-white rounded-lg p-6 text-center" style="box-shadow:0 2px 12px rgba(0,0,0,.06)">
          <div class="text-4xl font-bold text-[#303133] font-mono">{{ currentTime }}</div>
          <div class="text-sm text-[#909399] mt-2">{{ currentWeekday }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="mb-5">
      <el-col v-for="item in statsCards" :key="item.label" :span="6">
        <div class="bg-white rounded-lg p-5 flex items-center gap-4" style="box-shadow:0 2px 12px rgba(0,0,0,.06)">
          <div class="w-14 h-14 rounded-lg flex items-center justify-center" :style="{ backgroundColor: item.bgColor }">
            <el-icon :size="24" :color="item.color"><component :is="item.icon" /></el-icon>
          </div>
          <div class="flex flex-col">
            <span class="text-2xl font-bold text-[#303133]">{{ item.value }}</span>
            <span class="text-sm text-[#909399] mt-1">{{ item.label }}</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 今日考勤 + 考勤打卡 -->
    <el-row :gutter="20" class="mb-5">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="flex justify-between items-center">
              <span class="font-medium">今日考勤</span>
              <el-tag :type="statusTagType" effect="dark">{{ statusLabel }}</el-tag>
            </div>
          </template>
          <div v-if="todayAtt">
            <el-row :gutter="20">
              <el-col :span="12">
                <div class="text-center p-4 bg-[#f0f9ff] rounded-lg">
                  <div class="text-xs text-[#909399] mb-2">上班打卡</div>
                  <template v-if="todayAtt.clockIn">
                    <div class="text-xl font-bold text-[#67C23A]">
                      <el-icon class="mr-1"><CircleCheck /></el-icon>
                      {{ todayAtt.clockIn.substring(11, 16) }}
                    </div>
                  </template>
                  <template v-else>
                    <el-button type="primary" size="small" :loading="clockingIn" @click="handleClockIn">
                      上班打卡
                    </el-button>
                  </template>
                </div>
              </el-col>
              <el-col :span="12">
                <div class="text-center p-4 bg-[#fff7e6] rounded-lg">
                  <div class="text-xs text-[#909399] mb-2">下班打卡</div>
                  <template v-if="todayAtt.clockOut">
                    <div class="text-xl font-bold text-[#67C23A]">
                      <el-icon class="mr-1"><CircleCheck /></el-icon>
                      {{ todayAtt.clockOut.substring(11, 16) }}
                    </div>
                  </template>
                  <template v-else>
                    <el-button type="warning" size="small" :loading="clockingOut" @click="handleClockOut">
                      下班打卡
                    </el-button>
                  </template>
                </div>
              </el-col>
            </el-row>
            <div v-if="todayAtt.clockIn && todayAtt.clockOut" class="mt-3 text-center text-sm text-[#909399]">
              工作时长：{{ calcWorkHours(todayAtt.clockIn, todayAtt.clockOut) }}
            </div>
          </div>
          <el-empty v-else description="今日暂无考勤记录" :image-size="50">
            <el-button type="primary" size="small" :loading="clockingIn" @click="handleClockIn">
              立即打卡
            </el-button>
          </el-empty>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="flex justify-between items-center">
              <span class="font-medium">本月出勤概览</span>
              <el-button type="primary" link @click="$router.push('/oa/report/personal')">查看报表</el-button>
            </div>
          </template>
          <div class="grid grid-cols-4 gap-3 text-center">
            <div class="p-3 bg-[#f0f9ff] rounded-lg">
              <div class="text-2xl font-bold text-[#409EFF]">{{ monthStats.normalDays }}</div>
              <div class="text-xs text-[#909399] mt-1">正常</div>
            </div>
            <div class="p-3 bg-[#fff7e6] rounded-lg">
              <div class="text-2xl font-bold text-[#E6A23C]">{{ monthStats.lateDays }}</div>
              <div class="text-xs text-[#909399] mt-1">迟到</div>
            </div>
            <div class="p-3 bg-[#fef0f0] rounded-lg">
              <div class="text-2xl font-bold text-[#F56C6C]">{{ monthStats.earlyDays }}</div>
              <div class="text-xs text-[#909399] mt-1">早退</div>
            </div>
            <div class="p-3 bg-[#f9f0ff] rounded-lg">
              <div class="text-2xl font-bold text-[#9254de]">{{ monthStats.absentDays }}</div>
              <div class="text-xs text-[#909399] mt-1">缺勤</div>
            </div>
          </div>
          <div class="mt-3">
            <div class="flex justify-between text-xs text-[#909399] mb-1">
              <span>出勤率</span>
              <span>{{ attendanceRate }}%</span>
            </div>
            <el-progress :percentage="attendanceRate" :stroke-width="10" :show-text="false"
              :color="attendanceRate >= 90 ? '#67C23A' : attendanceRate >= 70 ? '#E6A23C' : '#F56C6C'" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷入口 -->
    <el-card class="mb-5">
      <template #header><span class="font-medium">快捷入口</span></template>
      <div class="grid grid-cols-8 gap-4">
        <div v-for="item in quickEntries" :key="item.label"
          class="flex flex-col items-center gap-2 py-3 cursor-pointer rounded-lg hover:bg-[#f5f7fa] transition-colors"
          @click="$router.push(item.path)">
          <div class="w-10 h-10 rounded-lg flex items-center justify-center" :style="{ backgroundColor: item.bgColor }">
            <el-icon :size="20" :color="item.color"><component :is="item.icon" /></el-icon>
          </div>
          <span class="text-xs text-[#606266]">{{ item.label }}</span>
        </div>
      </div>
    </el-card>

    <!-- 公告 + 日程 + 待办 -->
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card>
          <template #header>
            <div class="flex justify-between items-center">
              <span class="font-medium">最新公告</span>
              <el-button type="primary" link @click="$router.push('/oa/notice/list')">更多</el-button>
            </div>
          </template>
          <div v-if="noticeList.length > 0">
            <div v-for="item in noticeList" :key="item.id"
              class="py-3 border-b border-[#ebeef5] last:border-b-0 cursor-pointer hover:text-[#409EFF]"
              @click="handleViewNotice(item)">
              <div class="text-sm font-medium text-[#303133] truncate flex items-center gap-1">
                <el-tag v-if="item.noticeType === 1" type="danger" size="small" class="shrink-0">公告</el-tag>
                <el-tag v-else type="info" size="small" class="shrink-0">通知</el-tag>
                {{ item.title }}
              </div>
              <div class="text-xs text-[#909399] mt-1">{{ item.createTime }}</div>
            </div>
          </div>
          <el-empty v-else description="暂无公告" :image-size="50" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>
            <div class="flex justify-between items-center">
              <span class="font-medium">今日日程</span>
              <el-button type="primary" link @click="$router.push('/oa/schedule/index')">更多</el-button>
            </div>
          </template>
          <div v-if="scheduleList.length > 0">
            <div v-for="item in scheduleList" :key="item.id"
              class="py-3 border-b border-[#ebeef5] last:border-b-0">
              <div class="text-sm font-medium text-[#303133] flex items-center gap-2">
                <el-icon color="#409EFF"><Clock /></el-icon>
                {{ item.title }}
              </div>
              <div class="text-xs text-[#909399] mt-1">
                {{ (item.startTime || "").substring(11, 16) }} - {{ (item.endTime || "").substring(11, 16) }}
              </div>
            </div>
          </div>
          <el-empty v-else description="今日无日程" :image-size="50" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>
            <div class="flex justify-between items-center">
              <span class="font-medium">待办事项</span>
              <el-tag v-if="todoList.length > 0" type="danger" size="small">{{ todoList.length }}</el-tag>
            </div>
          </template>
          <div v-if="todoList.length > 0">
            <div v-for="item in todoList" :key="item.id"
              class="py-3 border-b border-[#ebeef5] last:border-b-0 flex gap-2 items-start">
              <el-tag :type="item.tagType" size="small" effect="light">{{ item.type }}</el-tag>
              <span class="text-sm text-[#606266] leading-5">{{ item.content }}</span>
            </div>
          </div>
          <el-empty v-else description="暂无待办事项" :image-size="50" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="noticeDialogVisible" :title="currentNotice?.title" width="600px">
      <div class="mb-4 text-sm text-[#909399]">{{ currentNotice?.createTime }}</div>
      <el-divider />
      <div class="text-sm text-[#303133] leading-6">{{ currentNotice?.content }}</div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed } from "vue";
import dayjs from "dayjs";
import { ElMessage } from "element-plus";
import { useUserStore } from "@/store/user";
import { getTodayAttendance, clockIn, clockOut } from "@/api/attendance";
import { getNoticePage } from "@/api/notice";
import { getLeavePage } from "@/api/leave";
import { getUnreadCount } from "@/api/message";
import { getSchedulePage } from "@/api/schedule";
import { getPersonalAttendanceSummary } from "@/api/report";

const userStore = useUserStore();

const currentTime = ref(dayjs().format("HH:mm:ss"));
const currentDate = computed(() => dayjs().format("YYYY年MM月DD日"));
const currentWeekday = computed(() => dayjs().format("dddd"));
let timer: number;

const greeting = computed(() => {
  const h = new Date().getHours();
  if (h < 12) return "早上好";
  if (h < 18) return "下午好";
  return "晚上好";
});

const todayAtt = ref<any>(null);
const clockingIn = ref(false);
const clockingOut = ref(false);

const monthStats = reactive({ normalDays: 0, lateDays: 0, earlyDays: 0, absentDays: 0 });
const attendanceRate = computed(() => {
  const total = monthStats.normalDays + monthStats.lateDays + monthStats.earlyDays + monthStats.absentDays;
  if (total === 0) return 0;
  return Math.round((monthStats.normalDays / total) * 100);
});

const statsCards = reactive([
  { label: "今日出勤", value: 0, icon: "CircleCheck", color: "#409EFF", bgColor: "#e6f7ff" },
  { label: "待审请假", value: 0, icon: "Document", color: "#E6A23C", bgColor: "#fff7e6" },
  { label: "未读消息", value: 0, icon: "Message", color: "#F56C6C", bgColor: "#fef0f0" },
  { label: "今日日程", value: 0, icon: "Clock", color: "#9254de", bgColor: "#f9f0ff" }
]);

const quickEntries = [
  { label: "考勤打卡", icon: "Sunny", color: "#E6A23C", bgColor: "#fff7e6", path: "/oa/attendance/clock" },
  { label: "请假申请", icon: "DocumentAdd", color: "#409EFF", bgColor: "#e6f7ff", path: "/oa/leave/apply" },
  { label: "公告通知", icon: "Bell", color: "#F56C6C", bgColor: "#fef0f0", path: "/oa/notice/list" },
  { label: "文档中心", icon: "Folder", color: "#67C23A", bgColor: "#f0f9eb", path: "/oa/document/list" },
  { label: "消息中心", icon: "ChatDotRound", color: "#9254de", bgColor: "#f9f0ff", path: "/oa/message/list" },
  { label: "我的日程", icon: "Calendar", color: "#409EFF", bgColor: "#e6f7ff", path: "/oa/schedule/index" },
  { label: "个人报表", icon: "DataAnalysis", color: "#E6A23C", bgColor: "#fff7e6", path: "/oa/report/personal" },
  { label: "数据看板", icon: "TrendCharts", color: "#F56C6C", bgColor: "#fef0f0", path: "/oa/dashboard" }
];

const noticeList = ref<any[]>([]);
const scheduleList = ref<any[]>([]);
const todoList = ref<any[]>([]);
const noticeDialogVisible = ref(false);
const currentNotice = ref<any>(null);

const statusLabel = computed(() => {
  if (!todayAtt.value) return "未打卡";
  const s = todayAtt.value.status;
  if (s === 0) return "正常";
  if (s === 1) return "迟到";
  if (s === 2) return "早退";
  if (s === 3) return "缺勤";
  if (s === 4) return "请假";
  return "未打卡";
});

const statusTagType = computed(() => {
  if (!todayAtt.value) return "info";
  const s = todayAtt.value.status;
  if (s === 0) return "success";
  if (s === 1) return "warning";
  if (s === 2) return "danger";
  if (s === 3) return "danger";
  return "info";
});

const calcWorkHours = (clockIn?: string, clockOut?: string) => {
  if (!clockIn || !clockOut) return "-";
  const diff = new Date(clockOut).getTime() - new Date(clockIn).getTime();
  return (diff / (1000 * 60 * 60)).toFixed(1) + "小时";
};

const handleClockIn = async () => {
  clockingIn.value = true;
  try {
    await clockIn();
    ElMessage.success("上班打卡成功");
    await fetchToday();
  } catch (e: any) {
    ElMessage.error(e.message || "打卡失败");
  } finally {
    clockingIn.value = false;
  }
};

const handleClockOut = async () => {
  clockingOut.value = true;
  try {
    await clockOut();
    ElMessage.success("下班打卡成功");
    await fetchToday();
  } catch (e: any) {
    ElMessage.error(e.message || "打卡失败");
  } finally {
    clockingOut.value = false;
  }
};

const handleViewNotice = (item: any) => {
  currentNotice.value = item;
  noticeDialogVisible.value = true;
};

const fetchToday = async () => {
  try {
    const res: any = await getTodayAttendance();
    if (res.data) todayAtt.value = res.data;
  } catch {}
};

const fetchStats = async () => {
  try {
    const res: any = await getUnreadCount();
    if (res.data !== undefined) statsCards[2].value = res.data;
  } catch {}
  try {
    const today = dayjs().format("YYYY-MM-DD");
    const res: any = await getSchedulePage({ pageNum: 1, pageSize: 100 });
    if (res.data?.list) {
      const todayItems = res.data.list.filter((s: any) =>
        (s.startTime || "").startsWith(today)
      );
      statsCards[3].value = todayItems.length;
    }
  } catch {}
  try {
    const res: any = await getLeavePage({ pageNum: 1, pageSize: 1, status: 0 });
    if (res.data?.total !== undefined) statsCards[1].value = res.data.total;
  } catch {}
  try {
    const res: any = await getTodayAttendance();
    if (res.data) statsCards[0].value = res.data.clockIn ? 1 : 0;
  } catch {}
};

const fetchMonthStats = async () => {
  try {
    const res: any = await getPersonalAttendanceSummary(dayjs().format("YYYY-MM"));
    if (res.data) {
      monthStats.normalDays = res.data.normalDays || 0;
      monthStats.lateDays = res.data.lateDays || 0;
      monthStats.earlyDays = res.data.earlyLeaveDays || 0;
      monthStats.absentDays = res.data.absentDays || 0;
    }
  } catch {}
};

const fetchNotice = async () => {
  try {
    const res: any = await getNoticePage({ pageNum: 1, pageSize: 5 });
    if (res.data?.list) noticeList.value = res.data.list;
  } catch {}
};

const fetchSchedule = async () => {
  try {
    const today = dayjs().format("YYYY-MM-DD");
    const res: any = await getSchedulePage({ pageNum: 1, pageSize: 10 });
    if (res.data?.list) {
      scheduleList.value = res.data.list.filter((s: any) =>
        (s.startTime || "").startsWith(today)
      );
    }
  } catch {}
};

const fetchTodo = async () => {
  try {
    const res: any = await getLeavePage({ pageNum: 1, pageSize: 10, status: 0 });
    if (res.data?.list) {
      const leaveTypeMap: Record<number, string> = { 0: "事假", 1: "病假", 2: "年假", 3: "婚假", 4: "产假", 5: "其他" };
      const leaveTagMap: Record<number, string> = { 0: "warning", 1: "danger", 2: "success", 3: "", 4: "info", 5: "warning" };
      todoList.value = res.data.list.map((item: any) => {
        const start = item.startTime ? new Date(item.startTime) : null;
        const end = item.endTime ? new Date(item.endTime) : null;
        const days = start && end ? Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)) : 0;
        return {
          id: item.id,
          type: leaveTypeMap[item.leaveType] || "请假",
          tagType: leaveTagMap[item.leaveType] || "info",
          content: `${item.empName || "员工"} 请假${days}天 - ${item.reason || "无原因"}`
        };
      });
    }
  } catch {}
};

onMounted(() => {
  fetchToday();
  fetchStats();
  fetchMonthStats();
  fetchNotice();
  fetchSchedule();
  fetchTodo();
  timer = window.setInterval(() => {
    currentTime.value = dayjs().format("HH:mm:ss");
  }, 1000);
});

onUnmounted(() => clearInterval(timer));
</script>
