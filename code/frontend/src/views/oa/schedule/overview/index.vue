<template>
  <div class="schedule-overview-container">
    <el-row :gutter="20">
      <el-col :span="16">
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
      <el-col :span="8">
        <el-card class="schedule-card">
          <template #header>
            <span>{{ selectedDate }} 日程</span>
          </template>
          <el-table :data="scheduleList" v-if="scheduleList.length > 0">
            <el-table-column prop="title" label="日程标题" />
            <el-table-column prop="time" label="时间范围" width="120" />
          </el-table>
          <el-empty v-else description="暂无日程" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import dayjs from "dayjs";

const currentDate = ref(new Date());

const selectedDate = computed(() => dayjs(currentDate.value).format("YYYY-MM-DD"));

const scheduleList = ref<any[]>([]);

const hasSchedule = (date: string) => {
  return ["05", "13", "20"].includes(date.split("-")[2]);
};
</script>

<style scoped lang="scss">
.schedule-overview-container {
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
}
</style>