<script setup lang="ts">
import { ref, reactive, onMounted, watch } from "vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import {
  getScheduleByDateRange,
  addSchedule,
  updateSchedule,
  deleteSchedule
} from "@/api/oa/schedule";

defineOptions({ name: "OaScheduleIndex" });

/** 日程记录 */
interface ScheduleRecord {
  id?: number;
  title: string;
  date: string;
  time: string;
  content: string;
}

const loading = ref(false);
const dialogVisible = ref(false);
const dialogTitle = ref("新增日程");
const schedules = ref<ScheduleRecord[]>([]);
const currentDate = ref(new Date());
const formRef = ref<FormInstance>();

const form = reactive({
  id: undefined as number | undefined,
  title: "",
  date: "",
  time: "",
  content: ""
});

const rules = reactive<FormRules>({
  title: [{ required: true, message: "请输入日程标题", trigger: "blur" }],
  date: [{ required: true, message: "请选择日期", trigger: "change" }],
  time: [{ required: true, message: "请选择时间", trigger: "change" }]
});

/** 格式化日期为 YYYY-MM-DD */
function formatDate(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

/** 获取当月日期范围 */
function getMonthRange(date: Date): { start: string; end: string } {
  const year = date.getFullYear();
  const month = date.getMonth();
  const start = new Date(year, month, 1);
  const end = new Date(year, month + 1, 0);
  return {
    start: formatDate(start),
    end: formatDate(end)
  };
}

/** 加载日程数据 */
async function fetchSchedules() {
  loading.value = true;
  try {
    const range = getMonthRange(currentDate.value);
    const res = await getScheduleByDateRange(range.start, range.end);
    schedules.value = res.data ?? res ?? [];
  } catch {
    ElMessage.error("获取日程失败");
  } finally {
    loading.value = false;
  }
}

/** 获取指定日期的日程 */
function getSchedulesByDate(date: string): ScheduleRecord[] {
  return schedules.value.filter((s) => s.date === date);
}

/** 日历单元格内容 */
function calendarCellContent(date: Date): ScheduleRecord[] {
  return getSchedulesByDate(formatDate(date));
}

/** 点击日期 - 新增日程 */
function handleDateClick(date: Date) {
  dialogTitle.value = "新增日程";
  form.id = undefined;
  form.title = "";
  form.date = formatDate(date);
  form.time = "";
  form.content = "";
  dialogVisible.value = true;
}

/** 编辑日程 */
function handleEdit(schedule: ScheduleRecord) {
  dialogTitle.value = "编辑日程";
  form.id = schedule.id;
  form.title = schedule.title;
  form.date = schedule.date;
  form.time = schedule.time;
  form.content = schedule.content;
  dialogVisible.value = true;
}

/** 删除日程 */
async function handleDelete(schedule: ScheduleRecord) {
  if (!schedule.id) return;
  await ElMessageBox.confirm(`确认删除日程「${schedule.title}」？`, "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning"
  });
  try {
    await deleteSchedule(schedule.id);
    ElMessage.success("删除成功");
    fetchSchedules();
  } catch {
    ElMessage.error("删除失败");
  }
}

/** 提交日程 */
async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  try {
    if (form.id) {
      await updateSchedule({ ...form });
      ElMessage.success("修改成功");
    } else {
      await addSchedule({ ...form });
      ElMessage.success("添加成功");
    }
    dialogVisible.value = false;
    fetchSchedules();
  } catch {
    ElMessage.error(form.id ? "修改失败" : "添加失败");
  }
}

/** 月份切换时重新加载数据 */
watch(currentDate, () => {
  fetchSchedules();
});

onMounted(() => {
  fetchSchedules();
});
</script>

<template>
  <div class="oa-schedule-index">
    <el-card shadow="hover">
      <template #header>
        <span class="card-title">我的日程</span>
      </template>

      <el-calendar v-model="currentDate">
        <template #date-cell="{ data }">
          <div class="calendar-cell" @click="handleDateClick(new Date(data.day))">
            <div class="calendar-day">{{ data.day.split("-")[2] }}</div>
            <div class="calendar-schedules">
              <div
                v-for="item in calendarCellContent(new Date(data.day))"
                :key="item.id"
                class="schedule-item"
                :title="item.title"
              >
                <span class="schedule-time">{{ item.time }}</span>
                {{ item.title }}
              </div>
            </div>
          </div>
        </template>
      </el-calendar>
    </el-card>

    <!-- 今日日程列表 -->
    <el-card shadow="hover" style="margin-top: 16px">
      <template #header>
        <span class="card-title">今日日程</span>
      </template>
      <el-table
        :data="getSchedulesByDate(formatDate(new Date()))"
        stripe
        empty-text="今日暂无日程"
        style="width: 100%"
      >
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="time" label="时间" width="120" align="center" />
        <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑日程弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入日程标题" maxlength="50" />
        </el-form-item>
        <el-form-item label="日期" prop="date">
          <el-date-picker
            v-model="form.date"
            type="date"
            placeholder="请选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="时间" prop="time">
          <el-time-select
            v-model="form.time"
            start="06:00"
            step="00:30"
            end="23:00"
            placeholder="请选择时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            placeholder="请输入日程内容"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.oa-schedule-index {
  padding: 20px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.calendar-cell {
  height: 100%;
  cursor: pointer;
  overflow: hidden;
}

.calendar-day {
  font-size: 14px;
  font-weight: 600;
  padding: 4px;
}

.calendar-schedules {
  max-height: 60px;
  overflow-y: auto;
}

.schedule-item {
  font-size: 12px;
  color: #409eff;
  padding: 2px 4px;
  margin: 1px 0;
  background: #ecf5ff;
  border-radius: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.schedule-time {
  margin-right: 4px;
  color: #909399;
}

:deep(.el-calendar-table .el-calendar-day) {
  padding: 0;
  height: auto;
  min-height: 80px;
}
</style>
