<template>
  <div>
    <div class="mb-4">
      <span class="text-lg font-medium">消息中心</span>
    </div>
    <el-card>
      <template #header>
        <div class="flex items-center justify-between">
          <span>我的消息</span>
          <el-button type="primary" link @click="$router.push('/oa/message/send')">发送消息</el-button>
        </div>
      </template>
      <div v-if="messages.length > 0" class="space-y-3">
        <div
          v-for="item in messages"
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
                <span class="font-medium text-sm text-[#303133]">{{ item.senderName || "系统" }}</span>
                <el-tag v-if="!item.isRead" type="danger" size="small">未读</el-tag>
                <span class="text-xs text-[#909399]">{{ formatTime(item.createTime) }}</span>
              </div>
              <div class="text-sm text-[#606266] font-medium">{{ item.title }}</div>
              <div class="text-sm text-[#909399] truncate">{{ item.content }}</div>
            </div>
            <el-button v-if="!item.isRead" type="primary" link size="small" @click.stop="handleRead(item)">
              标记已读
            </el-button>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无消息" />

      <div class="flex justify-end mt-4">
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @size-change="fetchMessages"
          @current-change="fetchMessages" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="currentMsg?.title || '消息详情'" width="500px">
      <div class="text-sm text-[#606266] leading-6">{{ currentMsg?.content }}</div>
      <div class="mt-4 text-xs text-[#909399]">{{ currentMsg?.senderName || "系统" }} · {{ formatTime(currentMsg?.createTime) }}</div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { ElMessage } from "element-plus";
import { getMessagePage, markAsRead } from "@/api/message";
import { formatTime } from "@/utils/format";
import type { Message } from "@/types/api";

const messages = ref<Message[]>([]);
const dialogVisible = ref(false);
const currentMsg = ref<Message | null>(null);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);

const colors = ["#409EFF", "#67C23A", "#E6A23C", "#F56C6C", "#9254de"];
const avatarColor = (name?: string) => colors[(name?.charCodeAt(0) || 0) % colors.length];

const handleRead = async (item: Message) => {
  try {
    await markAsRead(item.id!);
    item.isRead = 1;
    ElMessage.success("已标记为已读");
  } catch {
    ElMessage.error("标记已读失败");
  }
};

const openDetail = (item: Message) => {
  currentMsg.value = item;
  dialogVisible.value = true;
  if (!item.isRead) handleRead(item);
};

const fetchMessages = async () => {
  try {
    const res = await getMessagePage({ pageNum: pageNum.value, pageSize: pageSize.value });
    if (res.data) {
      messages.value = res.data.list || [];
      total.value = res.data.total || 0;
    }
  } catch {
    messages.value = [];
  }
};

onMounted(fetchMessages);
</script>
