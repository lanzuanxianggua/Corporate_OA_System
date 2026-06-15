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
              height: (isTyping || (loginForm.password.length > 0 && !showPassword)) ? '440px' : '400px',
              backgroundColor: '#6C3FF5',
              borderRadius: '10px 10px 0 0',
              zIndex: 1,
              transform: (loginForm.password.length > 0 && showPassword)
                ? 'skewX(0deg)'
                : (isTyping || (loginForm.password.length > 0 && !showPassword))
                  ? `skewX(${(purplePos.bodySkew || 0) - 12}deg) translateX(40px)`
                  : `skewX(${purplePos.bodySkew || 0}deg)`,
              transformOrigin: 'bottom center',
            }"
          >
            <div
              class="absolute flex gap-8 transition-all duration-700 ease-in-out"
              :style="{
                left: (loginForm.password.length > 0 && showPassword) ? '20px' : isLookingAtEachOther ? '55px' : `${45 + purplePos.faceX}px`,
                top: (loginForm.password.length > 0 && showPassword) ? '35px' : isLookingAtEachOther ? '65px' : `${40 + purplePos.faceY}px`,
              }"
            >
              <EyeBall
                :size="18"
                :pupil-size="7"
                :max-distance="5"
                eye-color="white"
                pupil-color="#2D2D2D"
                :is-blinking="isPurpleBlinking"
                :force-look-x="(loginForm.password.length > 0 && showPassword) ? (isPurplePeeking ? 4 : -4) : isLookingAtEachOther ? 3 : undefined"
                :force-look-y="(loginForm.password.length > 0 && showPassword) ? (isPurplePeeking ? 5 : -4) : isLookingAtEachOther ? 4 : undefined"
              />
              <EyeBall
                :size="18"
                :pupil-size="7"
                :max-distance="5"
                eye-color="white"
                pupil-color="#2D2D2D"
                :is-blinking="isPurpleBlinking"
                :force-look-x="(loginForm.password.length > 0 && showPassword) ? (isPurplePeeking ? 4 : -4) : isLookingAtEachOther ? 3 : undefined"
                :force-look-y="(loginForm.password.length > 0 && showPassword) ? (isPurplePeeking ? 5 : -4) : isLookingAtEachOther ? 4 : undefined"
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
              transform: (loginForm.password.length > 0 && showPassword)
                ? 'skewX(0deg)'
                : isLookingAtEachOther
                  ? `skewX(${(blackPos.bodySkew || 0) * 1.5 + 10}deg) translateX(20px)`
                  : (isTyping || (loginForm.password.length > 0 && !showPassword))
                    ? `skewX(${(blackPos.bodySkew || 0) * 1.5}deg)`
                    : `skewX(${blackPos.bodySkew || 0}deg)`,
              transformOrigin: 'bottom center',
            }"
          >
            <div
              class="absolute flex gap-6 transition-all duration-700 ease-in-out"
              :style="{
                left: (loginForm.password.length > 0 && showPassword) ? '10px' : isLookingAtEachOther ? '32px' : `${26 + blackPos.faceX}px`,
                top: (loginForm.password.length > 0 && showPassword) ? '28px' : isLookingAtEachOther ? '12px' : `${32 + blackPos.faceY}px`,
              }"
            >
              <EyeBall
                :size="16"
                :pupil-size="6"
                :max-distance="4"
                eye-color="white"
                pupil-color="#2D2D2D"
                :is-blinking="isBlackBlinking"
                :force-look-x="(loginForm.password.length > 0 && showPassword) ? -4 : isLookingAtEachOther ? 0 : undefined"
                :force-look-y="(loginForm.password.length > 0 && showPassword) ? -4 : isLookingAtEachOther ? -4 : undefined"
              />
              <EyeBall
                :size="16"
                :pupil-size="6"
                :max-distance="4"
                eye-color="white"
                pupil-color="#2D2D2D"
                :is-blinking="isBlackBlinking"
                :force-look-x="(loginForm.password.length > 0 && showPassword) ? -4 : isLookingAtEachOther ? 0 : undefined"
                :force-look-y="(loginForm.password.length > 0 && showPassword) ? -4 : isLookingAtEachOther ? -4 : undefined"
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
              transform: (loginForm.password.length > 0 && showPassword) ? 'skewX(0deg)' : `skewX(${orangePos.bodySkew || 0}deg)`,
              transformOrigin: 'bottom center',
            }"
          >
            <div
              class="absolute flex gap-8 transition-all duration-200 ease-out"
              :style="{
                left: (loginForm.password.length > 0 && showPassword) ? '50px' : `${82 + (orangePos.faceX || 0)}px`,
                top: (loginForm.password.length > 0 && showPassword) ? '85px' : `${90 + (orangePos.faceY || 0)}px`,
              }"
            >
              <Pupil :size="12" :max-distance="5" pupil-color="#2D2D2D" :force-look-x="(loginForm.password.length > 0 && showPassword) ? -5 : undefined" :force-look-y="(loginForm.password.length > 0 && showPassword) ? -4 : undefined" />
              <Pupil :size="12" :max-distance="5" pupil-color="#2D2D2D" :force-look-x="(loginForm.password.length > 0 && showPassword) ? -5 : undefined" :force-look-y="(loginForm.password.length > 0 && showPassword) ? -4 : undefined" />
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
              transform: (loginForm.password.length > 0 && showPassword) ? 'skewX(0deg)' : `skewX(${yellowPos.bodySkew || 0}deg)`,
              transformOrigin: 'bottom center',
            }"
          >
            <div
              class="absolute flex gap-6 transition-all duration-200 ease-out"
              :style="{
                left: (loginForm.password.length > 0 && showPassword) ? '20px' : `${52 + (yellowPos.faceX || 0)}px`,
                top: (loginForm.password.length > 0 && showPassword) ? '35px' : `${40 + (yellowPos.faceY || 0)}px`,
              }"
            >
              <Pupil :size="12" :max-distance="5" pupil-color="#2D2D2D" :force-look-x="(loginForm.password.length > 0 && showPassword) ? -5 : undefined" :force-look-y="(loginForm.password.length > 0 && showPassword) ? -4 : undefined" />
              <Pupil :size="12" :max-distance="5" pupil-color="#2D2D2D" :force-look-x="(loginForm.password.length > 0 && showPassword) ? -5 : undefined" :force-look-y="(loginForm.password.length > 0 && showPassword) ? -4 : undefined" />
            </div>
            <div
              class="absolute w-20 h-[4px] bg-[#2D2D2D] rounded-full transition-all duration-200 ease-out"
              :style="{
                left: (loginForm.password.length > 0 && showPassword) ? '10px' : `${40 + (yellowPos.faceX || 0)}px`,
                top: (loginForm.password.length > 0 && showPassword) ? '88px' : `${88 + (yellowPos.faceY || 0)}px`,
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

    <!-- Right Login Section - 黑色背景 -->
    <div class="flex items-center justify-center p-8 bg-gray-900">
      <div class="w-full max-w-[420px]">
        <!-- Mobile Logo -->
        <div class="lg:hidden text-center text-2xl font-semibold mb-12 text-white">
          OA办公系统
        </div>

        <!-- Header -->
        <div class="text-center mb-10">
          <h1 class="text-3xl font-bold tracking-tight mb-2 text-white">欢迎回来！</h1>
          <p class="text-gray-400 text-sm">请输入您的登录信息</p>
        </div>

        <!-- Login Form -->
        <form @submit.prevent="handleLogin" class="space-y-5">
          <!-- Username -->
          <div class="space-y-2">
            <label for="username" class="text-sm font-medium block text-gray-300">用户名</label>
            <input
              id="username"
              v-model="loginForm.username"
              type="text"
              placeholder="请输入用户名"
              autocomplete="off"
              class="login-input"
              @focus="isTyping = true"
              @blur="isTyping = false"
            />
          </div>

          <!-- Password -->
          <div class="space-y-2">
            <label for="password" class="text-sm font-medium block text-gray-300">密码</label>
            <div class="relative">
              <input
                id="password"
                v-model="loginForm.password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="请输入密码"
                class="login-input pr-12"
              />
              <button
                type="button"
                @click="showPassword = !showPassword"
                class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-300 transition-colors z-10"
                tabindex="-1"
              >
                <svg v-if="showPassword" xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                  <path d="M10 12a2 2 0 100-4 2 2 0 000 4z" />
                  <path fill-rule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clip-rule="evenodd" />
                </svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M3.707 2.293a1 1 0 00-1.414 1.414l14 14a1 1 0 001.414-1.414l-1.473-1.473A10.014 10.014 0 0019.542 10C18.268 5.943 14.478 3 10 3a9.958 9.958 0 00-4.512 1.074l-1.78-1.781zm4.261 4.26l1.514 1.515a2.003 2.003 0 012.45 2.45l1.514 1.514a4 4 0 00-5.478-5.478z" clip-rule="evenodd" />
                  <path d="M12.454 16.697L9.75 13.992a4 4 0 01-3.742-3.741L2.335 6.578A9.98 9.98 0 00.458 10c1.274 4.057 5.065 7 9.542 7 .847 0 1.669-.105 2.454-.303z" />
                </svg>
              </button>
            </div>
          </div>

          <!-- Captcha -->
          <div class="space-y-2">
            <label for="captcha" class="text-sm font-medium block text-gray-300">验证码</label>
            <div class="flex gap-3">
              <input
                id="captcha"
                v-model="loginForm.captchaCode"
                type="text"
                placeholder="请输入验证码"
                autocomplete="off"
                class="login-input flex-1"
              />
              <div class="captcha-image" @click="refreshCaptcha" title="点击刷新">
                <img v-if="captchaImg" :src="captchaImg" alt="验证码" class="h-full w-full object-cover" />
                <span v-else class="text-xs text-gray-500">点击刷新</span>
              </div>
            </div>
          </div>

          <!-- Submit Button -->
          <button
            type="submit"
            class="login-button mt-6"
            :disabled="loading"
          >
            <span v-if="loading" class="inline-flex items-center">
              <svg class="animate-spin -ml-1 mr-2 h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              登录中...
            </span>
            <span v-else>登 录</span>
          </button>
        </form>

        <!-- Sign Up Link -->
        <div class="text-center text-sm text-gray-400 mt-8">
          还没有账号？
          <router-link to="/register" class="text-gray-200 font-medium hover:text-white hover:underline">立即注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { useUserStore } from "@/store/user";
import { getCaptcha } from "@/api/auth";
import EyeBall from "@/components/EyeBall.vue";
import Pupil from "@/components/Pupil.vue";

const router = useRouter();
const userStore = useUserStore();
const loading = ref(false);
const captchaImg = ref("");
const captchaUuid = ref("");
const showPassword = ref(false);
const isTyping = ref(false);
const isLookingAtEachOther = ref(false);
const isPurpleBlinking = ref(false);
const isBlackBlinking = ref(false);
const isPurplePeeking = ref(false);

const mouseX = ref(0);
const mouseY = ref(0);

const purpleRef = ref<HTMLDivElement>();
const blackRef = ref<HTMLDivElement>();
const yellowRef = ref<HTMLDivElement>();
const orangeRef = ref<HTMLDivElement>();

const loginForm = reactive({
  username: "",
  password: "",
  captchaCode: "",
  remember: false
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

// Purple peeking
let purplePeekTimeout: ReturnType<typeof setTimeout>;
const schedulePurplePeek = () => {
  if (loginForm.password && loginForm.password.length > 0 && showPassword.value) {
    purplePeekTimeout = setTimeout(() => {
      isPurplePeeking.value = true;
      setTimeout(() => {
        isPurplePeeking.value = false;
      }, 800);
    }, Math.random() * 3000 + 2000);
  }
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

// Watch password for peeking
watch(() => [loginForm.password, showPassword.value], () => {
  clearTimeout(purplePeekTimeout);
  if (loginForm.password && loginForm.password.length > 0 && showPassword.value) {
    schedulePurplePeek();
  } else {
    isPurplePeeking.value = false;
  }
});

onMounted(() => {
  window.addEventListener("mousemove", handleMouseMove);
  schedulePurpleBlink();
  scheduleBlackBlink();
  refreshCaptcha();
});

onUnmounted(() => {
  window.removeEventListener("mousemove", handleMouseMove);
  clearTimeout(purpleBlinkTimeout);
  clearTimeout(blackBlinkTimeout);
  clearTimeout(purplePeekTimeout);
  clearTimeout(lookingTimeout);
});

const refreshCaptcha = async () => {
  try {
    const res = await getCaptcha();
    if (res.data) {
      captchaImg.value = res.data.img;
      captchaUuid.value = res.data.uuid;
      loginForm.captchaCode = "";
    }
  } catch {
    // ignore
  }
};

const handleLogin = async () => {
  // 防止重复提交
  if (loading.value) {
    return;
  }

  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning("请填写完整的登录信息");
    return;
  }
  if (!loginForm.captchaCode) {
    ElMessage.warning("请输入验证码");
    return;
  }

  loading.value = true;
  try {
    await userStore.loginAction(loginForm.username, loginForm.password, captchaUuid.value, loginForm.captchaCode);
    ElMessage.success("登录成功");

    // 清空验证码，防止跳转后误触发
    loginForm.captchaCode = "";
    captchaUuid.value = "";

    router.push("/welcome");
  } catch (error: any) {
    ElMessage.error(error.message || "登录失败");
    refreshCaptcha();
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

/* Captcha image */
.captcha-image {
  width: 130px;
  height: 48px;
  border-radius: 8px;
  border: 1.5px solid #374151;
  background-color: #1f2937;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  transition: all 0.2s;
  flex-shrink: 0;
}

.captcha-image:hover {
  border-color: #4b5563;
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

/* Social Button */
.social-button {
  width: 100%;
  height: 48px;
  font-size: 15px;
  color: #d1d5db;
  background-color: #1f2937;
  border: 1.5px solid #374151;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.social-button:hover {
  background-color: #374151;
  border-color: #4b5563;
}

/* Dark checkbox */
.dark-checkbox :deep(.el-checkbox__inner) {
  background: #1f2937 !important;
  border-color: #374151 !important;
}

.dark-checkbox :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background: #3b82f6 !important;
  border-color: #3b82f6 !important;
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
