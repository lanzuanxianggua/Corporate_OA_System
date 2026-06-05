import { defineStore } from "pinia";
import { ref } from "vue";

export const useAppStore = defineStore("app", () => {
  const sidebarCollapsed = ref(false);
  const theme = ref<"light" | "dark">("light");

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value;
  }

  function setTheme(t: "light" | "dark") {
    theme.value = t;
  }

  return { sidebarCollapsed, theme, toggleSidebar, setTheme };
});
