/**
 * OA System - Frontend UI Automated Tests (Playwright)
 *
 * Usage: npx playwright test tests/frontend-ui-test.spec.ts
 * Requires: Frontend running on http://localhost:8848, Backend on http://localhost:8080
 */
import { test, expect, Page } from "@playwright/test";
import { execSync } from "child_process";
import { setTimeout as sleep } from "timers/promises";

const BASE_URL = "http://localhost:8848";
const API_URL = "http://localhost:8080";
const REDIS_CLI = "C:/Program Files/Redis/redis-cli.exe";

// Cache tokens to avoid rate limiting
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
  // Wait between logins to avoid rate limiting (5 req/min)
  if (!_userToken) {
    await sleep(15000);
    _userToken = await getLoginToken("wujiu");
  }
}

async function loginByToken(page: Page, username: string) {
  await ensureTokens();
  const token = username === "admin" ? _adminToken : _userToken;
  if (!token) throw new Error(`No token for ${username}`);

  await page.goto(`${BASE_URL}/#/login`);
  await page.waitForLoadState("networkidle");
  await page.waitForTimeout(500);

  // Decode JWT to get empId and empName
  const payload = JSON.parse(Buffer.from(token.split(".")[1], "base64").toString());

  await page.evaluate(({ t, p }) => {
    localStorage.setItem("token", t);
    localStorage.setItem("refreshToken", t);
    localStorage.setItem("userInfo", JSON.stringify({
      username: p.empName || "admin",
      empName: p.empName || "admin",
      empId: p.empId,
    }));
  }, { t: token, p: payload });

  await page.goto(`${BASE_URL}/#/`);
  await page.waitForLoadState("networkidle");
  await page.waitForTimeout(1500);
}

// ============================================================
test.describe.configure({ mode: "serial" });

test.describe("1. Login Page", () => {
  test("should display login form", async ({ page }) => {
    await page.goto(`${BASE_URL}/#/login`);
    await page.waitForLoadState("networkidle");
    await page.waitForTimeout(1000);
    const inputs = page.locator("input");
    await expect(inputs.first()).toBeVisible({ timeout: 5000 });
  });

  test("should login as admin via token injection", async ({ page }) => {
    await loginByToken(page, "admin");
    await page.waitForTimeout(2000);
    const url = page.url();
    expect(url).not.toContain("/login");
  });
});

// ============================================================
test.describe("2. Workbench / Dashboard", () => {
  test.beforeEach(async ({ page }) => {
    await loginByToken(page, "admin");
  });

  test("should load workbench page", async ({ page }) => {
    await page.goto(`${BASE_URL}/#/oa/workbench`);
    await page.waitForLoadState("networkidle");
    await page.waitForTimeout(1500);
    const bodyText = await page.locator("body").textContent();
    expect(bodyText).not.toContain("系统繁忙");
  });

  test("should load dashboard with stats", async ({ page }) => {
    await page.goto(`${BASE_URL}/#/oa/dashboard`);
    await page.waitForLoadState("networkidle");
    await page.waitForTimeout(1500);
    const bodyText = await page.locator("body").textContent();
    expect(bodyText).not.toContain("系统繁忙");
  });
});

// ============================================================
test.describe("3. OA Application Pages", () => {
  test.beforeEach(async ({ page }) => {
    await loginByToken(page, "wujiu");
  });

  const oaPages = [
    { name: "Leave Apply", path: "/oa/leave/apply" },
    { name: "Overtime", path: "/oa/overtime-apply" },
    { name: "Business Trip", path: "/oa/business-trip/apply" },
    { name: "Outing", path: "/oa/outing/apply" },
    { name: "Purchase", path: "/oa/purchase/apply" },
    { name: "Expense", path: "/oa/expense/apply" },
    { name: "Loan", path: "/oa/loan-apply" },
  ];

  for (const oaPage of oaPages) {
    test(`should load ${oaPage.name} page without error`, async ({ page }) => {
      await page.goto(`${BASE_URL}/#${oaPage.path}`);
      await page.waitForLoadState("networkidle");
      await page.waitForTimeout(2000);
      const bodyText = await page.locator("body").textContent();
      expect(bodyText).not.toContain("系统繁忙");
      expect(bodyText).not.toContain("404");
    });
  }
});

// ============================================================
test.describe("4. Admin Pages", () => {
  test.beforeEach(async ({ page }) => {
    await loginByToken(page, "admin");
  });

  const adminPages = [
    { name: "Employee Management", path: "/system/user" },
    { name: "Role Management", path: "/system/role" },
    { name: "Dept Management", path: "/system/dept" },
    { name: "Notice", path: "/oa/notice/list" },
    { name: "Contract", path: "/oa/contract" },
    { name: "Asset", path: "/oa/asset" },
    { name: "Salary", path: "/oa/salary" },
    { name: "Attendance Group", path: "/oa/attendance-group" },
  ];

  for (const adminPage of adminPages) {
    test(`should load ${adminPage.name} page`, async ({ page }) => {
      await page.goto(`${BASE_URL}/#${adminPage.path}`);
      await page.waitForLoadState("networkidle");
      await page.waitForTimeout(2000);
      const bodyText = await page.locator("body").textContent();
      expect(bodyText).not.toContain("系统繁忙");
    });
  }
});

// ============================================================
test.describe("5. Approval Center", () => {
  test("should load approval center", async ({ page }) => {
    await loginByToken(page, "admin");
    await page.goto(`${BASE_URL}/#/oa/approval-center`);
    await page.waitForLoadState("networkidle");
    await page.waitForTimeout(2000);
    const bodyText = await page.locator("body").textContent();
    expect(bodyText).not.toContain("系统繁忙");
  });
});

// ============================================================
test.describe("6. Other OA Pages", () => {
  test.beforeEach(async ({ page }) => {
    await loginByToken(page, "wujiu");
  });

  const otherPages = [
    { name: "Attendance Clock", path: "/oa/attendance/clock" },
    { name: "Todo", path: "/oa/todo" },
    { name: "Message", path: "/oa/message/list" },
    { name: "Document", path: "/oa/document/list" },
    { name: "Schedule", path: "/oa/schedule/index" },
    { name: "Meeting", path: "/oa/meeting" },
    { name: "Leave Balance", path: "/oa/leave-balance" },
  ];

  for (const otherPage of otherPages) {
    test(`should load ${otherPage.name} page`, async ({ page }) => {
      await page.goto(`${BASE_URL}/#${otherPage.path}`);
      await page.waitForLoadState("networkidle");
      await page.waitForTimeout(2000);
      const bodyText = await page.locator("body").textContent();
      expect(bodyText).not.toContain("系统繁忙");
    });
  }
});
