import { createApp } from "vue";
import { createPinia } from "pinia";
import {
  User, Menu, Setting, Bell, Search, Edit, Delete, Plus, Download,
  Upload, Refresh, Files, Document, Message, ChatDotSquare, Check,
  Close, HomeFilled, Folder, DataAnalysis, Timer, Calendar, Clock, List,
  Notification, Star, WarnTriangleFilled, InfoFilled, SuccessFilled,
  CircleCloseFilled, Expand, Fold, ArrowDown
} from "@element-plus/icons-vue";
import App from "./App.vue";
import router from "./router";
import "./style.css";

const app = createApp(App);

// 按需注册实际使用的图标
const icons = [
  User, Menu, Setting, Bell, Search, Edit, Delete, Plus, Download,
  Upload, Refresh, Files, Document, Message, ChatDotSquare, Check,
  Close, HomeFilled, Folder, DataAnalysis, Timer, Calendar, Clock, List,
  Notification, Star, WarnTriangleFilled, InfoFilled, SuccessFilled,
  CircleCloseFilled, Expand, Fold, ArrowDown
];
icons.forEach(component => app.component(component.name!, component));

app.use(createPinia());
app.use(router);
app.mount("#app");
