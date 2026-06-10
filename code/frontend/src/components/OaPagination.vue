<template>
  <div class="oa-pagination-bar">
    <div class="oa-pagination-summary">
      <span>共 {{ safeTotal }} 条</span>
      <span>第 {{ safeCurrentPage }} / {{ pageCount }} 页</span>
    </div>

    <div class="oa-pagination-controls">
      <div class="oa-page-size-control">
        <span>每页</span>
        <el-select
          :model-value="safePageSize"
          size="small"
          class="oa-page-size-select"
          @change="handlePageSizeChange"
        >
          <el-option v-for="size in normalizedPageSizes" :key="size" :label="`${size} 条`" :value="size" />
        </el-select>
      </div>

      <el-pagination
        class="oa-pagination"
        :current-page="safeCurrentPage"
        :page-size="safePageSize"
        :total="safeTotal"
        :pager-count="5"
        layout="prev, pager, next, jumper"
        background
        @current-change="handleCurrentPageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";

defineOptions({ inheritAttrs: false });

const props = withDefaults(defineProps<{
  currentPage?: number;
  pageSize?: number;
  total?: number;
  pageSizes?: number[];
}>(), {
  currentPage: 1,
  pageSize: 10,
  total: 0,
  pageSizes: () => [10, 20, 50]
});

const emit = defineEmits<{
  "update:current-page": [value: number];
  "update:page-size": [value: number];
  "current-change": [value: number];
  "size-change": [value: number];
  change: [currentPage: number, pageSize: number];
}>();

const safeTotal = computed(() => Number.isFinite(Number(props.total)) ? Math.max(0, Number(props.total)) : 0);
const safePageSize = computed(() => Number.isFinite(Number(props.pageSize)) ? Math.max(1, Number(props.pageSize)) : 10);
const pageCount = computed(() => Math.max(1, Math.ceil(safeTotal.value / safePageSize.value)));
const safeCurrentPage = computed(() => {
  const page = Number.isFinite(Number(props.currentPage)) ? Number(props.currentPage) : 1;
  return Math.min(Math.max(1, page), pageCount.value);
});
const normalizedPageSizes = computed(() =>
  Array.from(new Set([...props.pageSizes, safePageSize.value]))
    .filter(size => Number.isFinite(Number(size)) && Number(size) > 0)
    .map(Number)
    .sort((a, b) => a - b)
);

function handleCurrentPageChange(page: number) {
  emit("update:current-page", page);
  emit("current-change", page);
  emit("change", page, safePageSize.value);
}

function handlePageSizeChange(size: number) {
  const nextPage = 1;
  emit("update:page-size", size);
  emit("update:current-page", nextPage);
  emit("size-change", size);
  emit("change", nextPage, size);
}
</script>
