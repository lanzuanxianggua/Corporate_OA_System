import { computed, ref, watch } from "vue";
import { defineStore } from "pinia";

export type ThemeMode = "light" | "dark" | "system";

const STORAGE_KEY = "oa_theme_mode";
const themeModes: ThemeMode[] = ["light", "dark", "system"];

function getStoredMode(): ThemeMode {
  const stored = localStorage.getItem(STORAGE_KEY) as ThemeMode | null;
  return stored && themeModes.includes(stored) ? stored : "system";
}

function getSystemDark() {
  return window.matchMedia?.("(prefers-color-scheme: dark)").matches ?? false;
}

export const useThemeStore = defineStore("theme", () => {
  const mode = ref<ThemeMode>(getStoredMode());
  const systemDark = ref(getSystemDark());
  let mediaQuery: MediaQueryList | null = null;

  const isDark = computed(() => mode.value === "dark" || (mode.value === "system" && systemDark.value));

  function applyTheme() {
    const root = document.documentElement;
    root.classList.toggle("dark", isDark.value);
    root.dataset.theme = isDark.value ? "dark" : "light";
    root.style.colorScheme = isDark.value ? "dark" : "light";
    window.dispatchEvent(new CustomEvent("oa-theme-change", {
      detail: { mode: mode.value, isDark: isDark.value }
    }));
  }

  function initTheme() {
    if (!mediaQuery) {
      mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");
      const handleSystemTheme = (event: MediaQueryListEvent) => {
        systemDark.value = event.matches;
      };
      mediaQuery.addEventListener("change", handleSystemTheme);
    }
    applyTheme();
  }

  function setThemeMode(nextMode: ThemeMode) {
    mode.value = nextMode;
    localStorage.setItem(STORAGE_KEY, nextMode);
  }

  function toggleDark() {
    setThemeMode(isDark.value ? "light" : "dark");
  }

  watch([mode, systemDark], applyTheme);

  return { mode, isDark, setThemeMode, toggleDark, initTheme };
});
