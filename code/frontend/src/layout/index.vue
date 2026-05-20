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
        <el-menu-item index="/welcome">
          <el-icon><HomeFilled /></el-icon>
          <template #title>首页</template>
        </el-menu-item>

        <el-sub-menu index="oa">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>OA办公</span>
          </template>
          <el-menu-item index="/oa/workbench">工作台</el-menu-item>
          <el-menu-item index="/oa/attendance/clock">考勤打卡</el-menu-item>
          <el-menu-item index="/oa/attendance/record">考勤记录</el-menu-item>
          <el-menu-item index="/oa/leave/apply">请假申请</el-menu-item>
          <el-menu-item index="/oa/business-trip/apply">出差申请</el-menu-item>
          <el-menu-item index="/oa/outing/apply">外出申请</el-menu-item>
          <el-menu-item index="/oa/purchase/apply">采购申请</el-menu-item>
          <el-menu-item index="/oa/expense/apply">经费申请</el-menu-item>
          <el-menu-item index="/oa/notice/list">公告通知</el-menu-item>
          <el-menu-item index="/oa/document/list">文档中心</el-menu-item>
          <el-menu-item index="/oa/schedule/index">我的日程</el-menu-item>
          <el-menu-item index="/oa/message/list">消息中心</el-menu-item>
          <el-menu-item index="/oa/report/personal">个人报表</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="userStore.isAdmin()" index="oa-admin">
          <template #title>
            <el-icon><DataAnalysis /></el-icon>
            <span>OA管理</span>
          </template>
          <el-menu-item index="/oa/dashboard">数据看板</el-menu-item>
          <el-menu-item index="/oa/attendance/manage">考勤管理</el-menu-item>
          <el-menu-item index="/oa/leave/approval">请假审批</el-menu-item>
          <el-menu-item index="/oa/business-trip/approval">出差审批</el-menu-item>
          <el-menu-item index="/oa/outing/approval">外出审批</el-menu-item>
          <el-menu-item index="/oa/purchase/approval">采购审批</el-menu-item>
          <el-menu-item index="/oa/expense/approval">经费审批</el-menu-item>
          <el-menu-item index="/oa/notice/manage">公告管理</el-menu-item>
          <el-menu-item index="/oa/document/manage">文档管理</el-menu-item>
          <el-menu-item index="/oa/schedule/overview">日程总览</el-menu-item>
          <el-menu-item index="/oa/message/send">发送消息</el-menu-item>
          <el-menu-item index="/oa/report/admin">管理员报表</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="userStore.isAdmin()" index="system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/system/user">员工管理</el-menu-item>
          <el-menu-item index="/system/role">角色管理</el-menu-item>
          <el-menu-item index="/system/menu">菜单管理</el-menu-item>
          <el-menu-item index="/system/dept">部门管理</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="userStore.isAdmin()" index="monitor">
          <template #title>
            <el-icon><Monitor /></el-icon>
            <span>系统监控</span>
          </template>
          <el-menu-item index="/monitor/online">在线用户</el-menu-item>
          <el-menu-item index="/monitor/logs/login">登录日志</el-menu-item>
          <el-menu-item index="/monitor/logs/operation">操作日志</el-menu-item>
          <el-menu-item index="/monitor/logs/system">系统日志</el-menu-item>
        </el-sub-menu>
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
import { ref, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessageBox } from "element-plus";
import { useUserStore } from "@/store/user";
import { getUnreadCount } from "@/api/message";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const isCollapsed = ref(false);
const unreadCount = ref(0);

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
  setInterval(fetchUnreadCount, 60000);
});
</script>
