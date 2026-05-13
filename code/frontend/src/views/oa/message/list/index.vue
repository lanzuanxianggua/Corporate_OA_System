<template>
  <div>
    <div v-if="messageList.length > 0" class="space-y-3">
      <div
        v-for="item in messageList"
        :key="item.id"
        class="p-4 rounded-lg border border-[#ebeef5] transition-colors cursor-pointer"
        :class="item.isRead ? 'bg-white' : 'bg-[#ecf5ff]'"
        @click="openDetail(item)"
      >
        <div class="flex items-start gap-3">
          <el-avatar :size="40" :style="{ backgroundColor: avatarColor(item.senderName) }" class="shrink-0">
            {{ item.senderName?.charAt(0) || "?" }}
          </el-avatar>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 mb-1">
              <span class="font-medium text-sm text-[#303133]">{{ item.senderName }}</span>
              <span class="text-xs text-[#909399]">{{ formatTime(item.createTime) }}</span>
            </div>
            <div class="text-sm text-[#606266] truncate">{{ item.content }}</div>
          </div>
          <el-button v-if="!item.isRead" type="primary" link size="small" @click.stop="handleRead(item)">
            标记已读
          </el-button>
        </div>
      </div>
    </div>
    <el-empty v-else description="暂无消息" />

    <div class="flex justify-end mt-4">
      <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" layout="total, prev, pager, next" @current-change="fetchList" />
    </div>

    <el-dialog v-model="dialogVisible" :title="currentMsg?.title || '消息详情'" width="500px">
      <div class="text-sm text-[#606266] leading-6">{{ currentMsg?.content }}</div>
      <div class="mt-4 text-xs text-[#909399]">{{ currentMsg?.senderName }} · {{ formatTime(currentMsg?.createTime) }}</div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { ElMessage } from "element-plus";
import { getMessagePage, markAsRead } from "@/api/message";

const messageList = ref<any[]>([]);
const dialogVisible = ref(false);
const currentMsg = ref<any>(null);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);

const colors = ["#409EFF", "#67C23A", "#E6A23C", "#F56C6C", "#9254de"];
const avatarColor = (name?: string) => colors[(name?.charCodeAt(0) || 0) % colors.length];

const formatTime = (time?: string) => {
  if (!time) return "-";
  return time.replace("T", " ").substring(0, 16);
};

const fetchList = async () => {
  try {
    const res: any = await getMessagePage({ pageNum: pageNum.value, pageSize: pageSize.value });
    if (res.data?.list) {
      messageList.value = res.data.list;
      total.value = res.data.total || 0;
    }
  } catch {}
};

const handleRead = async (item: any) => {
  try {
    await markAsRead(item.id);
    item.isRead = 1;
    ElMessage.success("已标记为已读");
  } catch {}
};

const openDetail = (item: any) => {
  currentMsg.value = item;
  dialogVisible.value = true;
  if (!item.isRead) handleRead(item);
};

onMounted(fetchList);
</script>
