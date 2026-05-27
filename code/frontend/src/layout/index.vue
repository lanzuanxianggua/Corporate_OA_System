<template>
  <div class="flex h-screen bg-[#f5f7fa]">
    <!-- 侧边栏 -->
    <div
      class="bg-white border-r border-[#ebeef5] transition-all duration-300 flex flex-col shrink-0"
      :class="isCollapsed ? 'w-16' : 'w-[210px]'"
    >
      <div
        class="h-14 flex items-center justify-center gap-2 border-b border-[#ebeef5] shrink-0"
      >
        <el-icon :size="24" color="#409EFF"><OfficeBuilding /></el-icon>
        <span
          v-if="!isCollapsed"
          class="text-lg font-bold text-[#409EFF] whitespace-nowrap"
        >
          OA办公系统
        </span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapsed"
        :router="true"
        class="border-r-0 flex-1 overflow-y-auto"
        background-color="#ffffff"
        text-color="#303133"
        active-text-color="#409EFF"
      >
        <template v-for="(item, idx) in menuConfig">
          <el-sub-menu
            v-if="item.children && (!item.roles || userStore.isAdmin())"
            :key="'sub-' + idx"
            :index="'menu-' + idx"
          >
            <template #title>
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </template>
            <el-menu-item
              v-for="(child, cidx) in item.children"
              :key="'menu-' + idx + '-' + cidx"
              :index="child.path"
            >
              {{ child.title }}
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item
            v-else-if="!item.children && (!item.roles || userStore.isAdmin())"
            :key="'item-' + idx"
            :index="item.path"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <template #title>{{ item.title }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </div>

    <!-- 主区域 -->
    <div class="flex-1 flex flex-col overflow-hidden min-w-0">
      <!-- 顶部导航栏 -->
      <div
        class="h-14 bg-white border-b border-[#ebeef5] flex items-center justify-between px-5 shrink-0"
      >
        <div class="flex items-center gap-4">
          <el-button text @click="isCollapsed = !isCollapsed">
            <el-icon :size="20">
              <Expand v-if="isCollapsed" />
              <Fold v-else />
            </el-icon>
          </el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/welcome' }">
              首页
            </el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta.title !== '首页'">
              {{ route.meta.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="flex items-center gap-4">
          <el-badge v-if="unreadCount > 0" :value="unreadCount" :max="99">
            <el-button text @click="router.push('/oa/message/list')">
              <el-icon :size="20"><Bell /></el-icon>
            </el-button>
          </el-badge>
          <el-button v-else text @click="router.push('/oa/message/list')">
            <el-icon :size="20"><Bell /></el-icon>
          </el-button>
          <el-dropdown @command="handleCommand">
            <span class="flex items-center cursor-pointer gap-1">
              <el-avatar :size="32">
                {{ userStore.userInfo?.empName?.charAt(0) || "U" }}
              </el-avatar>
              <span class="text-sm text-[#303133]">
                {{ userStore.userInfo?.empName || "用户" }}
              </span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="settings">
                  账号设置
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- 内容区 -->
      <div class="flex-1 p-5 overflow-y-auto">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessageBox, ElNotification } from "element-plus";
import { useUserStore } from "@/store/user";
import { getUnreadCount } from "@/api/message";
import { menuConfig } from "./menuConfig";
import { wsClient } from "@/utils/websocket";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const isCollapsed = ref(false);
const unreadCount = ref(0);
let unreadTimer: ReturnType<typeof setInterval> | null = null;

const activeMenu = computed(() => route.path);

const handleCommand = async (command: string) => {
  if (command === "logout") {
    try {
      await ElMessageBox.confirm("确定要退出登录吗？", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      });
      await userStore.logoutAction();
    } catch {
      // cancelled
    }
  } else if (command === "settings") {
    router.push("/account-settings");
  }
};

const fetchUnreadCount = async () => {
  try {
    const res: any = await getUnreadCount();
    if (res.data !== undefined) {
      unreadCount.value = res.data;
    }
  } catch {
    // ignore
  }
};

onMounted(() => {
  fetchUnreadCount();
  unreadTimer = setInterval(fetchUnreadCount, 60000);

  // WebSocket real-time notifications
  const empId = userStore.userInfo?.id;
  if (empId) {
    wsClient.connect(empId);
    wsClient.on("*", (data) => {
      unreadCount.value++;
      const actionMap: Record<string, string> = {
        approved: "已通过",
        rejected: "已驳回",
        task: "新审批任务"
      };
      const action = actionMap[data.action || data.type] || "新通知";
      ElNotification({
        title: action,
        message: data.description || `您有一条新的${action}`,
        type: data.action === "rejected" ? "warning" : "info",
        duration: 4000
      });
    });
  }
});

onUnmounted(() => {
  wsClient.disconnect();
  if (unreadTimer) {
    clearInterval(unreadTimer);
    unreadTimer = null;
  }
});
</script>
