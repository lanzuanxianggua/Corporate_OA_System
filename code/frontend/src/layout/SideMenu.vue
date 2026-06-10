<template>
  <el-menu
    :default-active="activeMenu"
    :collapse="collapsed"
    :router="true"
    class="border-r-0 flex-1 overflow-y-auto"
    background-color="var(--oa-surface)"
    text-color="var(--oa-text-soft)"
    active-text-color="var(--oa-primary)"
    @select="emit('navigate')"
  >
    <template v-for="(item, idx) in menuConfig" :key="'root-' + idx">
      <template v-if="!item.roles || userStore.hasAnyRole(item.roles)">
        <el-menu-item v-if="!item.children" :index="item.path ?? ''">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>

        <el-sub-menu v-else :index="'menu-' + idx">
          <template #title>
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </template>

          <template v-for="(child, cidx) in item.children" :key="'menu-' + idx + '-' + cidx">
            <el-sub-menu
              v-if="child.children && child.children.length"
              :index="'menu-' + idx + '-' + cidx"
            >
              <template #title>{{ child.title }}</template>
              <el-menu-item
                v-for="(nested, nidx) in child.children"
                v-show="!nested.roles || userStore.hasAnyRole(nested.roles)"
                :key="'menu-' + idx + '-' + cidx + '-' + nidx"
                :index="nested.path ?? ''"
              >
                {{ nested.title }}
              </el-menu-item>
            </el-sub-menu>

            <el-menu-item
              v-else
              v-show="!child.roles || userStore.hasAnyRole(child.roles)"
              :index="child.path ?? ''"
            >
              {{ child.title }}
            </el-menu-item>
          </template>
        </el-sub-menu>
      </template>
    </template>
  </el-menu>
</template>

<script setup lang="ts">
import { useUserStore } from "@/store/user";
import { menuConfig } from "./menuConfig";

withDefaults(defineProps<{
  activeMenu: string;
  collapsed?: boolean;
}>(), {
  collapsed: false
});

const emit = defineEmits<{
  navigate: [];
}>();

const userStore = useUserStore();
</script>
