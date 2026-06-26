<template>
  <view class="container">
    <view class="profile-card card">
      <view class="avatar-large">{{ userStore.displayName.charAt(0) }}</view>
      <text class="profile-name">{{ userStore.displayName }}</text>
      <text class="text-gray">{{ userStore.userInfo.empCode || '' }}</text>
    </view>

    <view class="card">
      <view class="menu-item" @click="showMyApps = true">
        <text>我的申请</text><text class="text-gray">&gt;</text>
      </view>
      <view class="menu-item" @click="navigate('/pages/oa/leave-list')">
        <text>请假记录</text><text class="text-gray">&gt;</text>
      </view>
      <view class="menu-item" @click="showLeaveBalance = true">
        <text>假期余额</text><text class="text-gray">&gt;</text>
      </view>
      <view class="menu-item" @click="navigate('/pages/oa/schedule')">
        <text>我的日程</text><text class="text-gray">&gt;</text>
      </view>
      <view class="menu-item" @click="navigate('/pages/oa/document')">
        <text>文档中心</text><text class="text-gray">&gt;</text>
      </view>
      <view class="menu-item" @click="navigate('/pages/oa/notice-list')">
        <text>公告通知</text><text class="text-gray">&gt;</text>
      </view>
    </view>

    <view class="card">
      <view class="menu-item" @click="showChangePwd = true">
        <text>修改密码</text><text class="text-gray">&gt;</text>
      </view>
      <view class="menu-item" @click="handleLogout">
        <text class="text-danger">退出登录</text><text class="text-gray">&gt;</text>
      </view>
    </view>

    <!-- 我的申请 dialog -->
    <view class="mask" v-if="showMyApps" @click="showMyApps = false">
      <view class="dialog large" @click.stop>
        <text class="section-title">我的申请</text>
        <!-- Type selection tabs -->
        <view class="type-tabs">
          <view
            v-for="t in bizTypes"
            :key="t.key"
            :class="['type-tab', activeBizType === t.key ? 'type-tab-active' : '']"
            @click="switchBizType(t.key)"
          >{{ t.label }}</view>
        </view>
        <!-- List -->
        <view v-if="bizList.length > 0">
          <view v-for="item in bizList" :key="item.id" class="app-item">
            <view class="flex-between">
              <text class="app-dates">{{ formatDate(item.createTime) }}</text>
              <text :class="statusClass(item.status)">{{ statusText(item.status) }}</text>
            </view>
            <text class="app-summary text-gray mt-10">{{ getSummary(item) }}</text>
          </view>
        </view>
        <view v-else class="empty-sm"><text class="text-gray">暂无申请记录</text></view>
      </view>
    </view>

    <!-- 假期余额 dialog -->
    <view class="mask" v-if="showLeaveBalance" @click="showLeaveBalance = false">
      <view class="dialog" @click.stop>
        <text class="section-title">假期余额</text>
        <view v-if="balances.length > 0">
          <view v-for="b in balances" :key="b.leaveType" class="balance-item">
            <text class="balance-type">{{ leaveTypeLabel(b.leaveType) }}</text>
            <view class="balance-bar">
              <view class="balance-fill" :style="{ width: balancePct(b) + '%' }"></view>
            </view>
            <text class="balance-num">{{ b.remainingDays }}/{{ b.totalDays }}天</text>
          </view>
        </view>
        <view v-else class="empty-sm" v-if="!balanceLoading">
          <text class="text-gray">暂无假期余额数据</text>
        </view>
        <view v-if="balanceLoading" class="empty-sm">
          <text class="text-gray">加载中...</text>
        </view>
      </view>
    </view>

    <!-- 修改密码 dialog -->
    <view class="mask" v-if="showChangePwd" @click="showChangePwd = false">
      <view class="dialog" @click.stop>
        <text class="section-title">修改密码</text>
        <view class="form-item">
          <text class="form-label">旧密码</text>
          <input class="form-input" type="safe-password" v-model="pwdForm.oldPassword" placeholder="请输入旧密码" />
        </view>
        <view class="form-item">
          <text class="form-label">新密码</text>
          <input class="form-input" type="safe-password" v-model="pwdForm.newPassword" placeholder="请输入新密码" />
        </view>
        <view class="form-item">
          <text class="form-label">确认新密码</text>
          <input class="form-input" type="safe-password" v-model="pwdForm.confirmPassword" placeholder="请再次输入新密码" />
        </view>
        <view class="dialog-btns">
          <button class="dialog-btn cancel" @click="showChangePwd = false">取消</button>
          <button class="dialog-btn confirm" :disabled="pwdSending" @click="handleChangePwd">
            <text v-if="!pwdSending">确认修改</text>
            <text v-else>提交中...</text>
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useUserStore } from "@/store/user";
import { changePassword } from "@/api/auth";
import { getMyBalances } from "@/api/leave";
import { STATUS_MAP, STATUS_CLASS_MAP, LEAVE_TYPE_MAP } from "@/utils/constants";
import {
  getLeavePage, getBusinessTripPage, getOutingPage,
  getOvertimePage, getPurchasePage, getExpensePage, getLoanPage
} from "@/api/common";

const userStore = useUserStore();

// === Navigation ===
const navigate = (url: string) => { uni.navigateTo({ url }); };

const handleLogout = async () => {
  const modalRes = await uni.showModal({ title: "提示", content: "确定要退出登录吗？" });
  if ((modalRes as any)?.confirm) {
    await userStore.logout();
  }
};

// === 我的申请 ===
const showMyApps = ref(false);
const activeBizType = ref("leave");
const bizList = ref<any[]>([]);
const bizLoading = ref(false);

const bizTypes = [
  { key: "leave", label: "请假" },
  { key: "trip", label: "出差" },
  { key: "outing", label: "外出" },
  { key: "overtime", label: "加班" },
  { key: "expense", label: "经费" },
  { key: "purchase", label: "采购" },
  { key: "loan", label: "借款" },
];

const fetchBizList = async (type: string) => {
  bizLoading.value = true;
  try {
    const pageApi: Record<string, any> = {
      leave: getLeavePage, trip: getBusinessTripPage,
      outing: getOutingPage, overtime: getOvertimePage,
      expense: getExpensePage, purchase: getPurchasePage,
      loan: getLoanPage,
    };
    const apiFn = pageApi[type];
    if (!apiFn) { bizList.value = []; return; }
    const res: any = await apiFn({ pageNum: 1, pageSize: 50 });
    bizList.value = res.data?.list || [];
  } catch {
    bizList.value = [];
  } finally {
    bizLoading.value = false;
  }
};

const switchBizType = (key: string) => {
  activeBizType.value = key;
  fetchBizList(key);
};

const statusText = (s: string | number) => STATUS_MAP[Number(s)] ?? "未知";
const statusClass = (s: string | number) => STATUS_CLASS_MAP[Number(s)] ?? "text-gray";
const formatDate = (t: string) => t ? t.replace("T", " ").substring(0, 10) : "";

const getSummary = (item: any) => {
  const type = activeBizType.value;
  if (type === "leave") return `${item.leaveType ? (LEAVE_TYPE_MAP[Number(item.leaveType)] || "请假") : "请假"} ${item.days || ""}天`;
  if (type === "trip") return item.destination || "出差";
  if (type === "outing") return item.destination || "外出";
  if (type === "overtime") return `${item.overtimeDate || ""} ${item.hours || ""}小时`;
  if (type === "expense") return `${item.title || "经费"} ${item.amount || ""}元`;
  if (type === "purchase") return `${item.itemName || "采购"} x${item.quantity || ""}`;
  if (type === "loan") return `借款 ${item.loanAmount || ""}元`;
  return "";
};

// === 假期余额 ===
const showLeaveBalance = ref(false);
const balances = ref<any[]>([]);
const balanceLoading = ref(false);

const fetchBalance = async () => {
  balanceLoading.value = true;
  try {
    const res: any = await getMyBalances();
    balances.value = Array.isArray(res.data) ? res.data : (res.data?.list || []);
  } catch {
    balances.value = [];
  } finally {
    balanceLoading.value = false;
  }
};

const leaveTypeLabel = (type: string | number) => LEAVE_TYPE_MAP[Number(type)] || "假期";
const balancePct = (b: any) => {
  if (!b.totalDays || Number(b.totalDays) <= 0) return 0;
  return Math.round((Number(b.remainingDays) / Number(b.totalDays)) * 100);
};

// === 修改密码 ===
const showChangePwd = ref(false);
const pwdSending = ref(false);
const pwdForm = ref({ oldPassword: "", newPassword: "", confirmPassword: "" });

const handleChangePwd = async () => {
  const { oldPassword, newPassword, confirmPassword } = pwdForm.value;
  if (!oldPassword || !newPassword || !confirmPassword) {
    uni.showToast({ title: "请填写完整", icon: "none" }); return;
  }
  if (newPassword.length < 6) {
    uni.showToast({ title: "新密码至少6个字符", icon: "none" }); return;
  }
  if (newPassword !== confirmPassword) {
    uni.showToast({ title: "两次密码输入不一致", icon: "none" }); return;
  }
  pwdSending.value = true;
  try {
    await changePassword({ oldPassword, newPassword, confirmPassword });
    uni.showToast({ title: "密码修改成功", icon: "success" });
    showChangePwd.value = false;
    pwdForm.value = { oldPassword: "", newPassword: "", confirmPassword: "" };
  } catch (e: any) {
    uni.showToast({ title: e.message || "修改失败", icon: "none" });
  } finally {
    pwdSending.value = false;
  }
};
</script>

<style scoped>
.profile-card {
  display: flex; flex-direction: column; align-items: center;
  padding: 48rpx 24rpx;
}
.avatar-large {
  width: 120rpx; height: 120rpx; border-radius: 50%;
  background: #409EFF; color: #fff; display: flex;
  align-items: center; justify-content: center;
  font-size: 48rpx; font-weight: bold; margin-bottom: 20rpx;
}
.profile-name { font-size: 36rpx; font-weight: 600; margin-bottom: 8rpx; }
.menu-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 28rpx 0; border-bottom: 1rpx solid #f2f3f5;
}
.menu-item:last-child { border-bottom: none; }

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
.dialog.large { width: 88%; }

/* 我的申请 */
.type-tabs { display: flex; flex-wrap: wrap; gap: 12rpx; margin: 16rpx 0; }
.type-tab {
  padding: 8rpx 20rpx; border-radius: 30rpx; font-size: 24rpx;
  background: #f4f4f5; color: #909399;
}
.type-tab-active { background: #ecf5ff; color: #409EFF; font-weight: 600; }
.app-item { padding: 16rpx 0; border-bottom: 1rpx solid #f2f3f5; }
.app-item:last-child { border-bottom: none; }
.app-dates { font-size: 26rpx; color: #303133; }
.app-summary { display: block; font-size: 24rpx; }

/* 假期余额 */
.balance-item { display: flex; align-items: center; gap: 16rpx; padding: 16rpx 0; border-bottom: 1rpx solid #f2f3f5; }
.balance-item:last-child { border-bottom: none; }
.balance-type { font-size: 26rpx; color: #303133; width: 100rpx; }
.balance-bar { flex: 1; height: 16rpx; background: #f2f3f5; border-radius: 8rpx; overflow: hidden; }
.balance-fill { height: 100%; background: linear-gradient(90deg, #409EFF, #66b1ff); border-radius: 8rpx; }
.balance-num { font-size: 24rpx; color: #606266; width: 100rpx; text-align: right; }

/* 修改密码 */
.form-item { margin-bottom: 24rpx; }
.form-label { display: block; font-size: 28rpx; color: #606266; margin-bottom: 8rpx; }
.form-input {
  width: 100%; height: 72rpx; border: 1rpx solid #dcdfe6; border-radius: 8rpx;
  padding: 0 20rpx; font-size: 28rpx; box-sizing: border-box;
}
.dialog-btns { display: flex; gap: 20rpx; margin-top: 32rpx; }
.dialog-btn {
  flex: 1; height: 76rpx; line-height: 76rpx; font-size: 28rpx;
  border-radius: 8rpx; border: none;
}
.dialog-btn.cancel { background: #f4f4f5; color: #909399; }
.dialog-btn.confirm { background: #409EFF; color: #fff; }
.dialog-btn.confirm[disabled] { background: #a0cfff; color: #fff; }
.empty-sm { text-align: center; padding: 40rpx 0; }
.text-gray { color: #909399; font-size: 24rpx; }
.mt-10 { margin-top: 10rpx; }
</style>
