<template>
  <view class="container">
    <!-- Upload button -->
    <button class="upload-btn" @click="handleUpload">上传文档</button>

    <!-- Document list -->
    <view class="card" v-for="item in list" :key="item.id">
      <view class="flex-between">
        <text class="doc-name">{{ item.fileName || item.name || '未命名文档' }}</text>
        <text class="text-primary" @click="handleDelete(item.id)">删除</text>
      </view>
      <view class="flex-between mt-20">
        <text class="text-gray">{{ formatSize(item.fileSize || item.size) }}</text>
        <text class="text-gray">{{ formatTime(item.uploadTime || item.createTime) }}</text>
      </view>
    </view>

    <view class="empty" v-if="!loading && list.length === 0">
      <text class="text-gray">暂无文档</text>
    </view>

    <view class="load-more" v-if="loading">
      <text class="text-gray">加载中...</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import { getDocumentPage, uploadDocument, deleteDocument } from "@/api/document";

const list = ref<any[]>([]);
const loading = ref(false);
const pageNum = ref(1);
const finished = ref(false);

const formatSize = (size: number) => {
  if (!size) return "--";
  if (size < 1024) return size + "B";
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + "KB";
  return (size / (1024 * 1024)).toFixed(1) + "MB";
};

const formatTime = (t: string) => t ? t.replace("T", " ").substring(0, 16) : "";

const fetchList = async () => {
  if (loading.value || finished.value) return;
  loading.value = true;
  try {
    const res: any = await getDocumentPage({ pageNum: pageNum.value, pageSize: 20 });
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
  }
};

const handleUpload = () => {
  uni.chooseFile({
    count: 1,
    success: async (res) => {
      const filePath = res.tempFilePaths[0];
      uni.showLoading({ title: "上传中" });
      try {
        await uploadDocument(filePath);
        uni.showToast({ title: "上传成功", icon: "success" });
        pageNum.value = 1;
        finished.value = false;
        fetchList();
      } catch {
        uni.showToast({ title: "上传失败", icon: "none" });
      } finally {
        uni.hideLoading();
      }
    }
  });
};

const handleDelete = async (id: number) => {
  const [, res] = await uni.showModal({ title: "提示", content: "确定删除该文档？" }) as any;
  if (!res?.confirm) return;
  try {
    await deleteDocument(id);
    uni.showToast({ title: "删除成功", icon: "success" });
    list.value = list.value.filter(item => item.id !== id);
  } catch {
    uni.showToast({ title: "删除失败", icon: "none" });
  }
};

onShow(fetchList);
</script>

<style scoped>
.upload-btn {
  margin-bottom: 20rpx;
  background: #67C23A;
  color: #fff;
  border: none;
  border-radius: 8rpx;
  font-size: 28rpx;
  height: 76rpx;
  line-height: 76rpx;
}
.doc-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #303133;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-right: 20rpx;
}
.empty { display: flex; justify-content: center; padding: 100rpx 0; }
.load-more { display: flex; justify-content: center; padding: 20rpx 0; }
</style>
