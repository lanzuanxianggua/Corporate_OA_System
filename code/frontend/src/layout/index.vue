<template>
  <div class="layout-container">
    <div class="layout-sidebar" :class="{ collapsed: isCollapsed }">
      <div class="logo-area">
        <el-icon v-if="!isCollapsed"><OfficeBuilding /></el-icon>
        <span v-if="!isCollapsed">OA办公系统</span>
        <el-icon v-else><OfficeBuilding /></el-icon>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapsed"
        :router="true"
        class="sidebar-menu"
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
          <el-menu-item index="/oa/notice/list">公告列表</el-menu-item>
          <el-menu-item index="/oa/document/list">文档中心</el-menu-item>
          <el-menu-item index="/oa/schedule/index">我的日程</el-menu-item>
          <el-menu-item index="/oa/message/list">消息中心</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="report">
          <template #title>
            <el-icon><DataAnalysis /></el-icon>
            <span>数据报表</span>
          </template>
          <el-menu-item index="/oa/report/personal">个人报表</el-menu-item>
          <el-menu-item index="/oa/report/admin">管理员报表</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/system/user">员工管理</el-menu-item>
          <el-menu-item index="/system/role">角色管理</el-menu-item>
          <el-menu-item index="/system/menu">菜单管理</el-menu-item>
          <el-menu-item index="/system/dept">部门管理</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="monitor">
          <template #title>
            <el-icon><Monitor /></el-icon>
            <span>系统监控</span>
          </template>
          <el-menu-item index="/monitor/online">在线用户</el-menu-item>
          <el-menu-item index="/monitor/logs/login">登录日志</el-menu-item>
          <el-menu-item index="/monitor/logs/operation">操作日志</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </div>
    <div class="layout-main">
      <div class="layout-navbar">
        <div class="navbar-left">
          <el-button text @click="isCollapsed = !isCollapsed">
            <el-icon size="20"><Expand v-if="isCollapsed" /><Fold v-else /></el-icon>
          </el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/welcome' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="navbar-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" style="margin-right: 8px">张</el-avatar>
              <span>张三</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="settings">账号设置</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
      <div class="layout-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessageBox } from "element-plus";

const route = useRoute();
const router = useRouter();
const isCollapsed = ref(false);

const activeMenu = computed(() => route.path);

const handleCommand = (command: string) => {
  if (command === "logout") {
    ElMessageBox.confirm("确定要退出登录吗？", "提示", {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning"
    }).then(() => {
      router.push("/login");
    });
  } else if (command === "settings") {
    router.push("/account-settings");
  }
};
</script>

<style scoped lang="scss">
.layout-container {
  display: flex;
  height: 100vh;
  background-color: #f5f7fa;
}

.layout-sidebar {
  width: 210px;
  background-color: #ffffff;
  border-right: 1px solid #ebeef5;
  transition: width 0.3s;
  overflow: hidden;

  &.collapsed {
    width: 64px;
  }
}

.logo-area {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 18px;
  font-weight: bold;
  color: #409EFF;
  border-bottom: 1px solid #ebeef5;
}

.sidebar-menu {
  border-right: none;
}

.layout-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.layout-navbar {
  height: 56px;
  background-color: #ffffff;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.navbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.navbar-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.layout-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}
</style>