/**
 * OA System - Mobile UI Automated Tests (Playwright)
 *
 * Usage: npx playwright test tests/mobile-ui-test.spec.ts
 * Requires: Mobile H5 running on http://localhost:5173 (uni-app dev port), Backend on http://localhost:8080
 *
 * Simulates mobile viewport (375x812 - iPhone X) and tests all mobile pages.
 */
import { test, expect, Page } from "@playwright/test";
import { execSync } from "child_process";
import { setTimeout as sleep } from "timers/promises";

const API_URL = "http://localhost:8080";
const REDIS_CLI = "C:/Program Files/Redis/redis-cli.exe";

// Cache tokens
let _adminToken = "";
let _userToken = "";

async function getLoginToken(username: string, password: string = "123456"): Promise<string> {
  const captchaResp = await fetch(`${API_URL}/api/auth/captcha`);
  const captchaData = await captchaResp.json();
  const uuid = captchaData.data.uuid;

  let captchaCode = "";
  try {
    captchaCode = execSync(`"${REDIS_CLI}" GET "captcha:${uuid}"`, { encoding: "utf-8" })
      .replace(/"/g, "").trim();
  } catch {}

  const loginResp = await fetch(`${API_URL}/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password, captchaCode, captchaUuid: uuid }),
  });
  const loginData = await loginResp.json();
  return loginData.data?.accessToken || "";
}

async function ensureTokens() {
  if (!_adminToken) {
    _adminToken = await getLoginToken("admin");
  }
  if (!_userToken) {
    await sleep(15000);
    _userToken = await getLoginToken("wujiu");
  }
}

// Detect mobile dev server port
async function detectBaseUrl(page: Page): Promise<string> {
  for (const port of [5173, 5174, 5175]) {
    try {
      const resp = await page.request.get(`http://localhost:${port}`, { timeout: 2000 });
      if (resp.ok()) return `http://localhost:${port}`;
    } catch {}
  }
  return "http://localhost:5173";
}

// Helper: login by injecting token into localStorage
async function mobileLogin(page: Page, baseUrl: string, username: string) {
  await ensureTokens();
  const token = username === "admin" ? _adminToken : _userToken;
  if (!token) throw new Error(`No token for ${username}`);

  await page.goto(`${baseUrl}/#/pages/login/index`);
  await page.waitForLoadState("networkidle");
  await page.waitForTimeout(500);

  const payload = JSON.parse(Buffer.from(token.split(".")[1], "base64").toString());

  // uni-app mobile uses uni.setStorageSync but in H5 mode it's localStorage
  await page.evaluate(({ t, p }) => {
    localStorage.setItem("token", t);
    localStorage.setItem("refreshToken", t);
    localStorage.setItem("userInfo", JSON.stringify({
      username: p.empName || username,
      empName: p.empName || username,
      empId: p.empId,
    }));
    // uni-app uses uni_xxx prefix for some storage keys
    localStorage.setItem("uni_token", t);
    localStorage.setItem("uni_userInfo", JSON.stringify({
      username: p.empName || username,
      empName: p.empName || username,
      empId: p.empId,
    }));
  }, { t: token, p: payload });

  // Navigate to home
  await page.goto(`${baseUrl}/#/pages/home/index`);
  await page.waitForLoadState("networkidle");
  await page.waitForTimeout(1500);
}

test.describe.configure({ mode: "serial" });

test.describe("Mobile UI Tests", () => {
  test.use({
    viewport: { width: 375, height: 812 },
    userAgent:
      "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1",
  });

  let baseUrl: string;

  test.beforeAll(async ({ browser }) => {
    const page = await browser.newPage();
    baseUrl = await detectBaseUrl(page);
    await page.close();
  });

  test("should display login page", async ({ page }) => {
    await page.goto(`${baseUrl}/#/pages/login/index`);
    await page.waitForLoadState("networkidle");
    await page.waitForTimeout(1500);

    const inputs = page.locator("input");
    const count = await inputs.count();
    expect(count).toBeGreaterThanOrEqual(1);
  });

  test("should login as user via token", async ({ page }) => {
    await mobileLogin(page, baseUrl, "wujiu");
    await page.waitForTimeout(2000);
    const url = page.url();
    // After login, should not be on login page
    expect(url).not.toContain("/login");
  });

  test("should load home/workspace page", async ({ page }) => {
    await mobileLogin(page, baseUrl, "wujiu");
    await page.goto(`${baseUrl}/#/pages/home/index`);
    await page.waitForLoadState("networkidle");
    await page.waitForTimeout(2000);
    const bodyText = await page.locator("body").textContent();
    expect(bodyText).not.toContain("系统繁忙");
  });

  test("should load todo page", async ({ page }) => {
    await mobileLogin(page, baseUrl, "wujiu");
    await page.goto(`${baseUrl}/#/pages/todo/index`);
    await page.waitForLoadState("networkidle");
    await page.waitForTimeout(2000);
    const bodyText = await page.locator("body").textContent();
    expect(bodyText).not.toContain("系统繁忙");
  });

  test("should load approval list", async ({ page }) => {
    await mobileLogin(page, baseUrl, "admin");
    await page.goto(`${baseUrl}/#/pages/approval/list`);
    await page.waitForLoadState("networkidle");
    await page.waitForTimeout(2000);
    const bodyText = await page.locator("body").textContent();
    expect(bodyText).not.toContain("系统繁忙");
  });

  test("should load mine/profile page", async ({ page }) => {
    await mobileLogin(page, baseUrl, "wujiu");
    await page.goto(`${baseUrl}/#/pages/mine/index`);
    await page.waitForLoadState("networkidle");
    await page.waitForTimeout(2000);
    const bodyText = await page.locator("body").textContent();
    expect(bodyText).not.toContain("系统繁忙");
  });

  // OA business pages
  const mobileOAPages = [
    { name: "Attendance", path: "/pages/oa/attendance" },
    { name: "Leave Apply", path: "/pages/oa/leave-apply" },
    { name: "Leave List", path: "/pages/oa/leave-list" },
    { name: "Business Trip", path: "/pages/oa/business-trip" },
    { name: "Outing", path: "/pages/oa/outing" },
    { name: "Overtime", path: "/pages/oa/overtime" },
    { name: "Purchase", path: "/pages/oa/purchase" },
    { name: "Expense", path: "/pages/oa/expense" },
    { name: "Loan", path: "/pages/oa/loan" },
    { name: "Notice List", path: "/pages/oa/notice-list" },
    { name: "Document", path: "/pages/oa/document" },
    { name: "Schedule", path: "/pages/oa/schedule" },
    { name: "Message", path: "/pages/oa/message" },
  ];

  for (const oaPage of mobileOAPages) {
    test(`should load ${oaPage.name} page`, async ({ page }) => {
      await mobileLogin(page, baseUrl, "wujiu");
      await page.goto(`${baseUrl}/#${oaPage.path}`);
      await page.waitForLoadState("networkidle");
      await page.waitForTimeout(2000);
      // Page should render (may show empty data if API proxy not configured in dev)
      const bodyText = await page.locator("body").textContent();
      // Should not show 404 or crash
      expect(bodyText).not.toContain("404");
      expect(bodyText).not.toContain("页面不存在");
    });
  }
});
