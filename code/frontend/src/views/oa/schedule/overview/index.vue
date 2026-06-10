<template>
  <div>
    <el-row :gutter="20">
      <el-col :xs="24" :md="16">
        <el-card>
          <template #header><span class="font-medium">月历</span></template>
          <el-calendar v-model="selectedDate">
            <template #date-cell="{ data }">
              <div class="relative">
                {{ data.day.split("-")[2] }}
                <span v-if="hasSchedule(data.day)" class="absolute bottom-0 left-1/2 -translate-x-1/2 w-1.5 h-1.5 bg-[var(--oa-primary)] rounded-full"></span>
              </div>
            </template>
          </el-calendar>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card>
          <template #header>
            <span class="font-medium">{{ dayjs(selectedDate).format("YYYY年MM月DD日") }} 日程</span>
          </template>
          <el-table :data="daySchedules" v-if="daySchedules.length > 0" size="small">
            <el-table-column label="日程标题" prop="title" />
            <el-table-column label="时间范围" width="140">
              <template #default="{ row }">{{ formatTime(row.startTime) }} - {{ formatTime(row.endTime) }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="暂无日程" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import dayjs from "dayjs";
import { getSchedulePage } from "@/api/schedule";

const selectedDate = ref(new Date());
const allSchedules = ref<any[]>([]);

const daySchedules = computed(() => {
  const day = dayjs(selectedDate.value).format("YYYY-MM-DD");
  return allSchedules.value.filter((s) => dayjs(s.startTime).format("YYYY-MM-DD") === day);
});

const hasSchedule = (day: string) => allSchedules.value.some((s) => dayjs(s.startTime).format("YYYY-MM-DD") === day);
const formatTime = (t: string) => t ? dayjs(t).format("HH:mm") : "";

onMounted(async () => {
  try {
    const res: any = await getSchedulePage({ pageNum: 1, pageSize: 200 });
    if (res.data?.list) allSchedules.value = res.data.list;
  } catch {}
});
</script>
