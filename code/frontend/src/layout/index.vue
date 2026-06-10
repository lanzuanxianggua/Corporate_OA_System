<template>
  <div class="oa-app-shell flex h-screen">
    <!-- 侧边栏 -->
    <div
      v-if="!isMobile"
      class="oa-sidebar transition-all duration-300 flex flex-col shrink-0"
      :class="isCollapsed ? 'w-16' : 'w-[210px]'"
    >
      <div
        class="oa-sidebar-header h-14 flex items-center justify-center gap-2 shrink-0"
      >
        <el-icon :size="24" color="var(--oa-primary)"><OfficeBuilding /></el-icon>
        <span
          v-if="!isCollapsed"
          class="oa-brand-text text-lg font-bold whitespace-nowrap"
        >
          OA办公系统
        </span>
      </div>
      <SideMenu :active-menu="activeMenu" :collapsed="isCollapsed" />
    </div>

    <!-- 主区域 -->
    <div class="flex-1 flex flex-col overflow-hidden min-w-0">
      <!-- 顶部导航栏 -->
      <div
        class="oa-topbar h-14 flex items-center justify-between px-5 shrink-0"
      >
        <div class="oa-topbar-left flex items-center gap-4 min-w-0">
          <el-button text class="oa-icon-button" :aria-label="isMobile ? '打开菜单' : '折叠菜单'" @click="handleMenuButtonClick">
            <el-icon :size="20">
              <MenuIcon v-if="isMobile" />
              <Expand v-else-if="isCollapsed" />
              <Fold v-else />
            </el-icon>
          </el-button>
          <el-breadcrumb class="oa-breadcrumb" separator="/">
            <el-breadcrumb-item :to="{ path: '/welcome' }">
              首页
            </el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta.title !== '首页'">
              {{ route.meta.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="oa-topbar-actions flex items-center gap-4">
          <el-badge v-if="unreadCount > 0" :value="unreadCount" :max="99">
            <el-button text @click="router.push('/oa/message/list')">
              <el-icon :size="20"><Bell /></el-icon>
            </el-button>
          </el-badge>
          <el-button v-else text @click="router.push('/oa/message/list')">
            <el-icon :size="20"><Bell /></el-icon>
          </el-button>
          <el-dropdown trigger="click" @command="handleThemeCommand">
            <el-button text class="oa-theme-button" :aria-label="themeStore.isDark ? '切换主题，当前深色' : '切换主题，当前浅色'">
              <el-icon :size="20">
                <component :is="themeStore.isDark ? Moon : Sunny" />
              </el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="light">
                  <el-icon><Sunny /></el-icon>
                  浅色
                </el-dropdown-item>
                <el-dropdown-item command="dark">
                  <el-icon><Moon /></el-icon>
                  深色
                </el-dropdown-item>
                <el-dropdown-item command="system">
                  <el-icon><Monitor /></el-icon>
                  跟随系统
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-dropdown @command="handleCommand">
            <span class="flex items-center cursor-pointer gap-1">
              <el-avatar :size="32">
                {{ userStore.userInfo?.empName?.charAt(0) || "U" }}
              </el-avatar>
              <span class="oa-user-name text-sm">
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
      <div class="oa-content flex-1 p-5 overflow-y-auto">
        <router-view />
      </div>
    </div>

    <el-drawer
      v-model="mobileMenuVisible"
      direction="ltr"
      size="min(84vw, 310px)"
      :with-header="false"
      class="oa-mobile-drawer"
    >
      <div class="oa-sidebar-header h-14 flex items-center justify-center gap-2 shrink-0">
        <el-icon :size="24" color="var(--oa-primary)"><OfficeBuilding /></el-icon>
        <span class="oa-brand-text text-lg font-bold whitespace-nowrap">OA办公系统</span>
      </div>
      <SideMenu :active-menu="activeMenu" @navigate="mobileMenuVisible = false" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessageBox, ElNotification } from "element-plus";
import { Menu as MenuIcon, Monitor, Moon, Sunny } from "@element-plus/icons-vue";
import { useUserStore } from "@/store/user";
import { useThemeStore, type ThemeMode } from "@/store/theme";
import { getUnreadCount } from "@/api/message";
import SideMenu from "./SideMenu.vue";
import { wsClient } from "@/utils/websocket";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const themeStore = useThemeStore();
const isCollapsed = ref(false);
const isMobile = ref(false);
const mobileMenuVisible = ref(false);
const unreadCount = ref(0);
let unreadTimer: ReturnType<typeof setInterval> | null = null;

const activeMenu = computed(() => route.path);

const updateViewport = () => {
  if (typeof window === "undefined") return;
  isMobile.value = window.innerWidth <= 768;
  if (!isMobile.value) {
    mobileMenuVisible.value = false;
  }
};

const handleMenuButtonClick = () => {
  if (isMobile.value) {
    mobileMenuVisible.value = true;
    return;
  }
  isCollapsed.value = !isCollapsed.value;
};

const handleThemeCommand = (command: string) => {
  themeStore.setThemeMode(command as ThemeMode);
};

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
  updateViewport();
  window.addEventListener("resize", updateViewport);
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

watch(() => route.fullPath, () => {
  mobileMenuVisible.value = false;
});

onUnmounted(() => {
  window.removeEventListener("resize", updateViewport);
  wsClient.disconnect();
  if (unreadTimer) {
    clearInterval(unreadTimer);
    unreadTimer = null;
  }
});
</script>

<style scoped>
.oa-app-shell {
  background: var(--oa-bg);
  color: var(--oa-text);
}

.oa-sidebar {
  background: var(--oa-surface);
  border-right: 1px solid var(--oa-border);
}

.oa-sidebar-header,
.oa-topbar {
  background: var(--oa-surface);
  border-bottom: 1px solid var(--oa-border);
}

.oa-brand-text {
  color: var(--oa-primary);
}

.oa-topbar {
  color: var(--oa-text);
}

.oa-user-name {
  color: var(--oa-text-soft);
}

.oa-content {
  background: var(--oa-bg);
}

.oa-theme-button {
  color: var(--oa-text-soft);
}

:deep(.el-menu) {
  border-right: 0;
}

:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  background-color: var(--oa-primary-soft);
}

:deep(.el-menu-item.is-active) {
  background-color: var(--oa-primary-soft);
}

:deep(.oa-mobile-drawer .el-drawer__body) {
  display: flex;
  flex-direction: column;
  padding: 0;
  background: var(--oa-surface);
}

.oa-icon-button {
  min-width: 40px;
  min-height: 40px;
}

@media (max-width: 768px) {
  .oa-app-shell {
    height: 100dvh;
  }

  .oa-topbar {
    height: 52px;
    padding: 0 10px;
  }

  .oa-topbar-left,
  .oa-topbar-actions {
    gap: 8px;
  }

  .oa-breadcrumb,
  .oa-user-name {
    display: none;
  }

  .oa-content {
    padding: 10px;
  }
}
</style>
