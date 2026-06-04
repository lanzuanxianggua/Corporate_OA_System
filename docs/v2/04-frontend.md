# 04 - Corporate OA System v2 前端架构

> 版本: v2.0-draft
> 日期: 2026-06-04
> 状态: **Phase 1 设计中**
> 前置阅读: `00-index.md`、`01-architecture.md`、`03-api-spec.md`

---

## 1. 技术栈

| 维度 | 选型 | 版本 | 理由 |
|------|------|------|------|
| 框架 | Vue 3 | 3.5+ | 组合式 API、性能 |
| 语言 | TypeScript | 5.x | 类型安全 |
| 构建 | Vite | 6.x | 快速 HMR |
| UI | Element Plus | 2.8+ | 组件丰富 |
| 状态 | Pinia | 2.x | Vue 官方 |
| 路由 | Vue Router | 4.x | 动态路由 |
| HTTP | Axios | 1.x | 拦截器、取消 |
| 工具 | VueUse | 11.x | 组合式工具 |
| 校验 | Zod | 3.x | TS 优先 |
| 表格 | VxeTable | 4.x | 大数据量 |
| 富文本 | TinyMCE | 7.x | 文档模块 |
| 时间 | Day.js | 1.x | 轻量 |
| 图表 | ECharts | 5.x | 报表 |
| 测试 | Vitest + Vue Test Utils | - | Vite 原生 |
| E2E | Playwright | 1.x | 跨浏览器 |
| 代码规范 | ESLint + Prettier + Stylelint | - | 一致性 |
| 提交规范 | Husky + Commitlint | - | Git workflow |

---

## 2. 目录结构

```
code/frontend/
├── public/                          # 静态资源（不打包）
│   ├── favicon.ico
│   └── logo.png
├── src/
│   ├── api/                         # API 客户端（按模块）
│   │   ├── platform/                # 平台
│   │   │   ├── auth.ts              # 登录/登出/刷新
│   │   │   ├── user.ts              # 当前用户/切换部门
│   │   │   └── dict.ts              # 字典
│   │   ├── workflow/
│   │   │   ├── task.ts              # 我的待办/已办
│   │   │   ├── instance.ts         # 流程实例
│   │   │   └── definition.ts       # 流程定义
│   │   ├── hr-leave/
│   │   │   ├── leave.ts             # 请假
│   │   │   ├── balance.ts          # 余额
│   │   │   └── rule.ts              # 规则
│   │   ├── admin/                   # 行政
│   │   ├── document/                # 文档
│   │   ├── finance/                 # 财务
│   │   ├── .../
│   │   └── index.ts                 # 统一导出
│   ├── assets/                      # 静态资源（打包）
│   │   ├── images/
│   │   ├── icons/                   # SVG 图标
│   │   └── styles/
│   │       ├── index.scss           # 全局样式
│   │       ├── reset.scss           # CSS reset
│   │       ├── variables.scss      # 主题变量
│   │       └── element-plus.scss    # EP 主题覆盖
│   ├── components/                  # 通用组件（业务无关）
│   │   ├── base/
│   │   │   ├── BaseTable.vue        # 通用表格（封装 VxeTable）
│   │   │   ├── BaseForm.vue         # 通用表单
│   │   │   ├── BaseSearch.vue       # 通用搜索栏
│   │   │   ├── BaseDialog.vue       # 通用对话框
│   │   │   ├── BaseDrawer.vue       # 通用抽屉
│   │   │   ├── BasePagination.vue   # 通用分页
│   │   │   ├── BaseUpload.vue       # 通用上传
│   │   │   ├── BaseIcon.vue         # 通用图标
│   │   │   └── BaseEmpty.vue        # 空状态
│   │   ├── business/                # 通用业务组件
│   │   │   ├── UserSelector.vue     # 员工选择
│   │   │   ├── DeptSelector.vue     # 部门选择
│   │   │   ├── RoleSelector.vue     # 角色选择
│   │   │   ├── DateRangePicker.vue  # 日期范围
│   │   │   ├── FileUploader.vue     # 文件上传
│   │   │   ├── ApprovalFlow.vue     # 审批流
│   │   │   ├── AttachmentList.vue   # 附件列表
│   │   │   └── ...
│   │   └── layout/                  # 布局组件
│   │       ├── LayoutDefault.vue    # 默认布局
│   │       ├── LayoutBlank.vue      # 空白布局（登录）
│   │       ├── Sidebar.vue          # 侧边栏
│   │       ├── Navbar.vue           # 顶栏
│   │       ├── Breadcrumb.vue       # 面包屑
│   │       └── TagsView.vue         # 多页签
│   ├── composables/                 # 组合式 API（use*）
│   │   ├── useTable.ts              # 表格逻辑封装
│   │   ├── useForm.ts               # 表单逻辑封装
│   │   ├── useDict.ts               # 字典数据
│   │   ├── useAuth.ts               # 权限检查
│   │   ├── useRequest.ts            # 异步请求封装
│   │   ├── usePagination.ts         # 分页
│   │   ├── useUserStore.ts          # 用户 store
│   │   ├── useDebounce.ts           # 防抖
│   │   ├── useThrottle.ts           # 节流
│   │   └── ...
│   ├── directives/                  # 自定义指令
│   │   ├── permission.ts            # v-permission
│   │   ├── copy.ts                  # v-copy
│   │   ├── debounce.ts              # v-debounce
│   │   └── ...
│   ├── enums/                       # 枚举（与后端一致）
│   │   ├── hr-leave.ts              # 请假类型/状态
│   │   ├── workflow.ts              # 工作流状态
│   │   └── common.ts
│   ├── locales/                     # 国际化（v2 留位）
│   │   ├── zh-CN.ts
│   │   ├── en-US.ts
│   │   └── index.ts
│   ├── plugins/                     # 插件
│   │   ├── element-plus.ts          # EP 完整注册
│   │   ├── pinia.ts                 # Pinia 持久化
│   │   └── router.ts                # 路由守卫
│   ├── router/                      # 路由
│   │   ├── index.ts                 # 路由入口
│   │   ├── routes.ts                # 静态路由
│   │   ├── dynamic.ts               # 动态路由生成
│   │   └── guards.ts                # 路由守卫
│   ├── stores/                      # Pinia store
│   │   ├── user.ts                  # 当前用户
│   │   ├── permission.ts            # 权限/路由
│   │   ├── dict.ts                  # 字典缓存
│   │   ├── app.ts                   # 应用全局（主题/语言）
│   │   ├── tags.ts                  # 多页签
│   │   └── ...
│   ├── types/                       # TS 类型定义
│   │   ├── api.d.ts                 # API 类型（与后端 DTO/VO 对齐）
│   │   ├── common.d.ts              # 通用类型
│   │   ├── global.d.ts              # 全局类型
│   │   └── shims.d.ts               # shim
│   ├── utils/                       # 工具函数
│   │   ├── request.ts               # Axios 封装
│   │   ├── auth.ts                  # Token 存储
│   │   ├── permission.ts            # 权限工具
│   │   ├── format.ts                # 格式化（日期/金额）
│   │   ├── validation.ts            # 校验
│   │   ├── storage.ts               # 本地存储
│   │   ├── sign.ts                  # 接口签名
│   │   ├── encrypt.ts               # 加密
│   │   └── ...
│   ├── views/                       # 页面（按模块）
│   │   ├── platform/
│   │   │   ├── login/               # 登录
│   │   │   ├── profile/             # 个人中心
│   │   │   ├── system/              # 系统管理
│   │   │   │   ├── user/
│   │   │   │   ├── role/
│   │   │   │   ├── menu/
│   │   │   │   ├── dict/
│   │   │   │   └── config/
│   │   │   └── monitor/             # 监控
│   │   ├── workflow/
│   │   │   ├── task/                # 我的待办
│   │   │   ├── instance/            # 流程实例
│   │   │   ├── definition/          # 流程设计
│   │   │   └── cc/                  # 抄送
│   │   ├── hr-leave/
│   │   │   ├── my-leave/            # 我的请假
│   │   │   ├── leave-apply/         # 请假申请
│   │   │   ├── leave-approval/      # 请假审批
│   │   │   ├── my-balance/          # 我的余额
│   │   │   ├── balance-manage/      # 余额管理
│   │   │   └── rule-manage/         # 规则管理
│   │   ├── admin/
│   │   ├── document/
│   │   ├── finance/
│   │   ├── knowledge/
│   │   ├── message/
│   │   ├── meeting/
│   │   ├── task/
│   │   ├── error/
│   │   │   ├── 404.vue
│   │   │   └── 403.vue
│   │   └── home.vue                 # 首页
│   ├── App.vue                      # 根组件
│   ├── main.ts                      # 入口
│   ├── env.d.ts                      # Vite env 声明
│   └── vite-env.d.ts
├── .env.development                 # 开发环境变量
├── .env.production                  # 生产环境变量
├── .eslintrc.cjs                    # ESLint 配置
├── .prettierrc.json                 # Prettier 配置
├── .stylelintrc.cjs                 # Stylelint 配置
├── .editorconfig                    # 编辑器配置
├── .gitignore
├── index.html                       # HTML 入口
├── package.json
├── pnpm-lock.yaml                   # pnpm 锁
├── tsconfig.json                    # TS 根配置
├── tsconfig.app.json                # 应用 TS 配置
├── tsconfig.node.json               # Node TS 配置
├── vite.config.ts                   # Vite 配置
└── README.md
```

**关键规则**：
- `src/api/` 严格按业务模块组织（与后端模块一一对应）
- `src/views/` 与 `src/api/` 同构
- `src/components/base/` 是与业务无关的通用组件
- `src/components/business/` 是跨业务通用的业务组件
- 业务专属组件放在 `src/views/{module}/_components/`

---

## 3. 命名规范

### 3.1 文件命名
- **Vue 组件**: PascalCase（`UserSelector.vue` `BaseTable.vue`）
- **TS 文件**: kebab-case（`user-store.ts` `format.ts`）
- **目录**: kebab-case（`hr-leave/` `my-leave/`）
- **路由路径**: kebab-case（`/hr-leave/my-leave`）

### 3.2 命名禁止
- ❌ 大写驼峰文件名（`UserSelector.vue` OK，`user-selector.vue` ❌）
- ❌ 缩写（`UsrSel.vue` ❌）
- ❌ 数字开头（`2-user.vue` ❌）
- ❌ 与 HTML 标签冲突（`Form.vue` ❌ 用 `BaseForm.vue`）

### 3.3 组件名（setup script）
```vue
<script setup lang="ts">
// 组件名由文件名自动推导，无需 options.name
</script>
```

---

## 4. 状态管理（Pinia）

### 4.1 Store 分层
- **user**: 当前用户、Token、权限
- **permission**: 动态路由、权限码
- **dict**: 字典缓存
- **app**: 主题、语言、侧边栏折叠
- **tags**: 多页签
- **breadcrumb**: 面包屑
- **各业务模块独立 store**（按需）

### 4.2 Store 写法（Setup Store）
```ts
// stores/user.ts
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '');
  const userInfo = ref<UserInfo | null>(null);
  const permissions = ref<string[]>([]);
  const roles = ref<string[]>([]);
  
  const isLogin = computed(() => !!token.value);
  const isAdmin = computed(() => roles.value.includes('ADMIN'));
  
  async function login(form: LoginForm) {
    const res = await authApi.login(form);
    token.value = res.data.accessToken;
    localStorage.setItem('token', token.value);
    await fetchUserInfo();
  }
  
  async function fetchUserInfo() {
    const res = await userApi.getCurrent();
    userInfo.value = res.data;
    permissions.value = res.data.permissions;
    roles.value = res.data.roles;
  }
  
  function logout() {
    token.value = '';
    userInfo.value = null;
    permissions.value = [];
    roles.value = [];
    localStorage.removeItem('token');
  }
  
  return {
    token, userInfo, permissions, roles,
    isLogin, isAdmin,
    login, fetchUserInfo, logout
  };
});
```

### 4.3 持久化
**Pinia 持久化策略**：
- `token` → localStorage（不持久化到 pinia-plugin，自动同步）
- `userInfo` / `permissions` / `roles` → sessionStorage（关闭标签页清空）
- `app.theme` / `app.sidebar` → localStorage
- `tags` → sessionStorage

---

## 5. 路由

### 5.1 路由结构

**静态路由**（无需权限）：
```ts
// router/routes.ts
export const constantRoutes = [
  {
    path: '/login',
    component: () => import('@/views/platform/login/index.vue'),
    meta: { layout: 'blank', title: '登录' }
  },
  {
    path: '/403',
    component: () => import('@/views/error/403.vue'),
    meta: { layout: 'blank', title: '无权限' }
  },
  {
    path: '/404',
    component: () => import('@/views/error/404.vue'),
    meta: { layout: 'blank', title: '页面不存在' }
  }
];
```

**动态路由**（需登录后由后端返回）：
```ts
// router/dynamic.ts
export async function generateRoutes(perms: string[]): Promise<RouteRecordRaw[]> {
  const res = await permissionApi.getMenus();
  const menus = res.data;
  return buildRoutes(menus, perms);
}
```

**示例**：
```ts
{
  path: '/hr-leave',
  component: LayoutDefault,
  redirect: '/hr-leave/my-leave',
  meta: { title: 'HR 请假', icon: 'Calendar' },
  children: [
    {
      path: 'my-leave',
      component: () => import('@/views/hr-leave/my-leave/index.vue'),
      meta: { title: '我的请假', perm: 'hr-leave:leave:list' }
    },
    {
      path: 'apply',
      component: () => import('@/views/hr-leave/leave-apply/index.vue'),
      meta: { title: '请假申请', perm: 'hr-leave:leave:create' }
    },
    // ...
  ]
}
```

### 5.2 路由守卫

```ts
// router/guards.ts
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore();
  NProgress.start();
  
  // 1. 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - Corporate OA` : 'Corporate OA';
  
  // 2. 公开路由
  if (isPublicRoute(to)) {
    return next();
  }
  
  // 3. 未登录
  if (!userStore.isLogin) {
    return next({ path: '/login', query: { redirect: to.fullPath } });
  }
  
  // 4. 已登录但未加载权限
  if (!userStore.userInfo) {
    try {
      await userStore.fetchUserInfo();
      const dynamicRoutes = await generateRoutes(userStore.permissions);
      dynamicRoutes.forEach(r => router.addRoute(r));
      return next({ ...to, replace: true });
    } catch (e) {
      userStore.logout();
      return next({ path: '/login' });
    }
  }
  
  // 5. 检查权限
  if (to.meta.perm && !userStore.permissions.includes(to.meta.perm as string)) {
    return next({ path: '/403' });
  }
  
  next();
});
```

### 5.3 多页签（Tags View）
- 已访问的路由入栈
- 关闭标签触发路由跳转
- 持久化到 sessionStorage

---

## 6. HTTP 客户端（Axios 封装）

### 6.1 request.ts 完整实现

```ts
// utils/request.ts
import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useUserStore } from '@/stores/user';
import { signRequest } from './sign';
import NProgress from 'nprogress';

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30000,
  withCredentials: true,
});

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    NProgress.start();
    
    // 1. Token
    const userStore = useUserStore();
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`;
    }
    
    // 2. Trace ID
    config.headers['X-Trace-Id'] = generateTraceId();
    
    // 3. 签名（写操作）
    if (['post', 'put', 'patch', 'delete'].includes(config.method!.toLowerCase())) {
      const timestamp = Math.floor(Date.now() / 1000).toString();
      config.headers['X-Timestamp'] = timestamp;
      config.headers['X-Sign'] = signRequest(config, timestamp);
    }
    
    // 4. 幂等键
    if (config.method!.toLowerCase() === 'post' && !config.headers['Idempotency-Key']) {
      config.headers['Idempotency-Key'] = generateIdempotencyKey();
    }
    
    return config;
  },
  (error) => {
    NProgress.done();
    return Promise.reject(error);
  }
);

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse<R<any>>) => {
    NProgress.done();
    
    const res = response.data;
    
    // 文件下载（blob）
    if (response.config.responseType === 'blob') {
      return response;
    }
    
    // 业务错误
    if (res.code !== 0) {
      // Token 过期
      if (res.code === 10002 || res.code === 10003) {
        handleTokenExpired();
        return Promise.reject(new Error(res.message));
      }
      
      // 限流
      if (res.code === 30001) {
        ElMessage.warning('请求过于频繁，请稍后重试');
        return Promise.reject(new Error(res.message));
      }
      
      // 通用错误
      ElMessage.error(res.message || '操作失败');
      return Promise.reject(new Error(res.message));
    }
    
    return res.data;
  },
  (error) => {
    NProgress.done();
    
    if (error.response?.status === 401) {
      handleTokenExpired();
    } else if (error.response?.status === 403) {
      ElMessage.error('无权限访问');
    } else if (error.response?.status === 500) {
      ElMessage.error('服务器错误');
    } else {
      ElMessage.error(error.message || '网络错误');
    }
    
    return Promise.reject(error);
  }
);

function handleTokenExpired() {
  const userStore = useUserStore();
  ElMessageBox.confirm('登录已过期，请重新登录', '提示', {
    confirmButtonText: '重新登录',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    userStore.logout();
    window.location.href = '/login';
  }).catch(() => {});
}

// 业务方法
export function get<T>(url: string, params?: any, config?: AxiosRequestConfig): Promise<T> {
  return service.get(url, { params, ...config });
}

export function post<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return service.post(url, data, config);
}

export function put<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return service.put(url, data, config);
}

export function del<T>(url: string, params?: any, config?: AxiosRequestConfig): Promise<T> {
  return service.delete(url, { params, ...config });
}

export function upload<T>(url: string, formData: FormData, onProgress?: (e: ProgressEvent) => void): Promise<T> {
  return service.post(url, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress,
  });
}

export function download(url: string, params?: any, filename?: string): Promise<void> {
  return service.get(url, { params, responseType: 'blob' }).then((response) => {
    const blob = new Blob([response.data]);
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = filename || extractFilename(response.headers) || 'download';
    link.click();
    URL.revokeObjectURL(link.href);
  });
}

export default service;
```

### 6.2 API 模块示例

```ts
// api/hr-leave/leave.ts
import { get, post, put } from '@/utils/request';
import type { PageResult, HrLeave, HrLeaveVO, HrLeaveCreateDTO, HrLeaveQueryDTO } from '@/types/api';

const BASE = '/api/v1/hr-leave/leaves';

export const leaveApi = {
  /** 查询我的请假 */
  listMy(query: HrLeaveQueryDTO): Promise<PageResult<HrLeaveVO>> {
    return get(BASE, { ...query, empId: 'me' });
  },
  
  /** 查询详情 */
  get(id: number): Promise<HrLeaveVO> {
    return get(`${BASE}/${id}`);
  },
  
  /** 提交申请 */
  create(dto: HrLeaveCreateDTO): Promise<{ id: number; applyNo: string }> {
    return post(BASE, dto);
  },
  
  /** 撤回 */
  revoke(id: number, reason: string): Promise<void> {
    return post(`${BASE}/${id}/actions/revoke`, { reason });
  },
  
  /** 重新提交 */
  resubmit(id: number): Promise<void> {
    return post(`${BASE}/${id}/actions/resubmit`);
  },
};
```

---

## 7. 组件设计

### 7.1 BaseTable 通用表格

**Props**：
```ts
interface Props {
  columns: TableColumn[];
  data: any[];
  loading?: boolean;
  pagination?: { page: number; size: number; total: number };
  selection?: boolean;
  showIndex?: boolean;
  showActions?: boolean;
  actionsWidth?: number;
  rowKey?: string;
  height?: number | string;
  size?: 'large' | 'default' | 'small';
  stripe?: boolean;
  border?: boolean;
}
```

**Emits**：
```ts
emit('selection-change', rows: any[]);
emit('page-change', page: number);
emit('size-change', size: number);
emit('row-click', row: any);
emit('action', { type: string; row: any });
```

**Slots**：
- `actions` 自定义操作列
- `toolbar` 工具栏
- `empty` 空状态
- `{column.prop}` 列内容

**使用示例**：
```vue
<BaseTable
  :columns="columns"
  :data="data"
  :loading="loading"
  :pagination="pagination"
  selection
  @selection-change="onSelectionChange"
  @page-change="onPageChange"
>
  <template #toolbar>
    <el-button type="primary" @click="onCreate">新建请假</el-button>
  </template>
  <template #actions="{ row }">
    <el-button link type="primary" @click="onView(row)">查看</el-button>
    <el-button link type="primary" @click="onRevoke(row)" v-if="canRevoke(row)">撤回</el-button>
  </template>
</BaseTable>
```

### 7.2 BaseForm 通用表单

**Props**：
```ts
interface Props {
  model: Record<string, any>;
  items: FormItem[];
  rules?: FormRules;
  labelWidth?: string;
  labelPosition?: 'left' | 'right' | 'top';
  size?: 'large' | 'default' | 'small';
  disabled?: boolean;
  showActions?: boolean;
  submitText?: string;
  resetText?: string;
}
```

**FormItem 类型**：
```ts
type FormItem = {
  prop: string;
  label: string;
  type: 'input' | 'textarea' | 'select' | 'date' | 'datetime' | 'daterange' | 'number' | 'switch' | 'radio' | 'checkbox' | 'cascader' | 'upload' | 'custom';
  span?: number;
  required?: boolean;
  options?: { label: string; value: any }[];
  attrs?: Record<string, any>;
  rules?: ValidationRule[];
  visibleWhen?: (model: any) => boolean;
  render?: (model: any) => any;  // 自定义渲染（type=custom）
};
```

**使用示例**：
```vue
<BaseForm
  v-model="form"
  :items="formItems"
  :rules="rules"
  @submit="onSubmit"
  @reset="onReset"
/>

<script setup>
const form = ref({ leaveType: 'ANNUAL', days: 1 });
const formItems = [
  { prop: 'leaveType', label: '假期类型', type: 'select', required: true,
    options: dictStore.getOptions('HR_LEAVE_TYPE') },
  { prop: 'startTime', label: '开始时间', type: 'datetime', required: true },
  { prop: 'endTime', label: '结束时间', type: 'datetime', required: true },
  { prop: 'reason', label: '请假原因', type: 'textarea', span: 24 },
];
</script>
```

### 7.3 ApprovalFlow 审批流组件

**用途**：展示审批流的当前节点、审批人、审批历史

**Props**：
```ts
interface Props {
  instanceId: number;          // 流程实例 ID
  showHistory?: boolean;       // 是否显示历史
  showActions?: boolean;       // 是否显示审批操作
  mode?: 'view' | 'edit';     // 查看/编辑模式
}
```

**功能**：
- 节点状态可视化（已完成/当前/未开始）
- 审批人头像
- 审批意见
- 操作按钮（通过/驳回/转交）
- 流程图缩略图

---

## 8. 权限控制

### 8.1 路由级权限
- `meta.perm` 字段
- 路由守卫检查
- 无权限跳转 `/403`

### 8.2 按钮级权限
**指令 v-permission**：
```ts
// directives/permission.ts
import type { Directive } from 'vue';
import { useUserStore } from '@/stores/user';

export const permission: Directive = {
  mounted(el, binding) {
    const { value } = binding;
    const userStore = useUserStore();
    
    if (value && !userStore.permissions.includes(value)) {
      el.parentNode?.removeChild(el);
    }
  },
};
```

**使用**：
```vue
<el-button v-permission="'hr-leave:leave:create'" @click="onCreate">
  新建请假
</el-button>

<el-button v-permission="['hr-leave:leave:approve', 'hr-leave:leave:reject']">
  审批
</el-button>
```

### 8.3 数据权限
- 前端通过 `useDataPermission()` composable 判断
- 显示/隐藏功能（不是数据过滤，数据过滤在后端）

```ts
// composables/useDataPermission.ts
export function useDataPermission() {
  const userStore = useUserStore();
  
  function canView(scope: 'SELF' | 'DEPT' | 'DEPT_DOWN' | 'COMPANY' | 'ALL'): boolean {
    const userScope = userStore.dataPermission;
    const levels = { SELF: 0, DEPT: 1, DEPT_DOWN: 2, COMPANY: 3, ALL: 4 };
    return levels[userScope] >= levels[scope];
  }
  
  return { canView };
}
```

---

## 9. 表单校验

### 9.1 校验规则
- 使用 `async-validator`（Element Plus 内置）
- 自定义规则
- 跨字段校验
- 异步校验（如请假冲突）

```ts
const rules = {
  startTime: [
    { required: true, message: '请选择开始时间' },
    { validator: validateLeaveTime, trigger: 'change' },
  ],
  endTime: [
    { required: true, message: '请选择结束时间' },
    { validator: validateEndAfterStart, trigger: 'change' },
  ],
  days: [
    { required: true, message: '请输入天数' },
    { type: 'number', min: 0.5, max: 365, message: '天数范围 0.5-365' },
  ],
  reason: [
    { required: true, message: '请填写请假原因' },
    { max: 500, message: '原因不能超过 500 字' },
  ],
};

async function validateLeaveTime(rule: any, value: string, callback: (err?: Error) => void) {
  if (!value) return callback();
  const start = new Date(value);
  const now = new Date();
  if (start < now) {
    callback(new Error('开始时间不能早于当前时间'));
  } else {
    callback();
  }
}
```

### 9.2 Zod 模式（与 TS 共享）

```ts
// types/api.ts
import { z } from 'zod';

export const HrLeaveCreateSchema = z.object({
  leaveType: z.enum(['PERSONAL', 'ANNUAL', 'SICK', 'MARRIAGE', 'FUNERAL', 'MATERNITY', 'PATERNITY', 'COMPENSATORY', 'OTHER']),
  startTime: z.string().datetime(),
  endTime: z.string().datetime(),
  leavePeriod: z.enum(['FULL', 'AM', 'PM']).default('FULL'),
  days: z.number().min(0.5).max(365),
  reason: z.string().min(1).max(500),
  attachments: z.array(z.string()).optional(),
});

export type HrLeaveCreateDTO = z.infer<typeof HrLeaveCreateSchema>;
```

---

## 10. 错误处理

### 10.1 全局错误处理

```ts
// utils/errorHandler.ts
import { ElMessage, ElNotification } from 'element-plus';

export const errorHandler = {
  /** 网络错误 */
  networkError(err: any) {
    ElMessage.error('网络连接失败，请检查网络');
  },
  
  /** 业务错误 */
  businessError(code: number, message: string) {
    if (code === 10002) return; // Token 过期已在拦截器处理
    ElMessage.error(message);
  },
  
  /** 表单校验错误 */
  validationError(errors: any[]) {
    ElNotification.error({
      title: '表单校验失败',
      message: errors.map(e => `${e.field}: ${e.message}`).join('\n'),
    });
  },
  
  /** 未知错误 */
  unknownError(err: any) {
    console.error('Unknown error:', err);
    ElMessage.error('未知错误，请稍后重试');
  },
};

// main.ts
app.config.errorHandler = (err, vm, info) => {
  console.error('Vue error:', err, info);
  errorHandler.unknownError(err);
};
```

---

## 11. 主题与样式

### 11.1 SCSS 变量
```scss
// assets/styles/variables.scss

// 主题色
$primary-color: #409eff;
$success-color: #67c23a;
$warning-color: #e6a23c;
$danger-color: #f56c6c;
$info-color: #909399;

// 中性色
$text-primary: #303133;
$text-regular: #606266;
$text-secondary: #909399;
$text-placeholder: #c0c4cc;

// 边框
$border-base: #dcdfe6;
$border-light: #e4e7ed;
$border-lighter: #ebeef5;
$border-extra-light: #f2f6fc;

// 字号
$font-size-extra-small: 12px;
$font-size-small: 13px;
$font-size-base: 14px;
$font-size-medium: 16px;
$font-size-large: 18px;
$font-size-extra-large: 20px;

// 间距
$spacing-mini: 4px;
$spacing-small: 8px;
$spacing-base: 12px;
$spacing-medium: 16px;
$spacing-large: 20px;
$spacing-extra-large: 24px;
```

### 11.2 主题切换
- 默认：light
- 暗黑：dark（v2 留位，Phase 3+ 完善）
- 自定义：用户配置（v2 留位）

### 11.3 命名规范（BEM）
```scss
.user-card {
  &__header { }
  &__body { }
  &__footer { }
  &--active { }
  &__icon {
    &--large { }
    &--small { }
  }
}
```

---

## 12. 性能优化

### 12.1 路由懒加载
```ts
component: () => import('@/views/hr-leave/my-leave/index.vue')
```

### 12.2 组件懒加载
- 大组件（富文本/图表）异步加载
- `defineAsyncComponent`

### 12.3 虚拟滚动
- 大列表用 `el-table-v2` 或 `vxe-table` 虚拟滚动
- 表格行数 > 1000 时强制启用

### 12.4 防抖节流
- 搜索输入：debounce 300ms
- 滚动事件：throttle 100ms
- 按钮点击：loading 状态防重复

### 12.5 缓存
- 字典数据：sessionStorage 缓存
- 路由：keep-alive（按需）
- API：相同请求 5s 内缓存

### 12.6 CDN
- Element Plus 走 CDN
- ECharts 走 CDN
- 生产环境配置 Vite `build.rollupOptions.output.manualChunks`

---

## 13. 国际化（v2 留位）

```ts
// locales/zh-CN.ts
export default {
  common: {
    confirm: '确认',
    cancel: '取消',
    save: '保存',
    delete: '删除',
    edit: '编辑',
    create: '新建',
    search: '搜索',
    reset: '重置',
    export: '导出',
    import: '导入',
  },
  hrLeave: {
    myLeave: '我的请假',
    leaveApply: '请假申请',
    leaveType: '假期类型',
    annual: '年假',
    sick: '病假',
    // ...
  },
};
```

**v2 仅预留接口，UI 文本不实现多语言**。所有 UI 文字直接写在模板里。

---

## 14. 测试

### 14.1 单元测试（Vitest）
```ts
// composables/useTable.test.ts
import { describe, it, expect, vi } from 'vitest';
import { useTable } from '@/composables/useTable';

describe('useTable', () => {
  it('loads data', async () => {
    const fetchData = vi.fn().mockResolvedValue({
      list: [{ id: 1 }, { id: 2 }],
      total: 2,
    });
    
    const table = useTable(fetchData);
    await table.search();
    
    expect(table.data.value).toHaveLength(2);
    expect(table.total.value).toBe(2);
  });
});
```

### 14.2 组件测试（Vue Test Utils）
```ts
// components/BaseTable.test.ts
import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import BaseTable from '@/components/base/BaseTable.vue';

describe('BaseTable', () => {
  it('renders data', () => {
    const wrapper = mount(BaseTable, {
      props: {
        columns: [{ prop: 'name', label: '姓名' }],
        data: [{ name: '张三' }],
      },
    });
    expect(wrapper.text()).toContain('张三');
  });
});
```

### 14.3 E2E 测试（Playwright）
```ts
// e2e/login.spec.ts
import { test, expect } from '@playwright/test';

test('login flow', async ({ page }) => {
  await page.goto('/login');
  await page.fill('input[name="username"]', 'admin');
  await page.fill('input[name="password"]', 'admin123');
  await page.click('button[type="submit"]');
  await expect(page).toHaveURL('/');
  await expect(page.locator('text=欢迎')).toBeVisible();
});
```

---

## 15. 部署

### 15.1 构建
```bash
pnpm build  # 输出到 dist/
```

### 15.2 Nginx 配置
```nginx
server {
  listen 80;
  server_name oa.example.com;
  root /var/www/oa-v2;
  index index.html;
  
  # SPA 路由
  location / {
    try_files $uri $uri/ /index.html;
  }
  
  # API 反代
  location /api/ {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
  }
  
  # 静态资源缓存
  location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf)$ {
    expires 1y;
    add_header Cache-Control "public, immutable";
  }
  
  # gzip
  gzip on;
  gzip_types text/css application/javascript application/json image/svg+xml;
}
```

### 15.3 Docker
```dockerfile
# 多阶段构建
FROM node:20-alpine AS builder
WORKDIR /app
COPY package.json pnpm-lock.yaml ./
RUN npm install -g pnpm && pnpm install --frozen-lockfile
COPY . .
RUN pnpm build

FROM nginx:1.27-alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

---

## 16. 总结

v2 前端架构核心：
1. **Vue 3 + TS + Vite + Pinia**：现代、高效
2. **Element Plus**：成熟组件库
3. **模块化 API**：与后端模块一一对应
4. **统一封装**：请求/响应/错误/权限
5. **可测试**：单元/组件/E2E 三层
6. **可部署**：Docker + Nginx

v2 不实现：
- 微前端（qiankun/wujie）
- SSR/SSG（Nuxt 暂不引入）
- 完整 i18n（留位）
- 主题切换（v3 考虑）
- PWA（v3 考虑）
