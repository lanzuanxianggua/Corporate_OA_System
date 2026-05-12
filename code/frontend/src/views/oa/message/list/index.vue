<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { ElMessage } from "element-plus";
import {
  getMessagePage,
  markMessageRead,
  batchMarkRead
} from "@/api/oa/message";

defineOptions({ name: "OaMessageList" });

/** 消息记录 */
interface MessageRecord {
  id: number;
  title: string;
  senderName: string;
  content: string;
  createTime: string;
  isRead: number;
}

const loading = ref(false);
const messages = ref<MessageRecord[]>([]);
const detailVisible = ref(false);
const currentMessage = ref<MessageRecord | null>(null);

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
});

/** 加载消息列表 */
async function fetchMessages() {
  loading.value = true;
  try {
    const res = await getMessagePage({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    });
    const data = res.data ?? res;
    messages.value = data.list ?? data.records ?? [];
    pagination.total = data.total ?? 0;
  } catch {
    ElMessage.error("获取消息列表失败");
  } finally {
    loading.value = false;
  }
}

/** 查看消息详情 */
async function handleViewDetail(row: MessageRecord) {
  currentMessage.value = { ...row };
  detailVisible.value = true;

  // 自动标记已读
  if (row.isRead === 0) {
    try {
      await markMessageRead(row.id);
      row.isRead = 1;
    } catch {
      // 静默处理
    }
  }
}

/** 标记单条已读 */
async function handleMarkRead(row: MessageRecord) {
  try {
    await markMessageRead(row.id);
    row.isRead = 1;
    ElMessage.success("已标记为已读");
  } catch {
    ElMessage.error("操作失败");
  }
}

/** 全部标记已读 */
async function handleMarkAllRead() {
  try {
    await batchMarkRead();
    messages.value.forEach((m) => (m.isRead = 1));
    ElMessage.success("已全部标记为已读");
  } catch {
    ElMessage.error("操作失败");
  }
}

/** 行点击 */
function handleRowClick(row: MessageRecord) {
  handleViewDetail(row);
}

/** 分页变化 */
function handlePageChange(pageNum: number) {
  pagination.pageNum = pageNum;
  fetchMessages();
}

function handleSizeChange(pageSize: number) {
  pagination.pageSize = pageSize;
  pagination.pageNum = 1;
  fetchMessages();
}

onMounted(() => {
  fetchMessages();
});
</script>

<template>
  <div class="oa-message-list">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span class="card-title">消息中心</span>
          <el-button type="primary" @click="handleMarkAllRead">全部已读</el-button>
        </div>
      </template>

      <el-table
        :data="messages"
        stripe
        v-loading="loading"
        empty-text="暂无消息"
        style="width: 100%"
        @row-click="handleRowClick"
        class="message-table"
      >
        <el-table-column prop="title" label="标题" min-width="250">
          <template #default="{ row }">
            <span :class="{ 'unread-text': row.isRead === 0 }">
              {{ row.title }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="senderName" label="发送人" width="120" align="center" />
        <el-table-column prop="createTime" label="发送时间" width="180" align="center" />
        <el-table-column prop="isRead" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isRead === 1 ? 'success' : 'danger'" size="small">
              {{ row.isRead === 1 ? "已读" : "未读" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.isRead === 0"
              type="primary"
              link
              size="small"
              @click.stop="handleMarkRead(row)"
            >
              标记已读
            </el-button>
            <el-button
              type="primary"
              link
              size="small"
              @click.stop="handleViewDetail(row)"
            >
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- 消息详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      title="消息详情"
      width="600px"
    >
      <template v-if="currentMessage">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="标题">
            {{ currentMessage.title }}
          </el-descriptions-item>
          <el-descriptions-item label="发送人">
            {{ currentMessage.senderName }}
          </el-descriptions-item>
          <el-descriptions-item label="发送时间">
            {{ currentMessage.createTime }}
          </el-descriptions-item>
          <el-descriptions-item label="内容">
            <div class="message-content">{{ currentMessage.content }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.oa-message-list {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.message-table {
  cursor: pointer;
}

.unread-text {
  font-weight: 700;
  color: #303133;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.message-content {
  white-space: pre-wrap;
  line-height: 1.6;
  max-height: 300px;
  overflow-y: auto;
}
</style>
