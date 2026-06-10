import { createApp } from "vue";
import { createPinia } from "pinia";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import "element-plus/theme-chalk/dark/css-vars.css";
import zhCn from "element-plus/es/locale/lang/zh-cn";
import {
  User, Menu, Setting, Bell, Search, Edit, Delete, Plus, Download,
  Upload, Refresh, Files, Document, Message, ChatDotSquare, Check,
  Close, HomeFilled, Folder, DataAnalysis, Timer, Calendar, Clock, List,
  Notification, Star, WarnTriangleFilled, InfoFilled, SuccessFilled,
  CircleCloseFilled, Expand, Fold, ArrowDown
} from "@element-plus/icons-vue";
import App from "./App.vue";
import router from "./router";
import { useThemeStore } from "@/store/theme";
import "./style.css";

const app = createApp(App);
const pinia = createPinia();

// 按需注册实际使用的图标
const icons = [
  User, Menu, Setting, Bell, Search, Edit, Delete, Plus, Download,
  Upload, Refresh, Files, Document, Message, ChatDotSquare, Check,
  Close, HomeFilled, Folder, DataAnalysis, Timer, Calendar, Clock, List,
  Notification, Star, WarnTriangleFilled, InfoFilled, SuccessFilled,
  CircleCloseFilled, Expand, Fold, ArrowDown
];
icons.forEach(component => app.component(component.name!, component));

app.use(pinia);
useThemeStore().initTheme();
app.use(router);
app.use(ElementPlus, { locale: zhCn });
app.mount("#app");
