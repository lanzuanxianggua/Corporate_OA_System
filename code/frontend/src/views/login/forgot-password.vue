<template>
  <div class="min-h-screen grid lg:grid-cols-2">
    <!-- Left Content Section - 灰白色背景 -->
    <div class="relative hidden lg:flex flex-col justify-between bg-gradient-to-br from-gray-100 via-gray-200 to-gray-300 p-12 text-gray-800">
      <div class="relative z-20">
        <div class="text-2xl font-semibold">
          OA办公系统
        </div>
      </div>

      <div class="relative z-20 flex items-end justify-center h-[500px]">
        <!-- Cartoon Characters -->
        <div class="relative" style="width: 550px; height: 400px">
          <!-- Purple character -->
          <div
            ref="purpleRef"
            class="absolute bottom-0 transition-all duration-700 ease-in-out"
            :style="{
              left: '70px',
              width: '180px',
              height: isTyping ? '440px' : '400px',
              backgroundColor: '#6C3FF5',
              borderRadius: '10px 10px 0 0',
              zIndex: 1,
              transform: isTyping ? `skewX(${(purplePos.bodySkew || 0) - 12}deg) translateX(40px)` : `skewX(${purplePos.bodySkew || 0}deg)`,
              transformOrigin: 'bottom center',
            }"
          >
            <div
              class="absolute flex gap-8 transition-all duration-700 ease-in-out"
              :style="{
                left: isLookingAtEachOther ? '55px' : `${45 + purplePos.faceX}px`,
                top: isLookingAtEachOther ? '65px' : `${40 + purplePos.faceY}px`,
              }"
            >
              <EyeBall
                :size="18"
                :pupil-size="7"
                :max-distance="5"
                eye-color="white"
                pupil-color="#2D2D2D"
                :is-blinking="isPurpleBlinking"
                :force-look-x="isLookingAtEachOther ? 3 : undefined"
                :force-look-y="isLookingAtEachOther ? 4 : undefined"
              />
              <EyeBall
                :size="18"
                :pupil-size="7"
                :max-distance="5"
                eye-color="white"
                pupil-color="#2D2D2D"
                :is-blinking="isPurpleBlinking"
                :force-look-x="isLookingAtEachOther ? 3 : undefined"
                :force-look-y="isLookingAtEachOther ? 4 : undefined"
              />
            </div>
          </div>

          <!-- Black character -->
          <div
            ref="blackRef"
            class="absolute bottom-0 transition-all duration-700 ease-in-out"
            :style="{
              left: '240px',
              width: '120px',
              height: '310px',
              backgroundColor: '#2D2D2D',
              borderRadius: '8px 8px 0 0',
              zIndex: 2,
              transform: isLookingAtEachOther
                ? `skewX(${(blackPos.bodySkew || 0) * 1.5 + 10}deg) translateX(20px)`
                : isTyping
                  ? `skewX(${(blackPos.bodySkew || 0) * 1.5}deg)`
                  : `skewX(${blackPos.bodySkew || 0}deg)`,
              transformOrigin: 'bottom center',
            }"
          >
            <div
              class="absolute flex gap-6 transition-all duration-700 ease-in-out"
              :style="{
                left: isLookingAtEachOther ? '32px' : `${26 + blackPos.faceX}px`,
                top: isLookingAtEachOther ? '12px' : `${32 + blackPos.faceY}px`,
              }"
            >
              <EyeBall
                :size="16"
                :pupil-size="6"
                :max-distance="4"
                eye-color="white"
                pupil-color="#2D2D2D"
                :is-blinking="isBlackBlinking"
                :force-look-x="isLookingAtEachOther ? 0 : undefined"
                :force-look-y="isLookingAtEachOther ? -4 : undefined"
              />
              <EyeBall
                :size="16"
                :pupil-size="6"
                :max-distance="4"
                eye-color="white"
                pupil-color="#2D2D2D"
                :is-blinking="isBlackBlinking"
                :force-look-x="isLookingAtEachOther ? 0 : undefined"
                :force-look-y="isLookingAtEachOther ? -4 : undefined"
              />
            </div>
          </div>

          <!-- Orange character -->
          <div
            ref="orangeRef"
            class="absolute bottom-0 transition-all duration-700 ease-in-out"
            :style="{
              left: '0px',
              width: '240px',
              height: '200px',
              zIndex: 3,
              backgroundColor: '#FF9B6B',
              borderRadius: '120px 120px 0 0',
              transform: `skewX(${orangePos.bodySkew || 0}deg)`,
              transformOrigin: 'bottom center',
            }"
          >
            <div
              class="absolute flex gap-8 transition-all duration-200 ease-out"
              :style="{
                left: `${82 + (orangePos.faceX || 0)}px`,
                top: `${90 + (orangePos.faceY || 0)}px`,
              }"
            >
              <Pupil :size="12" :max-distance="5" pupil-color="#2D2D2D" />
              <Pupil :size="12" :max-distance="5" pupil-color="#2D2D2D" />
            </div>
          </div>

          <!-- Yellow character -->
          <div
            ref="yellowRef"
            class="absolute bottom-0 transition-all duration-700 ease-in-out"
            :style="{
              left: '310px',
              width: '140px',
              height: '230px',
              backgroundColor: '#E8D754',
              borderRadius: '70px 70px 0 0',
              zIndex: 4,
              transform: `skewX(${yellowPos.bodySkew || 0}deg)`,
              transformOrigin: 'bottom center',
            }"
          >
            <div
              class="absolute flex gap-6 transition-all duration-200 ease-out"
              :style="{
                left: `${52 + (yellowPos.faceX || 0)}px`,
                top: `${40 + (yellowPos.faceY || 0)}px`,
              }"
            >
              <Pupil :size="12" :max-distance="5" pupil-color="#2D2D2D" />
              <Pupil :size="12" :max-distance="5" pupil-color="#2D2D2D" />
            </div>
            <div
              class="absolute w-20 h-[4px] bg-[#2D2D2D] rounded-full transition-all duration-200 ease-out"
              :style="{
                left: `${40 + (yellowPos.faceX || 0)}px`,
                top: `${88 + (yellowPos.faceY || 0)}px`,
              }"
            />
          </div>
        </div>
      </div>

      <div class="relative z-20 text-sm text-gray-600">
        <p>企业协同管理平台</p>
      </div>

      <!-- Decorative elements -->
      <div class="absolute inset-0 bg-grid-pattern opacity-20" />
      <div class="absolute top-1/4 right-1/4 size-64 bg-gray-400/20 rounded-full blur-3xl" />
      <div class="absolute bottom-1/4 left-1/4 size-96 bg-gray-500/15 rounded-full blur-3xl" />
    </div>

    <!-- Right Forgot Password Section - 黑色背景 -->
    <div class="flex items-center justify-center p-8 bg-gray-900">
      <div class="w-full max-w-[420px]">
        <!-- Mobile Logo -->
        <div class="lg:hidden text-center text-2xl font-semibold mb-12 text-white">
          OA办公系统
        </div>

        <!-- Header -->
        <div class="text-center mb-10">
          <h1 class="text-3xl font-bold tracking-tight mb-2 text-white">重置密码</h1>
          <p class="text-gray-400 text-sm">输入您的邮箱，我们将发送重置链接</p>
        </div>

        <!-- Forgot Password Form -->
        <form @submit.prevent="handleForgotPassword" class="space-y-5">
          <!-- Email -->
          <div class="space-y-2">
            <label for="email" class="text-sm font-medium block text-gray-300">邮箱</label>
            <input
              id="email"
              v-model="forgotForm.email"
              type="email"
              placeholder="your@email.com"
              autocomplete="off"
              class="login-input"
              @focus="isTyping = true"
              @blur="isTyping = false"
            />
          </div>

          <!-- Submit Button -->
          <button
            type="submit"
            class="login-button"
            :disabled="loading"
          >
            <span v-if="loading" class="inline-flex items-center">
              <svg class="animate-spin -ml-1 mr-2 h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              发送中...
            </span>
            <span v-else>发送重置链接</span>
          </button>
        </form>

        <!-- Back to Login -->
        <div class="text-center text-sm text-gray-400 mt-8">
          记起密码了？
          <a href="/login" class="text-gray-200 font-medium hover:text-white hover:underline">返回登录</a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import EyeBall from "@/components/EyeBall.vue";
import Pupil from "@/components/Pupil.vue";

const router = useRouter();
const loading = ref(false);
const isTyping = ref(false);
const isLookingAtEachOther = ref(false);
const isPurpleBlinking = ref(false);
const isBlackBlinking = ref(false);

const mouseX = ref(0);
const mouseY = ref(0);

const purpleRef = ref<HTMLDivElement>();
const blackRef = ref<HTMLDivElement>();
const yellowRef = ref<HTMLDivElement>();
const orangeRef = ref<HTMLDivElement>();

const forgotForm = reactive({
  email: ""
});

// Mouse tracking
const handleMouseMove = (e: MouseEvent) => {
  mouseX.value = e.clientX;
  mouseY.value = e.clientY;
};

// Calculate character position
const calculatePosition = (refEl: any) => {
  if (!refEl?.value) return { faceX: 0, faceY: 0, bodySkew: 0 };

  const rect = refEl.value.getBoundingClientRect();
  const centerX = rect.left + rect.width / 2;
  const centerY = rect.top + rect.height / 3;

  const deltaX = mouseX.value - centerX;
  const deltaY = mouseY.value - centerY;

  const faceX = Math.max(-15, Math.min(15, deltaX / 20));
  const faceY = Math.max(-10, Math.min(10, deltaY / 30));
  const bodySkew = Math.max(-6, Math.min(6, -deltaX / 120));

  return { faceX, faceY, bodySkew };
};

const purplePos = computed(() => calculatePosition(purpleRef));
const blackPos = computed(() => calculatePosition(blackRef));
const yellowPos = computed(() => calculatePosition(yellowRef));
const orangePos = computed(() => calculatePosition(orangeRef));

// Purple blinking
let purpleBlinkTimeout: ReturnType<typeof setTimeout>;
const schedulePurpleBlink = () => {
  const interval = Math.random() * 4000 + 3000;
  purpleBlinkTimeout = setTimeout(() => {
    isPurpleBlinking.value = true;
    setTimeout(() => {
      isPurpleBlinking.value = false;
      schedulePurpleBlink();
    }, 150);
  }, interval);
};

// Black blinking
let blackBlinkTimeout: ReturnType<typeof setTimeout>;
const scheduleBlackBlink = () => {
  const interval = Math.random() * 4000 + 3000;
  blackBlinkTimeout = setTimeout(() => {
    isBlackBlinking.value = true;
    setTimeout(() => {
      isBlackBlinking.value = false;
      scheduleBlackBlink();
    }, 150);
  }, interval);
};

// Watch typing state
let lookingTimeout: ReturnType<typeof setTimeout>;
watch(isTyping, (newVal) => {
  if (newVal) {
    isLookingAtEachOther.value = true;
    clearTimeout(lookingTimeout);
    lookingTimeout = setTimeout(() => {
      isLookingAtEachOther.value = false;
    }, 800);
  } else {
    isLookingAtEachOther.value = false;
  }
});

onMounted(() => {
  window.addEventListener("mousemove", handleMouseMove);
  schedulePurpleBlink();
  scheduleBlackBlink();
});

onUnmounted(() => {
  window.removeEventListener("mousemove", handleMouseMove);
  clearTimeout(purpleBlinkTimeout);
  clearTimeout(blackBlinkTimeout);
  clearTimeout(lookingTimeout);
});

const handleForgotPassword = async () => {
  if (!forgotForm.email) {
    ElMessage.warning("请输入邮箱地址");
    return;
  }

  // 简单的邮箱格式验证
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(forgotForm.email)) {
    ElMessage.error("请输入有效的邮箱地址");
    return;
  }

  loading.value = true;
  try {
    // TODO: 调用忘记密码 API
    // await forgotPasswordAPI(forgotForm.email);

    // 模拟 API 调用
    await new Promise(resolve => setTimeout(resolve, 1000));

    ElMessage.success("重置链接已发送到您的邮箱，请查收");

    // 3秒后跳转到登录页
    setTimeout(() => {
      router.push("/login");
    }, 3000);
  } catch (error: any) {
    ElMessage.error(error.message || "发送失败，请稍后重试");
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
/* Grid pattern */
.bg-grid-pattern {
  background-image:
    linear-gradient(to right, rgba(0, 0, 0, 0.05) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(0, 0, 0, 0.05) 1px, transparent 1px);
  background-size: 20px 20px;
}

/* Login Input - 黑色主题 */
.login-input {
  width: 100%;
  height: 48px;
  padding: 0 16px;
  font-size: 15px;
  color: #ffffff;
  background-color: #1f2937;
  border: 1.5px solid #374151;
  border-radius: 8px;
  outline: none;
  transition: all 0.2s;
}

.login-input:hover {
  border-color: #4b5563;
}

.login-input:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.login-input::placeholder {
  color: #6b7280;
}

/* Login Button */
.login-button {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  color: white;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-button:hover:not(:disabled) {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
}

.login-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Spacing utilities */
.space-y-2 > * + * {
  margin-top: 0.5rem;
}

.space-y-5 > * + * {
  margin-top: 1.25rem;
}

/* Mobile responsive */
@media (max-width: 1024px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
