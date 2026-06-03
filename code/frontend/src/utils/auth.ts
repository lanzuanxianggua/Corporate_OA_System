/**
 * 清除用户认证状态（本地存储中的token等）
 */
export function clearAuthState() {
  localStorage.removeItem("token");
  localStorage.removeItem("refreshToken");
  localStorage.removeItem("userInfo");
}
