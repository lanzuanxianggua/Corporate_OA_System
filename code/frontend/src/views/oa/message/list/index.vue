<template>
  <div class="message-list-container">
    <el-card>
      <template #header>
        <span>消息中心</span>
      </template>
      <div class="message-list" v-loading="loading">
        <div
          v-for="item in messageList"
          :key="item.id"
          class="message-card"
          :class="{ unread: !item.isRead }"
          @click="handleRead(item)"
        >
          <div class="message-avatar">
            <el-avatar :size="48" :style="{ backgroundColor: getAvatarColor(item.senderName) }">
              {{ item.senderName?.charAt(0) || '系' }}
            </el-avatar>
          </div>
          <div class="message-content">
            <div class="message-header">
              <span class="sender-name">{{ item.senderName }}</span>
              <span class="send-time">{{ item.createTime }}</span>
            </div>
            <div class="message-title">{{ item.title }}</div>
            <div class="message-body">{{ item.content }}</div>
          </div>
          <div class="message-action">
            <el-tag v-if="!item.isRead" type="warning" size="small">未读</el-tag>
            <el-tag v-else type="success" size="small">已读</el-tag>
          </div>
        </div>
        <el-empty v-if="!loading && messageList.length === 0" description="暂无消息" :image-size="80" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { getUnreadCount, markAsRead } from "@/api/message";

const loading = ref(false);
const messageList = ref<any[]>([]);

const getAvatarColor = (name: string) => {
  const colors = ['#409EFF', '#67C23A', '#E6A23C', '#9254de', '#F56C6C', '#909399'];
  const index = (name?.charCodeAt(0) || 0) % colors.length;
  return colors[index];
};

const loadData = async () => {
  try {
    loading.value = true;
    const res: any = await getUnreadCount();
    console.log("未读消息数量:", res.data);
  } catch (error) {
    console.error("获取未读消息数量失败", error);
  } finally {
    loading.value = false;
  }
};

const handleRead = async (item: any) => {
  if (!item.isRead) {
    try {
      await markAsRead(item.id);
      item.isRead = true;
    } catch (error) {
      console.error("标记已读失败", error);
    }
  }
};

onMounted(() => {
  loadData();
});
</script>

<style scoped lang="scss">
.message-list-container {
  .message-list {
    .message-card {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 16px;
      border-bottom: 1px solid #ebeef5;
      cursor: pointer;
      transition: background-color 0.3s;

      &:hover {
        background-color: #f5f7fa;
      }

      &.unread {
        background-color: #ecf5ff;
      }

      &:last-child {
        border-bottom: none;
      }
    }
  }

  .message-content {
    flex: 1;
    overflow: hidden;
  }

  .message-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 4px;
  }

  .sender-name {
    font-weight: bold;
    color: #303133;
  }

  .send-time {
    font-size: 12px;
    color: #c0c4cc;
  }

  .message-title {
    font-size: 14px;
    font-weight: 500;
    color: #303133;
    margin-bottom: 4px;
  }

  .message-body {
    font-size: 13px;
    color: #909399;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>