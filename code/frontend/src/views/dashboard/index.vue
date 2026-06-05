<template>
  <div>
    <!-- 问候区域 -->
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-[#303133] mb-2">
        {{ greeting }}，{{ userInfo?.realName || userInfo?.username || "用户" }}
      </h1>
      <p class="text-sm text-[#909399]">{{ currentDate }}</p>
    </div>

    <!-- 信息卡片 -->
    <el-row :gutter="20" class="mb-5">
      <el-col :span="6">
        <div
          class="bg-white rounded-lg p-5 flex items-center gap-4"
          style="box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06)"
        >
          <div
            class="w-14 h-14 rounded-lg flex items-center justify-center"
            style="background-color: #e6f7ff"
          >
            <el-icon :size="24" color="#409EFF"><User /></el-icon>
          </div>
          <div class="flex flex-col">
            <span class="text-2xl font-bold text-[#303133]">
              {{ userInfo?.realName || "-" }}
            </span>
            <span class="text-sm text-[#909399] mt-1">姓名</span>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div
          class="bg-white rounded-lg p-5 flex items-center gap-4"
          style="box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06)"
        >
          <div
            class="w-14 h-14 rounded-lg flex items-center justify-center"
            style="background-color: #fff7e6"
          >
            <el-icon :size="24" color="#E6A23C"><OfficeBuilding /></el-icon>
          </div>
          <div class="flex flex-col">
            <span class="text-2xl font-bold text-[#303133]">
              {{ userInfo?.deptName || "-" }}
            </span>
            <span class="text-sm text-[#909399] mt-1">部门</span>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div
          class="bg-white rounded-lg p-5 flex items-center gap-4"
          style="box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06)"
        >
          <div
            class="w-14 h-14 rounded-lg flex items-center justify-center"
            style="background-color: #f0f9ff"
          >
            <el-icon :size="24" color="#67C23A"><UserFilled /></el-icon>
          </div>
          <div class="flex flex-col">
            <span class="text-2xl font-bold text-[#303133]">
              {{ userInfo?.username || "-" }}
            </span>
            <span class="text-sm text-[#909399] mt-1">用户名</span>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div
          class="bg-white rounded-lg p-5 flex items-center gap-4"
          style="box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06)"
        >
          <div
            class="w-14 h-14 rounded-lg flex items-center justify-center"
            style="background-color: #f9f0ff"
          >
            <el-icon :size="24" color="#9254de"><Setting /></el-icon>
          </div>
          <div class="flex flex-col">
            <span class="text-2xl font-bold text-[#303133]">
              {{ (userInfo?.roles || []).join("、") || "-" }}
            </span>
            <span class="text-sm text-[#909399] mt-1">角色</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 权限展示 -->
    <el-card class="mb-5">
      <template #header>
        <span class="font-medium">当前权限 ({{ (userInfo?.permissions || []).length }} 项)</span>
      </template>
      <div v-if="(userInfo?.permissions || []).length">
        <el-tag
          v-for="perm in userInfo?.permissions"
          :key="perm"
          class="mr-2 mb-2"
          type="info"
          effect="plain"
        >
          {{ perm }}
        </el-tag>
      </div>
      <el-empty v-else description="暂无权限数据" :image-size="50" />
    </el-card>

    <!-- 数据看板占位卡片（用户要求暂存） -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="mb-5">
          <template #header>
            <span class="font-medium">快速入口</span>
          </template>
          <div class="grid grid-cols-4 gap-4">
            <div
              v-for="item in quickEntries"
              :key="item.label"
              class="flex flex-col items-center gap-2 py-3 cursor-pointer rounded-lg hover:bg-[#f5f7fa] transition-colors"
              @click="$router.push(item.path)"
            >
              <el-icon :size="28" :color="item.color">
                <component :is="item.icon" />
              </el-icon>
              <span class="text-xs text-[#606266]">{{ item.label }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="mb-5">
          <template #header>
            <span class="font-medium">数据统计（暂存）</span>
          </template>
          <el-skeleton :rows="3" animated />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import dayjs from "dayjs";
import { authApi } from "@/api/auth";
import type { UserInfo } from "@/api/auth";
import { ElMessage } from "element-plus";

const userInfo = ref<UserInfo | null>(null);

const greeting = computed(() => {
  const h = new Date().getHours();
  if (h < 12) return "早上好";
  if (h < 18) return "下午好";
  return "晚上好";
});

const currentDate = computed(() => dayjs().format("YYYY年MM月DD日 dddd"));

const quickEntries = [
  {
    label: "请假申请",
    icon: "DocumentAdd",
    color: "#409EFF",
    path: "/oa/leave/apply",
  },
  {
    label: "公告通知",
    icon: "Bell",
    color: "#E6A23C",
    path: "/oa/notice/list",
  },
  {
    label: "消息中心",
    icon: "ChatDotRound",
    color: "#9254de",
    path: "/oa/message/list",
  },
  {
    label: "我的日程",
    icon: "Calendar",
    color: "#67C23A",
    path: "/oa/schedule/index",
  },
];

onMounted(async () => {
  try {
    userInfo.value = await authApi.getCurrent();
  } catch (error: any) {
    ElMessage.error(error?.message || "获取用户信息失败");
  }
});
</script>
