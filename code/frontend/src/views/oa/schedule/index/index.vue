<template>
  <div class="schedule-container">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card class="calendar-card">
          <el-calendar v-model="currentDate">
            <template #date-cell="{ data }">
              <div class="calendar-day">
                <span>{{ data.day.split("-").slice(2).join("") }}</span>
                <div v-if="hasSchedule(data.day)" class="schedule-dot"></div>
              </div>
            </template>
          </el-calendar>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card class="schedule-card">
          <template #header>
            <div class="card-header">
              <span>{{ selectedDate }} 日程</span>
              <el-button type="primary" size="small" @click="handleAdd">
                <el-icon><Plus /></el-icon>
                添加日程
              </el-button>
            </div>
          </template>
          <div class="timeline" v-if="scheduleList.length > 0">
            <div v-for="item in scheduleList" :key="item.id" class="timeline-item">
              <div class="timeline-time">{{ item.time }}</div>
              <div class="timeline-content">
                <div class="timeline-title">{{ item.title }}</div>
                <div class="timeline-desc">{{ item.desc }}</div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无日程" :image-size="80" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" title="添加日程" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="日程标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入日程标题" />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker v-model="form.startTime" type="datetime" placeholder="选择开始时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker v-model="form.endTime" type="datetime" placeholder="选择结束时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.desc" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { ElMessage } from "element-plus";
import dayjs from "dayjs";
import type { FormInstance, FormRules } from "element-plus";

const currentDate = ref(new Date());
const dialogVisible = ref(false);
const formRef = ref<FormInstance>();

const form = ref({
  title: "",
  startTime: "",
  endTime: "",
  desc: ""
});

const rules: FormRules = {
  title: [{ required: true, message: "请输入日程标题", trigger: "blur" }],
  startTime: [{ required: true, message: "请选择开始时间", trigger: "change" }],
  endTime: [{ required: true, message: "请选择结束时间", trigger: "change" }]
};

const selectedDate = computed(() => dayjs(currentDate.value).format("YYYY-MM-DD"));

const scheduleList = ref([
  { id: 1, time: "09:00", title: "晨会", desc: "技术部周例会" },
  { id: 2, time: "14:00", title: "项目评审", desc: "新项目需求评审会议" }
]);

const hasSchedule = (date: string) => {
  return ["01", "05", "13", "20"].includes(date.split("-")[2]);
};

const handleAdd = () => {
  form.value = { title: "", startTime: "", endTime: "", desc: "" };
  dialogVisible.value = true;
};

const handleSubmit = () => {
  formRef.value?.validate((valid) => {
    if (valid) {
      ElMessage.success("添加成功");
      dialogVisible.value = false;
    }
  });
};
</script>

<style scoped lang="scss">
.schedule-container {
  .calendar-day {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    position: relative;

    .schedule-dot {
      width: 4px;
      height: 4px;
      background-color: #409EFF;
      border-radius: 50%;
      position: absolute;
      bottom: 4px;
    }
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .timeline {
    .timeline-item {
      display: flex;
      gap: 16px;
      padding: 12px 0;
      border-bottom: 1px solid #ebeef5;

      &:last-child {
        border-bottom: none;
      }
    }

    .timeline-time {
      width: 60px;
      color: #409EFF;
      font-weight: bold;
    }

    .timeline-content {
      flex: 1;
    }

    .timeline-title {
      font-weight: bold;
      color: #303133;
      margin-bottom: 4px;
    }

    .timeline-desc {
      font-size: 13px;
      color: #909399;
    }
  }
}
</style>