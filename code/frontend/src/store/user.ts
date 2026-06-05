import { defineStore } from "pinia";
import { ref } from "vue";
import { logout as logoutApi } from "@/api/auth";
import type { LoginResult, UserInfo } from "@/api/auth";
import router from "@/router";

export const useUserStore = defineStore("user", () => {
  // ── State ──────────────────────────────────────────────────────────────────

  const token = ref(localStorage.getItem("token") || "");
  const refreshToken = ref(localStorage.getItem("refreshToken") || "");
  const userInfo = ref<UserInfo | null>(
    localStorage.getItem("userInfo")
      ? JSON.parse(localStorage.getItem("userInfo")!)
      : null
  );
  const permissions = ref<string[]>(
    localStorage.getItem("permissions")
      ? JSON.parse(localStorage.getItem("permissions")!)
      : []
  );
  const roles = ref<string[]>(
    localStorage.getItem("roles")
      ? JSON.parse(localStorage.getItem("roles")!)
      : []
  );

  // ── Getters / helpers ─────────────────────────────────────────────────────

  function hasRole(role: string): boolean {
    return roles.value.some(
      (r) => r === role || r === role.toUpperCase()
    );
  }

  function hasAnyRole(checkRoles: string[]): boolean {
    if (!checkRoles?.length) return true;
    return checkRoles.some((r) => hasRole(r));
  }

  function isAdmin(): boolean {
    return hasRole("ADMIN");
  }

  // ── Actions ───────────────────────────────────────────────────────────────

  /**
   * Called after a successful login API call.
   * Persists token / userInfo to localStorage and updates reactive state.
   */
  function setLoginResult(res: LoginResult) {
    token.value = res.accessToken;
    refreshToken.value = res.refreshToken;

    // Add backward-compatible aliases for existing views
    const info = res.userInfo;
    info.id = info.empId;
    info.empName = info.realName;
    userInfo.value = info;

    permissions.value = info.permissions || [];
    roles.value = info.roles || [];

    localStorage.setItem("token", res.accessToken);
    localStorage.setItem("refreshToken", res.refreshToken);
    localStorage.setItem("userInfo", JSON.stringify(info));
    localStorage.setItem("permissions", JSON.stringify(info.permissions || []));
    localStorage.setItem("roles", JSON.stringify(info.roles || []));
  }

  /** Log out: call backend, clear state, redirect to login. */
  async function logoutAction() {
    try {
      await logoutApi();
    } catch {
      // ignore errors on logout
    }
    clearAuth();
    router.push("/login");
  }

  /** Clear all auth state without calling backend. */
  function clearAuth() {
    token.value = "";
    refreshToken.value = "";
    userInfo.value = null;
    permissions.value = [];
    roles.value = [];

    localStorage.removeItem("token");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("userInfo");
    localStorage.removeItem("permissions");
    localStorage.removeItem("roles");
  }

  return {
    // state
    token,
    refreshToken,
    userInfo,
    permissions,
    roles,
    // helpers
    hasRole,
    hasAnyRole,
    isAdmin,
    // actions
    setLoginResult,
    logoutAction,
    clearAuth,
  };
});
