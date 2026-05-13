<template>
  <div class="menu-container">
    <el-card>
      <template #header>
        <span>菜单管理</span>
      </template>
      <el-table :data="tableData" row-key="id" :tree-props="{ children: 'children' }" stripe>
        <el-table-column prop="menuName" label="菜单名称" width="200" />
        <el-table-column prop="menuType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.menuType === '目录' ? 'warning' : 'success'" size="small">
              {{ row.menuType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由路径" />
        <el-table-column prop="component" label="组件路径" />
        <el-table-column prop="orderNum" label="排序" width="80" />
        <el-table-column prop="icon" label="图标" width="100">
          <template #default="{ row }">
            <el-icon v-if="row.icon"><component :is="row.icon" /></el-icon>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";

const tableData = ref([
  {
    id: 1,
    menuName: "系统管理",
    menuType: "目录",
    path: "/system",
    component: "",
    orderNum: 1,
    icon: "Setting",
    children: [
      { id: 11, menuName: "员工管理", menuType: "菜单", path: "/system/user", component: "/system/user/index.vue", orderNum: 1, icon: "" },
      { id: 12, menuName: "角色管理", menuType: "菜单", path: "/system/role", component: "/system/role/index.vue", orderNum: 2, icon: "" },
      { id: 13, menuName: "菜单管理", menuType: "菜单", path: "/system/menu", component: "/system/menu/index.vue", orderNum: 3, icon: "" },
      { id: 14, menuName: "部门管理", menuType: "菜单", path: "/system/dept", component: "/system/dept/index.vue", orderNum: 4, icon: "" }
    ]
  },
  {
    id: 2,
    menuName: "OA办公",
    menuType: "目录",
    path: "/oa",
    component: "",
    orderNum: 2,
    icon: "Document",
    children: [
      { id: 21, menuName: "工作台", menuType: "菜单", path: "/oa/workbench", component: "/oa/workbench/index.vue", orderNum: 1, icon: "" },
      { id: 22, menuName: "考勤打卡", menuType: "菜单", path: "/oa/attendance/clock", component: "/oa/attendance/clock/index.vue", orderNum: 2, icon: "" }
    ]
  }
]);
</script>

<style scoped lang="scss">
.menu-container {
}
</style>