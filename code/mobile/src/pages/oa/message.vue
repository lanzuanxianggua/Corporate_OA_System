<template>
  <view class="container">
    <!-- Send button -->
    <button class="send-btn" @click="openSend">发送消息</button>

    <!-- Message list -->
    <view class="card" v-for="item in list" :key="item.id" @click="handleRead(item)">
      <view class="flex-between">
        <text class="msg-sender">{{ item.senderName || item.sender || '系统' }}</text>
        <text :class="item.isRead ? 'text-gray' : 'text-primary'">
          {{ item.isRead ? '已读' : '未读' }}
        </text>
      </view>
      <text class="msg-content mt-20">{{ item.content }}</text>
      <text class="text-gray mt-20">{{ formatTime(item.createTime) }}</text>
    </view>

    <view class="empty" v-if="!loading && list.length === 0">
      <text class="text-gray">暂无消息</text>
    </view>

    <view class="load-more" v-if="loading">
      <text class="text-gray">加载中...</text>
    </view>

    <!-- Send dialog -->
    <view class="mask" v-if="showSend" @click="showSend = false">
      <view class="dialog" @click.stop>
        <text class="section-title">发送消息</text>

        <!-- Step 1: Search & Select employee -->
        <view class="form-item" v-if="!selectedEmp">
          <text class="form-label">选择接收人</text>
          <input class="form-input" v-model="searchKeyword" placeholder="搜索员工姓名..." @input="searchEmployee" />
        </view>
        <view class="emp-list" v-if="!selectedEmp && searchResults.length > 0">
          <view
            v-for="emp in searchResults"
            :key="emp.id"
            class="emp-item"
            @click="selectEmployee(emp)"
          >
            <text class="emp-name">{{ emp.empName }}</text>
            <text class="emp-code text-gray">{{ emp.empCode }}</text>
          </view>
        </view>
        <view v-if="!selectedEmp && searchKeyword && searchResults.length === 0 && !searching" class="text-gray" style="text-align:center;padding:20rpx">
          未找到匹配的员工
        </view>

        <!-- Selected employee -->
        <view class="form-item" v-if="selectedEmp">
          <text class="form-label">接收人</text>
          <view class="selected-emp">
            <text>{{ selectedEmp.empName }}（{{ selectedEmp.empCode }}）</text>
            <text class="text-primary" @click="selectedEmp = null; searchKeyword = ''; searchResults = []">更换</text>
          </view>
        </view>

        <!-- Step 2: Message content -->
        <view class="form-item">
          <text class="form-label">消息内容</text>
          <textarea class="form-textarea" v-model="sendContent" placeholder="请输入消息内容" :maxlength="500" />
        </view>

        <view class="dialog-btns">
          <button class="dialog-btn cancel" @click="showSend = false">取消</button>
          <button class="dialog-btn confirm" :disabled="sending" @click="handleSend">
            <text v-if="!sending">发送</text>
            <text v-else>发送中...</text>
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onShow, onPullDownRefresh } from "@dcloudio/uni-app";
import { getMessagePage, markAsRead, sendMessage } from "@/api/message";
import { getEmployeePage } from "@/api/employee";

const list = ref<any[]>([]);
const loading = ref(false);
const pageNum = ref(1);
const finished = ref(false);

// Send dialog
const showSend = ref(false);
const sending = ref(false);
const searchKeyword = ref("");
const searchResults = ref<any[]>([]);
const searching = ref(false);
const selectedEmp = ref<any>(null);
const sendContent = ref("");

const formatTime = (t: string) => t ? t.replace("T", " ").substring(0, 16) : "";

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await getMessagePage({ pageNum: pageNum.value, pageSize: 20 });
    const records = res.data?.list || [];
    if (pageNum.value === 1) {
      list.value = records;
    } else {
      list.value.push(...records);
    }
    if (records.length < 20) finished.value = true;
    else pageNum.value++;
  } catch {
    // silently handle
  } finally {
    loading.value = false;
    uni.stopPullDownRefresh();
  }
};

const handleRead = async (item: any) => {
  if (item.isRead) return;
  try {
    await markAsRead(item.id);
    item.isRead = true;
  } catch {
    // silently handle
  }
};

const openSend = () => {
  selectedEmp.value = null;
  searchKeyword.value = "";
  searchResults.value = [];
  sendContent.value = "";
  showSend.value = true;
};

const searchEmployee = async () => {
  const keyword = searchKeyword.value.trim();
  if (!keyword) {
    searchResults.value = [];
    return;
  }
  searching.value = true;
  try {
    const res: any = await getEmployeePage({ pageNum: 1, pageSize: 20, empName: keyword });
    searchResults.value = res.data?.list || [];
  } catch {
    searchResults.value = [];
  } finally {
    searching.value = false;
  }
};

const selectEmployee = (emp: any) => {
  selectedEmp.value = emp;
  searchKeyword.value = "";
  searchResults.value = [];
};

const handleSend = async () => {
  if (!selectedEmp.value) {
    uni.showToast({ title: "请选择接收人", icon: "none" });
    return;
  }
  if (!sendContent.value.trim()) {
    uni.showToast({ title: "请输入消息内容", icon: "none" });
    return;
  }
  sending.value = true;
  try {
    await sendMessage({ receiverId: selectedEmp.value.id, content: sendContent.value });
    uni.showToast({ title: "发送成功", icon: "success" });
    showSend.value = false;
    // Refresh list
    pageNum.value = 1;
    finished.value = false;
    fetchList();
  } catch (e: any) {
    uni.showToast({ title: e.message || "发送失败", icon: "none" });
  } finally {
    sending.value = false;
  }
};

onShow(fetchList);
onPullDownRefresh(() => {
  pageNum.value = 1;
  finished.value = false;
  fetchList();
});
</script>

<style scoped>
.send-btn {
  margin-bottom: 20rpx;
  background: #409EFF;
  color: #fff;
  border: none;
  border-radius: 8rpx;
  font-size: 28rpx;
  height: 76rpx;
  line-height: 76rpx;
}
.msg-sender {
  font-size: 30rpx;
  font-weight: 600;
  color: #303133;
}
.msg-content {
  display: block;
  font-size: 28rpx;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.empty { display: flex; justify-content: center; padding: 100rpx 0; }
.load-more { display: flex; justify-content: center; padding: 20rpx 0; }

/* Dialog */
.mask {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5); display: flex;
  justify-content: center; align-items: center; z-index: 999;
}
.dialog {
  width: 80%; background: #fff; border-radius: 16rpx; padding: 32rpx;
  max-height: 70vh; overflow-y: auto;
}
.form-item { margin-bottom: 24rpx; }
.form-label { display: block; font-size: 28rpx; color: #606266; margin-bottom: 8rpx; }
.form-input {
  width: 100%; height: 72rpx; border: 1rpx solid #dcdfe6; border-radius: 8rpx;
  padding: 0 20rpx; font-size: 28rpx; box-sizing: border-box;
}
.form-textarea {
  width: 100%; height: 160rpx; border: 1rpx solid #dcdfe6; border-radius: 8rpx;
  padding: 16rpx 20rpx; font-size: 28rpx; box-sizing: border-box;
}

/* Employee list */
.emp-list {
  max-height: 300rpx;
  overflow-y: auto;
  border: 1rpx solid #dcdfe6;
  border-radius: 8rpx;
  margin-bottom: 24rpx;
}
.emp-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx;
  border-bottom: 1rpx solid #f2f3f5;
}
.emp-item:last-child { border-bottom: none; }
.emp-name { font-size: 28rpx; color: #303133; }
.emp-code { font-size: 24rpx; }

/* Selected employee */
.selected-emp {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 72rpx;
  padding: 0 20rpx;
  border: 1rpx solid #409EFF;
  border-radius: 8rpx;
  font-size: 28rpx;
  color: #303133;
}

.dialog-btns { display: flex; gap: 20rpx; margin-top: 32rpx; }
.dialog-btn {
  flex: 1; height: 76rpx; line-height: 76rpx; font-size: 28rpx;
  border-radius: 8rpx; border: none;
}
.dialog-btn.cancel { background: #f4f4f5; color: #909399; }
.dialog-btn.confirm { background: #409EFF; color: #fff; }
.dialog-btn.confirm[disabled] { background: #a0cfff; color: #fff; }
</style>
