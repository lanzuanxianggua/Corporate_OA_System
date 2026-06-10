<template>
  <div>
    <el-row :gutter="20">
      <el-col :xs="24" :md="14">
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
      <el-col :xs="24" :md="10">
        <el-card>
          <template #header>
            <div class="flex justify-between items-center">
              <span class="font-medium">{{ dayjs(selectedDate).format("YYYY年MM月DD日") }} 日程</span>
              <el-button type="primary" size="small" :icon="Plus" @click="addDialogVisible = true">添加日程</el-button>
            </div>
          </template>
          <div v-if="daySchedules.length > 0">
            <el-timeline>
              <el-timeline-item v-for="item in daySchedules" :key="item.id" :timestamp="formatTime(item.startTime ?? '') + ' - ' + formatTime(item.endTime ?? '')" placement="top">
                <div class="font-medium">{{ item.title }}</div>
                <div v-if="item.description" class="text-sm text-[var(--oa-subtle)] mt-1">{{ item.description }}</div>
              </el-timeline-item>
            </el-timeline>
          </div>
          <el-empty v-else description="暂无日程" :image-size="80" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="addDialogVisible" title="添加日程" width="500px">
      <el-form ref="formRef" :model="scheduleForm" :rules="formRules" label-width="80px">
        <el-form-item label="日程标题" prop="title"><el-input v-model="scheduleForm.title" /></el-form-item>
        <el-form-item label="开始时间" prop="startTime"><el-date-picker v-model="scheduleForm.startTime" type="datetime" style="width:100%" /></el-form-item>
        <el-form-item label="结束时间" prop="endTime"><el-date-picker v-model="scheduleForm.endTime" type="datetime" style="width:100%" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="scheduleForm.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="adding" @click="handleAdd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from "vue";
import dayjs from "dayjs";
import { ElMessage } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import { Plus } from "@element-plus/icons-vue";
import { getSchedulePage, addSchedule } from "@/api/schedule";
import { useUserStore } from "@/store/user";
import type { Schedule } from "@/types/api";

const userStore = useUserStore();
const selectedDate = ref(new Date());
const allSchedules = ref<Schedule[]>([]);
const addDialogVisible = ref(false);
const adding = ref(false);
const formRef = ref<FormInstance>();
const scheduleForm = reactive({ title: "", startTime: "", endTime: "", description: "" });

const formRules = reactive<FormRules>({
  title: [{ required: true, message: "请输入日程标题", trigger: "blur" }],
  startTime: [{ required: true, message: "请选择开始时间", trigger: "change" }],
  endTime: [{ required: true, message: "请选择结束时间", trigger: "change" }]
});

const daySchedules = computed(() => {
  const day = dayjs(selectedDate.value).format("YYYY-MM-DD");
  return allSchedules.value.filter((s) => {
    const sDay = dayjs(s.startTime).format("YYYY-MM-DD");
    return sDay === day;
  });
});

const hasSchedule = (day: string) => allSchedules.value.some((s) => dayjs(s.startTime).format("YYYY-MM-DD") === day);
const formatTime = (t: string) => t ? dayjs(t).format("HH:mm") : "";

const fetchSchedules = async () => {
  try {
    const empId = userStore.userInfo?.empId || userStore.userInfo?.id;
    const res = await getSchedulePage({ pageNum: 1, pageSize: 200, empId });
    if (res.data?.list) allSchedules.value = res.data.list;
  } catch {}
};

const handleAdd = async () => {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
  } catch {
    return;
  }
  adding.value = true;
  try {
    await addSchedule({
      ...scheduleForm,
      empId: userStore.userInfo?.empId || userStore.userInfo?.id,
      startTime: dayjs(scheduleForm.startTime).format("YYYY-MM-DD HH:mm:ss"),
      endTime: dayjs(scheduleForm.endTime).format("YYYY-MM-DD HH:mm:ss")
    });
    ElMessage.success("添加成功");
    addDialogVisible.value = false;
    Object.assign(scheduleForm, { title: "", startTime: "", endTime: "", description: "" });
    await fetchSchedules();
  } catch (e: any) { ElMessage.error(e.message || "添加失败"); }
  finally { adding.value = false; }
};

onMounted(fetchSchedules);
</script>
