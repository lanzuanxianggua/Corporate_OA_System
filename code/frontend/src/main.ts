import { createApp } from "vue";
import { createPinia } from "pinia";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import "element-plus/theme-chalk/dark/css-vars.css";
import zhCn from "element-plus/es/locale/lang/zh-cn";
import * as ElementPlusIconsVue from "@element-plus/icons-vue";
import App from "./App.vue";
import router from "./router";
import { useThemeStore } from "@/store/theme";
import "./style.css";

const app = createApp(App);
const pinia = createPinia();

// 全量注册 Element Plus 图标，避免按需注册遗漏导致图标不显示
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  if (key !== "default") app.component(key, component);
}

app.use(pinia);
useThemeStore().initTheme();
app.use(router);
app.use(ElementPlus, { locale: zhCn });
app.mount("#app");
