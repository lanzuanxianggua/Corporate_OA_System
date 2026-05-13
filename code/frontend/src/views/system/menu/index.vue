<template>
  <div>
    <div class="flex items-center justify-between mb-4">
      <span class="text-lg font-medium">菜单管理</span>
    </div>
    <el-card>
      <el-table :data="menuList" row-key="id" :tree-props="{ children: 'children' }" default-expand-all stripe>
        <el-table-column label="菜单名称" prop="menuName" min-width="200" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.menuType === 'directory' ? '' : 'success'" size="small">
              {{ row.menuType === "directory" ? "目录" : "菜单" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="路由路径" prop="path" />
        <el-table-column label="组件路径" prop="component" />
        <el-table-column label="排序" prop="orderNum" width="80" />
        <el-table-column label="图标" prop="icon" width="100" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { getMenuList } from "@/api/system";

const menuList = ref<any[]>([]);

onMounted(async () => {
  try { const r: any = await getMenuList(); if (r.data) menuList.value = r.data; } catch {}
});
</script>
