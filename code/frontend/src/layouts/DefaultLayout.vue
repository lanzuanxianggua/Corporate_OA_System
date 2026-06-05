<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();
const title = computed(() => (route.meta?.title as string) || '企业 OA');
</script>

<template>
  <el-container class="default-layout">
    <el-aside width="220px" class="sidebar">
      <div class="logo">{{ title }}</div>
      <el-menu :router="true" :default-active="route.path">
        <el-menu-item index="/dashboard">
          <el-icon><House /></el-icon>
          <span>工作台</span>
        </el-menu-item>
        <el-menu-item index="/hr/leave">
          <el-icon><Calendar /></el-icon>
          <span>请假管理</span>
        </el-menu-item>
        <el-menu-item index="/workflow">
          <el-icon><Document /></el-icon>
          <span>行政审批</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-title">企业 OA 系统 v2</div>
        <el-dropdown>
          <span class="user-info">
            <el-avatar :size="32">U</el-avatar>
            <span>当前用户</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item>个人中心</el-dropdown-item>
              <el-dropdown-item>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<style lang="scss" scoped>
.default-layout {
  height: 100vh;
}
.sidebar {
  background: #001529;
  color: #fff;
  .logo {
    height: 60px;
    line-height: 60px;
    text-align: center;
    font-size: 16px;
    font-weight: 600;
    color: #fff;
  }
  .el-menu {
    background: transparent;
    border: none;
  }
  :deep(.el-menu-item) {
    color: rgba(255, 255, 255, 0.75);
  }
  :deep(.el-menu-item.is-active) {
    background: #1890ff;
    color: #fff;
  }
}
.header {
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  .header-title {
    font-size: 16px;
    font-weight: 500;
  }
  .user-info {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
  }
}
.main {
  background: #f5f5f5;
  padding: 16px;
}
</style>
