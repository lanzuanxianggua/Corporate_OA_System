<template>
  <view class="container">
    <!-- Add button -->
    <button class="add-btn" @click="showDialog = true">添加日程</button>

    <!-- Schedule list -->
    <view class="card" v-for="item in list" :key="item.id">
      <view class="flex-between">
        <text class="item-title">{{ item.title }}</text>
        <text class="text-danger" @click="handleDelete(item.id)">删除</text>
      </view>
      <view class="item-time mt-20">
        <text class="text-gray">{{ (item.startTime || '').substring(0, 10) }}</text>
        <text class="text-gray ml-20">{{ formatTime(item.startTime) }} ~ {{ formatTime(item.endTime) }}</text>
      </view>
    </view>

    <view class="empty" v-if="!loading && list.length === 0">
      <text class="text-gray">暂无日程</text>
    </view>

    <view class="load-more" v-if="loading">
      <text class="text-gray">加载中...</text>
    </view>

    <!-- Add dialog -->
    <view class="mask" v-if="showDialog" @click="showDialog = false">
      <view class="dialog" @click.stop>
        <text class="section-title">添加日程</text>

        <view class="form-item">
          <text class="form-label">标题</text>
          <input class="form-input" v-model="form.title" placeholder="请输入日程标题" />
        </view>

        <view class="form-item">
          <text class="form-label">日期</text>
          <picker mode="date" @change="onDateChange">
            <view class="form-value picker-value">
              {{ form.date || '请选择' }}
              <text class="picker-arrow">&#9654;</text>
            </view>
          </picker>
        </view>

        <view class="form-item">
          <text class="form-label">开始时间</text>
          <picker mode="time" @change="onStartTimeChange">
            <view class="form-value picker-value">
              {{ form.startTime || '请选择' }}
              <text class="picker-arrow">&#9654;</text>
            </view>
          </picker>
        </view>

        <view class="form-item">
          <text class="form-label">结束时间</text>
          <picker mode="time" @change="onEndTimeChange">
            <view class="form-value picker-value">
              {{ form.endTime || '请选择' }}
              <text class="picker-arrow">&#9654;</text>
            </view>
          </picker>
        </view>

        <view class="dialog-btns">
          <button class="dialog-btn cancel" @click="showDialog = false">取消</button>
          <button class="dialog-btn confirm" :disabled="submitting" @click="handleSubmit">确定</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onShow, onPullDownRefresh } from "@dcloudio/uni-app";
import { getSchedulePage, addSchedule, deleteSchedule } from "@/api/schedule";

const list = ref<any[]>([]);
const loading = ref(false);
const pageNum = ref(1);
const finished = ref(false);
const showDialog = ref(false);
const submitting = ref(false);

/** Extract HH:mm from "2026-06-24 09:00:00" or return as-is */
const formatTime = (dt: string) => {
  if (!dt) return "";
  const parts = dt.split(" ");
  return parts.length > 1 ? parts[1].substring(0, 5) : dt;
};

const form = ref({
  title: "",
  date: "",
  startTime: "",
  endTime: ""
});

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await getSchedulePage({ pageNum: pageNum.value, pageSize: 20 });
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

const onDateChange = (e: any) => { form.value.date = e.detail.value; };
const onStartTimeChange = (e: any) => { form.value.startTime = e.detail.value; };
const onEndTimeChange = (e: any) => { form.value.endTime = e.detail.value; };

const handleSubmit = async () => {
  const { title, date, startTime, endTime } = form.value;
  if (!title || !date || !startTime || !endTime) {
    uni.showToast({ title: "请填写完整信息", icon: "none" });
    return;
  }
  submitting.value = true;
  try {
    await addSchedule({ title, startTime: date + " " + startTime + ":00", endTime: date + " " + endTime + ":00" });
    uni.showToast({ title: "添加成功", icon: "success" });
    showDialog.value = false;
    form.value = { title: "", date: "", startTime: "", endTime: "" };
    pageNum.value = 1;
    finished.value = false;
    fetchList();
  } catch {
    uni.showToast({ title: "添加失败", icon: "none" });
  } finally {
    submitting.value = false;
  }
};

const handleDelete = async (id: number) => {
  const modalRes = await uni.showModal({ title: "提示", content: "确定删除该日程？" });
  if (!(modalRes as any)?.confirm) return;
  try {
    await deleteSchedule(id);
    uni.showToast({ title: "删除成功", icon: "success" });
    list.value = list.value.filter(item => item.id !== id);
  } catch {
    uni.showToast({ title: "删除失败", icon: "none" });
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
.add-btn {
  margin-bottom: 20rpx;
  background: #67C23A;
  color: #fff;
  border: none;
  border-radius: 8rpx;
  font-size: 28rpx;
  height: 76rpx;
  line-height: 76rpx;
}
.item-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #303133;
}
.item-time { display: flex; align-items: center; }
.ml-20 { margin-left: 20rpx; }
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
}
.form-item { margin-bottom: 24rpx; }
.form-label { display: block; font-size: 28rpx; color: #606266; margin-bottom: 8rpx; }
.form-input {
  width: 100%; height: 72rpx; border: 1rpx solid #dcdfe6; border-radius: 8rpx;
  padding: 0 20rpx; font-size: 28rpx; box-sizing: border-box;
}
.picker-value {
  display: flex; justify-content: space-between; align-items: center;
  height: 72rpx; border: 1rpx solid #dcdfe6; border-radius: 8rpx;
  padding: 0 20rpx; font-size: 28rpx; color: #303133;
}
.picker-arrow { font-size: 22rpx; color: #c0c4cc; transform: rotate(90deg); }
.dialog-btns { display: flex; gap: 20rpx; margin-top: 32rpx; }
.dialog-btn {
  flex: 1; height: 76rpx; line-height: 76rpx; font-size: 28rpx;
  border-radius: 8rpx; border: none;
}
.dialog-btn.cancel { background: #f4f4f5; color: #909399; }
.dialog-btn.confirm { background: #409EFF; color: #fff; }
.dialog-btn.confirm[disabled] { background: #a0cfff; color: #fff; }
</style>
