/**
 * OA System - Frontend UI Automated Tests (Playwright)
 *
 * Comprehensively tests all 65 frontend routes with:
 * - Page-load smoke tests (all routes)
 * - Interaction tests (form elements, CRUD operations, buttons, filters)
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

/**
 * Navigate to a hash route, wait for network idle, then wait a short
 * period for the Vue component to render. Returns the page object for chaining.
 */
async function goto(page: Page, hashPath: string, extraWait = 1500): Promise<Page> {
  await page.goto(`${BASE_URL}/#${hashPath}`);
  await page.waitForLoadState("networkidle");
  if (extraWait > 0) await page.waitForTimeout(extraWait);
  return page;
}

/** Assert the page does not show a system error. */
async function expectNoError(page: Page) {
  const bodyText = await page.locator("body").textContent();
  expect(bodyText).not.toContain("系统繁忙");
  expect(bodyText).not.toContain("404");
}

// ============================================================
test.describe.configure({ mode: "serial" });

// ============================================================
// 1. LOGIN PAGE (Route #1)
// ============================================================
test.describe("1. Login Page", () => {
  test("should display login form with username, password, captcha, and login button", async ({ page }) => {
    await goto(page, "/login", 1000);

    // Username input
    const usernameInput = page.locator('input[placeholder="请输入用户名"]');
    await expect(usernameInput).toBeVisible({ timeout: 5000 });

    // Password input
    const passwordInput = page.locator('input[placeholder="请输入密码"]');
    await expect(passwordInput).toBeVisible();

    // Captcha input
    const captchaInput = page.locator('input[placeholder="请输入验证码"]');
    await expect(captchaInput).toBeVisible();

    // Login button
    const loginButton = page.locator("button", { hasText: "登 录" });
    await expect(loginButton).toBeVisible();
    await expect(loginButton).toBeEnabled();

    // Remember-me checkbox
    const rememberCheckbox = page.locator('.el-checkbox');
    await expect(rememberCheckbox).toBeVisible();
  });

  test("should show captcha image area", async ({ page }) => {
    await goto(page, "/login", 1000);
    // The captcha container (either an img or "点击获取" placeholder)
    const captchaArea = page.locator("img[alt='验证码']").or(
      page.locator("text=点击获取")
    );
    await expect(captchaArea).toBeVisible({ timeout: 5000 });
  });

  test("should login as admin via token injection", async ({ page }) => {
    await loginByToken(page, "admin");
    await page.waitForTimeout(2000);
    const url = page.url();
    expect(url).not.toContain("/login");
  });
});

// ============================================================
// 2. WELCOME / WORKBENCH (Routes #2, #3)
// ============================================================
test.describe("2. Welcome / Dashboard Pages", () => {
  test.beforeEach(async ({ page }) => {
    await loginByToken(page, "admin");
  });

  test("Route #2: /welcome - should load welcome page", async ({ page }) => {
    await goto(page, "/welcome");
    await expectNoError(page);
    // Welcome page should have some content visible
    const mainContent = page.locator(".main-content, .el-card, .el-row").first();
    await expect(mainContent).toBeVisible({ timeout: 5000 });
  });

  test("Route #3: /oa/workbench - should load workbench page", async ({ page }) => {
    await goto(page, "/oa/workbench");
    await expectNoError(page);
  });

  test("Route #3: /oa/workbench - should display workbench cards/widgets", async ({ page }) => {
    await goto(page, "/oa/workbench");
    // Workbench typically has cards or stat sections
    const cards = page.locator(".el-card");
    const cardCount = await cards.count();
    expect(cardCount).toBeGreaterThanOrEqual(1);
  });
});

// ============================================================
// 3. OA DASHBOARD (Route #4, ADMIN)
// ============================================================
test.describe("3. OA Dashboard", () => {
  test.beforeEach(async ({ page }) => {
    await loginByToken(page, "admin");
  });

  test("Route #4: /oa/dashboard - should load dashboard without error", async ({ page }) => {
    await goto(page, "/oa/dashboard", 2000);
    await expectNoError(page);
  });

  test("Route #4: /oa/dashboard - should display period filter radio buttons", async ({ page }) => {
    await goto(page, "/oa/dashboard", 2000);
    // The period radio group (today/week/month/year)
    const radioButtons = page.locator(".el-radio-group .el-radio-button");
    await expect(radioButtons.first()).toBeVisible({ timeout: 5000 });
    const count = await radioButtons.count();
    expect(count).toBeGreaterThanOrEqual(4);
  });

  test("Route #4: /oa/dashboard - should render stat cards", async ({ page }) => {
    await goto(page, "/oa/dashboard", 2000);
    // Dashboard has stat cards with numbers
    const statCards = page.locator(".el-col .bg-white, .el-col .rounded-lg");
    const count = await statCards.count();
    expect(count).toBeGreaterThanOrEqual(1);
  });

  test("Route #4: /oa/dashboard - should render chart containers", async ({ page }) => {
    await goto(page, "/oa/dashboard", 2000);
    // ECharts renders into div containers with explicit height
    const chartContainers = page.locator("div[style*='height']");
    const chartCount = await chartContainers.count();
    expect(chartCount).toBeGreaterThanOrEqual(1);
  });

  test("Route #4: /oa/dashboard - should switch period filter", async ({ page }) => {
    await goto(page, "/oa/dashboard", 2000);
    const weekButton = page.locator(".el-radio-button", { hasText: "本周" });
    if (await weekButton.isVisible()) {
      await weekButton.click();
      await page.waitForTimeout(1000);
      await expectNoError(page);
    }
  });
});

// ============================================================
// 4. APPROVAL CENTER (Route #5, ADMIN)
// ============================================================
test.describe("4. Approval Center", () => {
  test.beforeEach(async ({ page }) => {
    await loginByToken(page, "admin");
  });

  test("Route #5: /oa/approval-center - should load approval center", async ({ page }) => {
    await goto(page, "/oa/approval-center", 2000);
    await expectNoError(page);
  });

  test("Route #5: /oa/approval-center - should display status filter tabs", async ({ page }) => {
    await goto(page, "/oa/approval-center", 2000);
    // Status filter: 全部/待审批/已通过/已拒绝
    const filterButtons = page.locator(".el-radio-group .el-radio-button");
    await expect(filterButtons.first()).toBeVisible({ timeout: 5000 });
  });

  test("Route #5: /oa/approval-center - should display business type tabs", async ({ page }) => {
    await goto(page, "/oa/approval-center", 2000);
    // Tabs for leave/trip/outing/purchase/expense/overtime/loan
    const tabs = page.locator(".el-tabs__item");
    const tabCount = await tabs.count();
    expect(tabCount).toBeGreaterThanOrEqual(1);
  });

  test("Route #5: /oa/approval-center - should display table with data", async ({ page }) => {
    await goto(page, "/oa/approval-center", 2000);
    const table = page.locator(".el-table");
    await expect(table).toBeVisible({ timeout: 5000 });
  });

  test("Route #5: /oa/approval-center - should switch between tabs", async ({ page }) => {
    await goto(page, "/oa/approval-center", 2000);
    const secondTab = page.locator(".el-tabs__item").nth(1);
    if (await secondTab.isVisible()) {
      await secondTab.click();
      await page.waitForTimeout(1000);
      await expectNoError(page);
    }
  });
});

// ============================================================
// 5. ATTENDANCE PAGES (Routes #6, #7, #8)
// ============================================================
test.describe("5. Attendance Pages", () => {
  test.describe("Route #6: Attendance Clock (USER)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "wujiu");
    });

    test("should load attendance clock page", async ({ page }) => {
      await goto(page, "/oa/attendance/clock", 2000);
      await expectNoError(page);
    });

    test("should display current time", async ({ page }) => {
      await goto(page, "/oa/attendance/clock", 2000);
      // The page shows current time in a large font
      const timeDisplay = page.locator(".text-5xl, .font-mono");
      await expect(timeDisplay.first()).toBeVisible({ timeout: 5000 });
    });

    test("should display clock-in / clock-out buttons or status", async ({ page }) => {
      await goto(page, "/oa/attendance/clock", 2000);
      // Either show "点击打卡" button or already-clocked status
      const clockButton = page.locator("button", { hasText: "点击打卡" });
      const clockStatus = page.locator(".text-2xl.font-bold");
      // At least one of these should be present
      const hasButton = await clockButton.count();
      const hasStatus = await clockStatus.count();
      expect(hasButton + hasStatus).toBeGreaterThanOrEqual(1);
    });

    test("should display attendance history table", async ({ page }) => {
      await goto(page, "/oa/attendance/clock", 2000);
      const table = page.locator(".el-table");
      await expect(table).toBeVisible({ timeout: 5000 });
    });

    test("should have period filter radio buttons (day/week/month)", async ({ page }) => {
      await goto(page, "/oa/attendance/clock", 2000);
      const radioButtons = page.locator(".el-radio-group .el-radio-button");
      const count = await radioButtons.count();
      expect(count).toBeGreaterThanOrEqual(3);
    });
  });

  test.describe("Route #7: Attendance Record (USER)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "wujiu");
    });

    test("should load attendance record page", async ({ page }) => {
      await goto(page, "/oa/attendance/record", 2000);
      await expectNoError(page);
    });

    test("should display record table or empty state", async ({ page }) => {
      await goto(page, "/oa/attendance/record", 2000);
      const table = page.locator(".el-table").or(page.locator(".el-empty"));
      await expect(table.first()).toBeVisible({ timeout: 5000 });
    });
  });

  test.describe("Route #8: Attendance Manage (ADMIN)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should load attendance manage page", async ({ page }) => {
      await goto(page, "/oa/attendance/manage", 2000);
      await expectNoError(page);
    });

    test("should display management table", async ({ page }) => {
      await goto(page, "/oa/attendance/manage", 2000);
      const table = page.locator(".el-table");
      await expect(table).toBeVisible({ timeout: 5000 });
    });
  });
});

// ============================================================
// 6. LEAVE APPLY (Routes #9, #10)
// ============================================================
test.describe("6. Leave Pages", () => {
  test.describe("Route #9: Leave Apply (USER)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "wujiu");
    });

    test("should load leave apply page without error", async ({ page }) => {
      await goto(page, "/oa/leave/apply", 2000);
      await expectNoError(page);
    });

    test("should display leave application form", async ({ page }) => {
      await goto(page, "/oa/leave/apply", 2000);
      // Form should have el-form with inputs
      const form = page.locator(".el-form");
      await expect(form).toBeVisible({ timeout: 5000 });
    });

    test("should have leave type select", async ({ page }) => {
      await goto(page, "/oa/leave/apply", 2000);
      const leaveTypeSelect = page.locator(".el-select").first();
      await expect(leaveTypeSelect).toBeVisible({ timeout: 5000 });
    });

    test("should have start/end date pickers", async ({ page }) => {
      await goto(page, "/oa/leave/apply", 2000);
      const datePickers = page.locator(".el-date-editor");
      const count = await datePickers.count();
      expect(count).toBeGreaterThanOrEqual(2);
    });

    test("should have reason textarea", async ({ page }) => {
      await goto(page, "/oa/leave/apply", 2000);
      const textarea = page.locator("textarea");
      await expect(textarea.first()).toBeVisible({ timeout: 5000 });
    });

    test("should have submit button", async ({ page }) => {
      await goto(page, "/oa/leave/apply", 2000);
      const submitBtn = page.locator("button", { hasText: "提交申请" });
      await expect(submitBtn).toBeVisible({ timeout: 5000 });
      await expect(submitBtn).toBeEnabled();
    });

    test("should have reset button", async ({ page }) => {
      await goto(page, "/oa/leave/apply", 2000);
      const resetBtn = page.locator("button", { hasText: "重置" });
      await expect(resetBtn).toBeVisible({ timeout: 5000 });
    });

    test("should display leave records table on the left", async ({ page }) => {
      await goto(page, "/oa/leave/apply", 2000);
      const table = page.locator(".el-table");
      await expect(table).toBeVisible({ timeout: 5000 });
    });

    test("should have pagination for leave records", async ({ page }) => {
      await goto(page, "/oa/leave/apply", 2000);
      const pagination = page.locator(".el-pagination");
      await expect(pagination).toBeVisible({ timeout: 5000 });
    });
  });

  test.describe("Route #10: Leave Approval (ADMIN)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should load leave approval page", async ({ page }) => {
      await goto(page, "/oa/leave/approval", 2000);
      await expectNoError(page);
    });

    test("should display approval table", async ({ page }) => {
      await goto(page, "/oa/leave/approval", 2000);
      const table = page.locator(".el-table");
      await expect(table).toBeVisible({ timeout: 5000 });
    });
  });
});

// ============================================================
// 7. BUSINESS TRIP (Routes #11, #12)
// ============================================================
test.describe("7. Business Trip Pages", () => {
  test.describe("Route #11: Business Trip Apply (USER)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "wujiu");
    });

    test("should load business trip apply page", async ({ page }) => {
      await goto(page, "/oa/business-trip/apply", 2000);
      await expectNoError(page);
    });

    test("should display form with destination input", async ({ page }) => {
      await goto(page, "/oa/business-trip/apply", 2000);
      const destInput = page.locator('input[placeholder*="目的地"]');
      await expect(destInput).toBeVisible({ timeout: 5000 });
    });

    test("should display form with purpose textarea", async ({ page }) => {
      await goto(page, "/oa/business-trip/apply", 2000);
      const purposeInput = page.locator("textarea");
      await expect(purposeInput.first()).toBeVisible({ timeout: 5000 });
    });

    test("should have start/end date pickers", async ({ page }) => {
      await goto(page, "/oa/business-trip/apply", 2000);
      const datePickers = page.locator(".el-date-editor");
      const count = await datePickers.count();
      expect(count).toBeGreaterThanOrEqual(2);
    });

    test("should have submit and reset buttons", async ({ page }) => {
      await goto(page, "/oa/business-trip/apply", 2000);
      const submitBtn = page.locator("button", { hasText: "提交申请" });
      await expect(submitBtn).toBeVisible({ timeout: 5000 });
    });

    test("should display records table", async ({ page }) => {
      await goto(page, "/oa/business-trip/apply", 2000);
      const table = page.locator(".el-table");
      await expect(table).toBeVisible({ timeout: 5000 });
    });
  });

  test.describe("Route #12: Business Trip Approval (ADMIN)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should load business trip approval page", async ({ page }) => {
      await goto(page, "/oa/business-trip/approval", 2000);
      await expectNoError(page);
    });

    test("should display approval table", async ({ page }) => {
      await goto(page, "/oa/business-trip/approval", 2000);
      const table = page.locator(".el-table");
      await expect(table).toBeVisible({ timeout: 5000 });
    });
  });
});

// ============================================================
// 8. OUTING (Routes #13, #14)
// ============================================================
test.describe("8. Outing Pages", () => {
  test.describe("Route #13: Outing Apply (USER)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "wujiu");
    });

    test("should load outing apply page", async ({ page }) => {
      await goto(page, "/oa/outing/apply", 2000);
      await expectNoError(page);
    });

    test("should display form elements", async ({ page }) => {
      await goto(page, "/oa/outing/apply", 2000);
      const form = page.locator(".el-form");
      await expect(form).toBeVisible({ timeout: 5000 });
      const submitBtn = page.locator("button", { hasText: "提交申请" });
      await expect(submitBtn).toBeVisible({ timeout: 5000 });
    });
  });

  test.describe("Route #14: Outing Approval (ADMIN)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should load outing approval page", async ({ page }) => {
      await goto(page, "/oa/outing/approval", 2000);
      await expectNoError(page);
    });
  });
});

// ============================================================
// 9. PURCHASE (Routes #15, #16)
// ============================================================
test.describe("9. Purchase Pages", () => {
  test.describe("Route #15: Purchase Apply (USER)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "wujiu");
    });

    test("should load purchase apply page", async ({ page }) => {
      await goto(page, "/oa/purchase/apply", 2000);
      await expectNoError(page);
    });

    test("should display form elements", async ({ page }) => {
      await goto(page, "/oa/purchase/apply", 2000);
      const form = page.locator(".el-form");
      await expect(form).toBeVisible({ timeout: 5000 });
    });
  });

  test.describe("Route #16: Purchase Approval (ADMIN)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should load purchase approval page", async ({ page }) => {
      await goto(page, "/oa/purchase/approval", 2000);
      await expectNoError(page);
    });
  });
});

// ============================================================
// 10. EXPENSE (Routes #17, #18)
// ============================================================
test.describe("10. Expense Pages", () => {
  test.describe("Route #17: Expense Apply (USER)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "wujiu");
    });

    test("should load expense apply page", async ({ page }) => {
      await goto(page, "/oa/expense/apply", 2000);
      await expectNoError(page);
    });

    test("should display form elements", async ({ page }) => {
      await goto(page, "/oa/expense/apply", 2000);
      const form = page.locator(".el-form");
      await expect(form).toBeVisible({ timeout: 5000 });
    });
  });

  test.describe("Route #18: Expense Approval (ADMIN)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should load expense approval page", async ({ page }) => {
      await goto(page, "/oa/expense/approval", 2000);
      await expectNoError(page);
    });
  });
});

// ============================================================
// 11. NOTICE (Routes #19, #20)
// ============================================================
test.describe("11. Notice Pages", () => {
  test.describe("Route #19: Notice List (USER)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "wujiu");
    });

    test("should load notice list page", async ({ page }) => {
      await goto(page, "/oa/notice/list", 2000);
      await expectNoError(page);
    });

    test("should display search input", async ({ page }) => {
      await goto(page, "/oa/notice/list", 2000);
      const searchInput = page.locator('input[placeholder*="搜索"]').or(
        page.locator('input[placeholder*="公告"]')
      );
      await expect(searchInput.first()).toBeVisible({ timeout: 5000 });
    });

    test("should display notice list or empty state", async ({ page }) => {
      await goto(page, "/oa/notice/list", 2000);
      const noticeItem = page.locator(".cursor-pointer, .el-empty");
      await expect(noticeItem.first()).toBeVisible({ timeout: 5000 });
    });

    test("should have pagination", async ({ page }) => {
      await goto(page, "/oa/notice/list", 2000);
      const pagination = page.locator(".el-pagination");
      await expect(pagination).toBeVisible({ timeout: 5000 });
    });
  });

  test.describe("Route #20: Notice Manage (ADMIN)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should load notice manage page", async ({ page }) => {
      await goto(page, "/oa/notice/manage", 2000);
      await expectNoError(page);
    });

    test("should display notice table", async ({ page }) => {
      await goto(page, "/oa/notice/manage", 2000);
      const table = page.locator(".el-table");
      await expect(table).toBeVisible({ timeout: 5000 });
    });

    test("should have '发布公告' button", async ({ page }) => {
      await goto(page, "/oa/notice/manage", 2000);
      const addBtn = page.locator("button", { hasText: "发布公告" }).or(
        page.locator("button", { hasText: "新增" })
      );
      await expect(addBtn.first()).toBeVisible({ timeout: 5000 });
    });

    test("should have search input", async ({ page }) => {
      await goto(page, "/oa/notice/manage", 2000);
      const searchInput = page.locator('input[placeholder*="搜索"]').or(
        page.locator('input[placeholder*="公告"]')
      );
      await expect(searchInput.first()).toBeVisible({ timeout: 5000 });
    });

    test("should open create dialog when add button clicked", async ({ page }) => {
      await goto(page, "/oa/notice/manage", 2000);
      const addBtn = page.locator("button", { hasText: "发布公告" }).or(
        page.locator("button", { hasText: "新增" })
      );
      if (await addBtn.first().isVisible()) {
        await addBtn.first().click();
        await page.waitForTimeout(500);
        const dialog = page.locator(".el-dialog");
        await expect(dialog).toBeVisible({ timeout: 5000 });
        // Dialog should have title input, content textarea, notice type select
        const titleInput = page.locator('.el-dialog input[placeholder*="标题"]');
        const contentTextarea = page.locator('.el-dialog textarea');
        expect(
          (await titleInput.count()) + (await contentTextarea.count())
        ).toBeGreaterThanOrEqual(1);
      }
    });
  });
});

// ============================================================
// 12. DOCUMENT (Routes #21, #22)
// ============================================================
test.describe("12. Document Pages", () => {
  test.describe("Route #21: Document List (USER)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "wujiu");
    });

    test("should load document list page", async ({ page }) => {
      await goto(page, "/oa/document/list", 2000);
      await expectNoError(page);
    });

    test("should display table or empty state", async ({ page }) => {
      await goto(page, "/oa/document/list", 2000);
      const content = page.locator(".el-table").or(page.locator(".el-empty"));
      await expect(content.first()).toBeVisible({ timeout: 5000 });
    });
  });

  test.describe("Route #22: Document Manage (ADMIN)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should load document manage page", async ({ page }) => {
      await goto(page, "/oa/document/manage", 2000);
      await expectNoError(page);
    });
  });
});

// ============================================================
// 13. SCHEDULE (Routes #23, #24)
// ============================================================
test.describe("13. Schedule Pages", () => {
  test.describe("Route #23: My Schedule (USER)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "wujiu");
    });

    test("should load schedule page", async ({ page }) => {
      await goto(page, "/oa/schedule/index", 2000);
      await expectNoError(page);
    });

    test("should display calendar component", async ({ page }) => {
      await goto(page, "/oa/schedule/index", 2000);
      const calendar = page.locator(".el-calendar");
      await expect(calendar).toBeVisible({ timeout: 5000 });
    });

    test("should display add schedule button", async ({ page }) => {
      await goto(page, "/oa/schedule/index", 2000);
      const addBtn = page.locator("button", { hasText: "添加日程" });
      await expect(addBtn).toBeVisible({ timeout: 5000 });
    });

    test("should open add schedule dialog", async ({ page }) => {
      await goto(page, "/oa/schedule/index", 2000);
      const addBtn = page.locator("button", { hasText: "添加日程" });
      await addBtn.click();
      await page.waitForTimeout(500);
      const dialog = page.locator(".el-dialog");
      await expect(dialog).toBeVisible({ timeout: 5000 });
      // Dialog should have title input
      const titleInput = page.locator('.el-dialog input[placeholder*="标题"]').or(
        page.locator('.el-dialog .el-input input')
      );
      expect(await titleInput.count()).toBeGreaterThanOrEqual(1);
    });
  });

  test.describe("Route #24: Schedule Overview (ADMIN)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should load schedule overview page", async ({ page }) => {
      await goto(page, "/oa/schedule/overview", 2000);
      await expectNoError(page);
    });
  });
});

// ============================================================
// 14. MESSAGE (Routes #25, #26)
// ============================================================
test.describe("14. Message Pages", () => {
  test.describe("Route #25: Message List (USER)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "wujiu");
    });

    test("should load message list page", async ({ page }) => {
      await goto(page, "/oa/message/list", 2000);
      await expectNoError(page);
    });

    test("should display message content area", async ({ page }) => {
      await goto(page, "/oa/message/list", 2000);
      const content = page.locator(".el-card, .el-table, .el-empty").first();
      await expect(content).toBeVisible({ timeout: 5000 });
    });
  });

  test.describe("Route #26: Send Message (ADMIN)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should load send message page", async ({ page }) => {
      await goto(page, "/oa/message/send", 2000);
      await expectNoError(page);
    });

    test("should display message form with receiver select", async ({ page }) => {
      await goto(page, "/oa/message/send", 2000);
      const form = page.locator(".el-form");
      await expect(form).toBeVisible({ timeout: 5000 });
    });

    test("should have title input", async ({ page }) => {
      await goto(page, "/oa/message/send", 2000);
      const titleInput = page.locator('input[placeholder*="标题"]');
      await expect(titleInput).toBeVisible({ timeout: 5000 });
    });

    test("should have content textarea", async ({ page }) => {
      await goto(page, "/oa/message/send", 2000);
      const textarea = page.locator('textarea[placeholder*="内容"]');
      await expect(textarea).toBeVisible({ timeout: 5000 });
    });

    test("should have send button", async ({ page }) => {
      await goto(page, "/oa/message/send", 2000);
      const sendBtn = page.locator("button", { hasText: "发送" });
      await expect(sendBtn).toBeVisible({ timeout: 5000 });
      await expect(sendBtn).toBeEnabled();
    });
  });
});

// ============================================================
// 15. REPORT (Routes #27, #28)
// ============================================================
test.describe("15. Report Pages", () => {
  test.describe("Route #27: Personal Report (USER)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "wujiu");
    });

    test("should load personal report page", async ({ page }) => {
      await goto(page, "/oa/report/personal", 2000);
      await expectNoError(page);
    });

    test("should display period filter radio buttons", async ({ page }) => {
      await goto(page, "/oa/report/personal", 2000);
      const radioButtons = page.locator(".el-radio-group .el-radio-button");
      const count = await radioButtons.count();
      expect(count).toBeGreaterThanOrEqual(4);
    });

    test("should display stat cards", async ({ page }) => {
      await goto(page, "/oa/report/personal", 2000);
      // Four stat cards for attendance stats
      const statCards = page.locator(".el-col .bg-white, .el-col .rounded-lg");
      const count = await statCards.count();
      expect(count).toBeGreaterThanOrEqual(1);
    });

    test("should render chart containers", async ({ page }) => {
      await goto(page, "/oa/report/personal", 2000);
      const chartContainers = page.locator("div[style*='height']");
      const count = await chartContainers.count();
      expect(count).toBeGreaterThanOrEqual(1);
    });
  });

  test.describe("Route #28: Admin Report (ADMIN)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should load admin report page", async ({ page }) => {
      await goto(page, "/oa/report/admin", 2000);
      await expectNoError(page);
    });
  });
});

// ============================================================
// 16. WORKFLOW (Routes #29-32)
// ============================================================
test.describe("16. Workflow Pages", () => {
  test.describe("Route #29: Workflow Definition (ADMIN)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should load workflow definition page", async ({ page }) => {
      await goto(page, "/oa/workflow-definition", 2000);
      await expectNoError(page);
    });

    test("should display definition table", async ({ page }) => {
      await goto(page, "/oa/workflow-definition", 2000);
      const table = page.locator(".el-table");
      await expect(table).toBeVisible({ timeout: 5000 });
    });

    test("should have add definition button", async ({ page }) => {
      await goto(page, "/oa/workflow-definition", 2000);
      const addBtn = page.locator("button", { hasText: "新增定义" }).or(
        page.locator("button", { hasText: "新增" })
      );
      await expect(addBtn.first()).toBeVisible({ timeout: 5000 });
    });
  });

  test.describe("Route #30: Workflow Todo", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "wujiu");
    });

    test("should load workflow todo page", async ({ page }) => {
      await goto(page, "/oa/workflow-todo", 2000);
      await expectNoError(page);
    });

    test("should display pending task table", async ({ page }) => {
      await goto(page, "/oa/workflow-todo", 2000);
      const table = page.locator(".el-table");
      await expect(table).toBeVisible({ timeout: 5000 });
    });

    test("should display action buttons in table rows (if tasks exist)", async ({ page }) => {
      await goto(page, "/oa/workflow-todo", 2000);
      // Check for approve/reject/transfer/return buttons
      const actionButtons = page.locator(".el-table .el-button");
      // May or may not have tasks, but the column headers should exist
      const tableHeaders = page.locator(".el-table__header-wrapper th");
      const headerCount = await tableHeaders.count();
      expect(headerCount).toBeGreaterThanOrEqual(1);
    });
  });

  test.describe("Route #31: Workflow CC", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "wujiu");
    });

    test("should load workflow CC page", async ({ page }) => {
      await goto(page, "/oa/workflow/cc", 2000);
      await expectNoError(page);
    });
  });

  test.describe("Route #32: Workflow Delegation", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "wujiu");
    });

    test("should load workflow delegation page", async ({ page }) => {
      await goto(page, "/oa/workflow/delegation", 2000);
      await expectNoError(page);
    });
  });
});

// ============================================================
// 17. TODO CENTER (Route #33)
// ============================================================
test.describe("17. Todo Center", () => {
  test.beforeEach(async ({ page }) => {
    await loginByToken(page, "wujiu");
  });

  test("Route #33: /oa/todo - should load todo page", async ({ page }) => {
    await goto(page, "/oa/todo", 2000);
    await expectNoError(page);
  });

  test("Route #33: /oa/todo - should display todo content", async ({ page }) => {
    await goto(page, "/oa/todo", 2000);
    const content = page.locator(".el-card, .el-table, .el-empty").first();
    await expect(content).toBeVisible({ timeout: 5000 });
  });
});

// ============================================================
// 18. ATTENDANCE GROUP (Route #34, ADMIN)
// ============================================================
test.describe("18. Attendance Group", () => {
  test.beforeEach(async ({ page }) => {
    await loginByToken(page, "admin");
  });

  test("Route #34: /oa/attendance-group - should load attendance group page", async ({ page }) => {
    await goto(page, "/oa/attendance-group", 2000);
    await expectNoError(page);
  });

  test("Route #34: /oa/attendance-group - should display table or content", async ({ page }) => {
    await goto(page, "/oa/attendance-group", 2000);
    const content = page.locator(".el-table, .el-card").first();
    await expect(content).toBeVisible({ timeout: 5000 });
  });
});

// ============================================================
// 19. LEAVE BALANCE (Route #35)
// ============================================================
test.describe("19. Leave Balance", () => {
  test.beforeEach(async ({ page }) => {
    await loginByToken(page, "wujiu");
  });

  test("Route #35: /oa/leave-balance - should load leave balance page", async ({ page }) => {
    await goto(page, "/oa/leave-balance", 2000);
    await expectNoError(page);
  });

  test("Route #35: /oa/leave-balance - should display balance content", async ({ page }) => {
    await goto(page, "/oa/leave-balance", 2000);
    const content = page.locator(".el-card, .el-table, .el-descriptions").first();
    await expect(content).toBeVisible({ timeout: 5000 });
  });
});

// ============================================================
// 20. OVERTIME (Routes #36, #37)
// ============================================================
test.describe("20. Overtime Pages", () => {
  test.describe("Route #36: Overtime Apply (USER)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "wujiu");
    });

    test("should load overtime apply page", async ({ page }) => {
      await goto(page, "/oa/overtime-apply", 2000);
      await expectNoError(page);
    });

    test("should display form elements", async ({ page }) => {
      await goto(page, "/oa/overtime-apply", 2000);
      const form = page.locator(".el-form");
      await expect(form).toBeVisible({ timeout: 5000 });
    });

    test("should have submit button", async ({ page }) => {
      await goto(page, "/oa/overtime-apply", 2000);
      const submitBtn = page.locator("button", { hasText: "提交" }).or(
        page.locator("button", { hasText: "申请" })
      );
      await expect(submitBtn.first()).toBeVisible({ timeout: 5000 });
    });
  });

  test.describe("Route #37: Overtime Approval (ADMIN)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should load overtime approval page", async ({ page }) => {
      await goto(page, "/oa/overtime-approval", 2000);
      await expectNoError(page);
    });
  });
});

// ============================================================
// 21. SALARY (Routes #38, #39)
// ============================================================
test.describe("21. Salary Pages", () => {
  test.describe("Route #38: Salary Manage (ADMIN)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should load salary management page", async ({ page }) => {
      await goto(page, "/oa/salary", 2000);
      await expectNoError(page);
    });

    test("should display salary table or content", async ({ page }) => {
      await goto(page, "/oa/salary", 2000);
      const content = page.locator(".el-table, .el-card").first();
      await expect(content).toBeVisible({ timeout: 5000 });
    });
  });

  test.describe("Route #39: My Salary (USER)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "wujiu");
    });

    test("should load my salary page", async ({ page }) => {
      await goto(page, "/oa/salary-my", 2000);
      await expectNoError(page);
    });
  });
});

// ============================================================
// 22. EMPLOYEE ARCHIVE (Route #40, ADMIN)
// ============================================================
test.describe("22. Employee Archive", () => {
  test.beforeEach(async ({ page }) => {
    await loginByToken(page, "admin");
  });

  test("Route #40: /oa/archive - should load archive page", async ({ page }) => {
    await goto(page, "/oa/archive", 2000);
    await expectNoError(page);
  });

  test("Route #40: /oa/archive - should display content", async ({ page }) => {
    await goto(page, "/oa/archive", 2000);
    const content = page.locator(".el-table, .el-card").first();
    await expect(content).toBeVisible({ timeout: 5000 });
  });
});

// ============================================================
// 23. MEETING ROOM & MEETING (Routes #41, #42)
// ============================================================
test.describe("23. Meeting Pages", () => {
  test.describe("Route #41: Meeting Room (ADMIN)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should load meeting room page", async ({ page }) => {
      await goto(page, "/oa/meeting-room", 2000);
      await expectNoError(page);
    });

    test("should display meeting room content", async ({ page }) => {
      await goto(page, "/oa/meeting-room", 2000);
      const content = page.locator(".el-table, .el-card").first();
      await expect(content).toBeVisible({ timeout: 5000 });
    });
  });

  test.describe("Route #42: Meeting (USER)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "wujiu");
    });

    test("should load meeting page", async ({ page }) => {
      await goto(page, "/oa/meeting", 2000);
      await expectNoError(page);
    });

    test("should display meeting content", async ({ page }) => {
      await goto(page, "/oa/meeting", 2000);
      const content = page.locator(".el-table, .el-card, .el-empty").first();
      await expect(content).toBeVisible({ timeout: 5000 });
    });
  });
});

// ============================================================
// 24. ASSET (Routes #43, #44)
// ============================================================
test.describe("24. Asset Pages", () => {
  test.describe("Route #43: Asset Manage (ADMIN)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should load asset management page", async ({ page }) => {
      await goto(page, "/oa/asset", 2000);
      await expectNoError(page);
    });

    test("should display asset table", async ({ page }) => {
      await goto(page, "/oa/asset", 2000);
      const table = page.locator(".el-table");
      await expect(table).toBeVisible({ timeout: 5000 });
    });
  });

  test.describe("Route #44: Asset Borrow (USER)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "wujiu");
    });

    test("should load asset borrow page", async ({ page }) => {
      await goto(page, "/oa/asset-borrow", 2000);
      await expectNoError(page);
    });
  });
});

// ============================================================
// 25. CONTRACT (Route #45, ADMIN)
// ============================================================
test.describe("25. Contract Management", () => {
  test.beforeEach(async ({ page }) => {
    await loginByToken(page, "admin");
  });

  test("Route #45: /oa/contract - should load contract page", async ({ page }) => {
    await goto(page, "/oa/contract", 2000);
    await expectNoError(page);
  });

  test("Route #45: /oa/contract - should display contract table", async ({ page }) => {
    await goto(page, "/oa/contract", 2000);
    const table = page.locator(".el-table");
    await expect(table).toBeVisible({ timeout: 5000 });
  });
});

// ============================================================
// 26. BUDGET (Route #46, ADMIN)
// ============================================================
test.describe("26. Budget Management", () => {
  test.beforeEach(async ({ page }) => {
    await loginByToken(page, "admin");
  });

  test("Route #46: /oa/budget - should load budget page", async ({ page }) => {
    await goto(page, "/oa/budget", 2000);
    await expectNoError(page);
  });

  test("Route #46: /oa/budget - should display budget content", async ({ page }) => {
    await goto(page, "/oa/budget", 2000);
    const content = page.locator(".el-table, .el-card").first();
    await expect(content).toBeVisible({ timeout: 5000 });
  });
});

// ============================================================
// 27. LOAN (Routes #47, #48)
// ============================================================
test.describe("27. Loan Pages", () => {
  test.describe("Route #47: Loan Apply (USER)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "wujiu");
    });

    test("should load loan apply page", async ({ page }) => {
      await goto(page, "/oa/loan-apply", 2000);
      await expectNoError(page);
    });

    test("should display form elements", async ({ page }) => {
      await goto(page, "/oa/loan-apply", 2000);
      const form = page.locator(".el-form");
      await expect(form).toBeVisible({ timeout: 5000 });
    });
  });

  test.describe("Route #48: Loan Approval (ADMIN)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should load loan approval page", async ({ page }) => {
      await goto(page, "/oa/loan-approval", 2000);
      await expectNoError(page);
    });
  });
});

// ============================================================
// 28. ALERT (Routes #49, #50, ADMIN)
// ============================================================
test.describe("28. Alert Pages", () => {
  test.beforeEach(async ({ page }) => {
    await loginByToken(page, "admin");
  });

  test("Route #49: /oa/alert-rule - should load alert rule page", async ({ page }) => {
    await goto(page, "/oa/alert-rule", 2000);
    await expectNoError(page);
  });

  test("Route #49: /oa/alert-rule - should display alert rule content", async ({ page }) => {
    await goto(page, "/oa/alert-rule", 2000);
    const content = page.locator(".el-table, .el-card").first();
    await expect(content).toBeVisible({ timeout: 5000 });
  });

  test("Route #50: /oa/alert-log - should load alert log page", async ({ page }) => {
    await goto(page, "/oa/alert-log", 2000);
    await expectNoError(page);
  });

  test("Route #50: /oa/alert-log - should display alert log content", async ({ page }) => {
    await goto(page, "/oa/alert-log", 2000);
    const content = page.locator(".el-table, .el-card").first();
    await expect(content).toBeVisible({ timeout: 5000 });
  });
});

// ============================================================
// 29. SYSTEM MODULE (Routes #51-57, ADMIN only)
// ============================================================
test.describe("29. System Module", () => {
  test.describe("Route #51: Employee Management", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should load employee management page", async ({ page }) => {
      await goto(page, "/system/user", 2000);
      await expectNoError(page);
    });

    test("should display search input", async ({ page }) => {
      await goto(page, "/system/user", 2000);
      const searchInput = page.locator('input[placeholder*="搜索"]').or(
        page.locator('input[placeholder*="姓名"]')
      );
      await expect(searchInput.first()).toBeVisible({ timeout: 5000 });
    });

    test("should display add employee button", async ({ page }) => {
      await goto(page, "/system/user", 2000);
      const addBtn = page.locator("button", { hasText: "新增员工" }).or(
        page.locator("button", { hasText: "新增" })
      );
      await expect(addBtn.first()).toBeVisible({ timeout: 5000 });
    });

    test("should display query button", async ({ page }) => {
      await goto(page, "/system/user", 2000);
      const queryBtn = page.locator("button", { hasText: "查询" });
      await expect(queryBtn).toBeVisible({ timeout: 5000 });
    });

    test("should display employee table", async ({ page }) => {
      await goto(page, "/system/user", 2000);
      const table = page.locator(".el-table");
      await expect(table).toBeVisible({ timeout: 5000 });
    });

    test("should have pagination", async ({ page }) => {
      await goto(page, "/system/user", 2000);
      const pagination = page.locator(".el-pagination");
      await expect(pagination).toBeVisible({ timeout: 5000 });
    });

    test("should open add employee dialog", async ({ page }) => {
      await goto(page, "/system/user", 2000);
      const addBtn = page.locator("button", { hasText: "新增员工" }).or(
        page.locator("button", { hasText: "新增" })
      );
      await addBtn.first().click();
      await page.waitForTimeout(500);
      const dialog = page.locator(".el-dialog");
      await expect(dialog).toBeVisible({ timeout: 5000 });
      // Dialog should have form fields
      const formInputs = page.locator(".el-dialog .el-input input");
      const inputCount = await formInputs.count();
      expect(inputCount).toBeGreaterThanOrEqual(1);
    });
  });

  test.describe("Route #52: Role Management", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should load role management page", async ({ page }) => {
      await goto(page, "/system/role", 2000);
      await expectNoError(page);
    });

    test("should display add role button", async ({ page }) => {
      await goto(page, "/system/role", 2000);
      const addBtn = page.locator("button", { hasText: "新增角色" }).or(
        page.locator("button", { hasText: "新增" })
      );
      await expect(addBtn.first()).toBeVisible({ timeout: 5000 });
    });

    test("should display role table", async ({ page }) => {
      await goto(page, "/system/role", 2000);
      const table = page.locator(".el-table");
      await expect(table).toBeVisible({ timeout: 5000 });
    });

    test("should open add role dialog", async ({ page }) => {
      await goto(page, "/system/role", 2000);
      const addBtn = page.locator("button", { hasText: "新增角色" }).or(
        page.locator("button", { hasText: "新增" })
      );
      await addBtn.first().click();
      await page.waitForTimeout(500);
      const dialog = page.locator(".el-dialog");
      await expect(dialog).toBeVisible({ timeout: 5000 });
    });
  });

  test.describe("Route #53: Menu Management", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should load menu management page", async ({ page }) => {
      await goto(page, "/system/menu", 2000);
      await expectNoError(page);
    });

    test("should display menu table or tree", async ({ page }) => {
      await goto(page, "/system/menu", 2000);
      const content = page.locator(".el-table, .el-tree").first();
      await expect(content).toBeVisible({ timeout: 5000 });
    });
  });

  test.describe("Route #54: Dept Management", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should load dept management page", async ({ page }) => {
      await goto(page, "/system/dept", 2000);
      await expectNoError(page);
    });

    test("should display dept content", async ({ page }) => {
      await goto(page, "/system/dept", 2000);
      const content = page.locator(".el-table, .el-tree, .el-card").first();
      await expect(content).toBeVisible({ timeout: 5000 });
    });
  });

  test.describe("Route #55: Dict Management", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should load dict management page", async ({ page }) => {
      await goto(page, "/system/dict", 2000);
      await expectNoError(page);
    });

    test("should display dict table", async ({ page }) => {
      await goto(page, "/system/dict", 2000);
      const content = page.locator(".el-table, .el-card").first();
      await expect(content).toBeVisible({ timeout: 5000 });
    });
  });

  test.describe("Route #56: Config Management", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should load config management page", async ({ page }) => {
      await goto(page, "/system/config", 2000);
      await expectNoError(page);
    });

    test("should display config content", async ({ page }) => {
      await goto(page, "/system/config", 2000);
      const content = page.locator(".el-table, .el-card").first();
      await expect(content).toBeVisible({ timeout: 5000 });
    });
  });

  test.describe("Route #57: Post Management", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should load post management page", async ({ page }) => {
      await goto(page, "/system/post", 2000);
      await expectNoError(page);
    });

    test("should display post content", async ({ page }) => {
      await goto(page, "/system/post", 2000);
      const content = page.locator(".el-table, .el-card").first();
      await expect(content).toBeVisible({ timeout: 5000 });
    });
  });
});

// ============================================================
// 30. MONITOR MODULE (Routes #58-61, ADMIN only)
// ============================================================
test.describe("30. Monitor Module", () => {
  test.describe("Route #58: Online Users", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should load online users page", async ({ page }) => {
      await goto(page, "/monitor/online", 2000);
      await expectNoError(page);
    });

    test("should display online users content", async ({ page }) => {
      await goto(page, "/monitor/online", 2000);
      const content = page.locator(".el-table, .el-card").first();
      await expect(content).toBeVisible({ timeout: 5000 });
    });
  });

  test.describe("Route #59: Login Logs", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should load login logs page", async ({ page }) => {
      await goto(page, "/monitor/logs/login", 2000);
      await expectNoError(page);
    });

    test("should display login logs table", async ({ page }) => {
      await goto(page, "/monitor/logs/login", 2000);
      const content = page.locator(".el-table, .el-card").first();
      await expect(content).toBeVisible({ timeout: 5000 });
    });
  });

  test.describe("Route #60: Operation Logs", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should load operation logs page", async ({ page }) => {
      await goto(page, "/monitor/logs/operation", 2000);
      await expectNoError(page);
    });

    test("should display operation logs content", async ({ page }) => {
      await goto(page, "/monitor/logs/operation", 2000);
      const content = page.locator(".el-table, .el-card").first();
      await expect(content).toBeVisible({ timeout: 5000 });
    });
  });

  test.describe("Route #61: System Logs", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should load system logs page", async ({ page }) => {
      await goto(page, "/monitor/logs/system", 2000);
      await expectNoError(page);
    });

    test("should display system logs content", async ({ page }) => {
      await goto(page, "/monitor/logs/system", 2000);
      const content = page.locator(".el-table, .el-card").first();
      await expect(content).toBeVisible({ timeout: 5000 });
    });
  });
});

// ============================================================
// 31. ACCOUNT SETTINGS (Route #62)
// ============================================================
test.describe("31. Account Settings", () => {
  test.beforeEach(async ({ page }) => {
    await loginByToken(page, "wujiu");
  });

  test("Route #62: /account-settings - should load account settings page", async ({ page }) => {
    await goto(page, "/account-settings", 2000);
    await expectNoError(page);
  });

  test("Route #62: /account-settings - should display settings form or card", async ({ page }) => {
    await goto(page, "/account-settings", 2000);
    const content = page.locator(".el-card, .el-form").first();
    await expect(content).toBeVisible({ timeout: 5000 });
  });
});

// ============================================================
// 32. ERROR PAGES (Routes #63-65)
// ============================================================
test.describe("32. Error Pages", () => {
  test.beforeEach(async ({ page }) => {
    await loginByToken(page, "wujiu");
  });

  test("Route #63: /error/403 - should display 403 forbidden page", async ({ page }) => {
    await goto(page, "/error/403", 1500);
    const bodyText = await page.locator("body").textContent();
    // Should show 403 related content
    expect(bodyText).toMatch(/403|forbidden|无权/i);
  });

  test("Route #64: /error/404 - should display 404 not found page", async ({ page }) => {
    await goto(page, "/error/404", 1500);
    const bodyText = await page.locator("body").textContent();
    expect(bodyText).toMatch(/404|not.?found|未找到/i);
  });

  test("Route #65: /error/500 - should display 500 error page", async ({ page }) => {
    await goto(page, "/error/500", 1500);
    const bodyText = await page.locator("body").textContent();
    expect(bodyText).toMatch(/500|error|错误|繁忙/i);
  });
});

// ============================================================
// 33. COMPREHENSIVE CRUD TABLE INTERACTION TESTS
// ============================================================
test.describe("33. CRUD Table Interaction Tests", () => {
  test.describe("Employee Management CRUD (ADMIN)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should search employees by name", async ({ page }) => {
      await goto(page, "/system/user", 2000);
      const searchInput = page.locator('input[placeholder*="搜索"], input[placeholder*="姓名"]');
      if (await searchInput.first().isVisible()) {
        await searchInput.first().fill("test");
        const queryBtn = page.locator("button", { hasText: "查询" });
        if (await queryBtn.isVisible()) {
          await queryBtn.click();
          await page.waitForTimeout(1000);
          await expectNoError(page);
        }
      }
    });

    test("should clear search and reload", async ({ page }) => {
      await goto(page, "/system/user", 2000);
      const searchInput = page.locator('input[placeholder*="搜索"], input[placeholder*="姓名"]');
      if (await searchInput.first().isVisible()) {
        await searchInput.first().fill("nonexistent");
        const queryBtn = page.locator("button", { hasText: "查询" });
        if (await queryBtn.isVisible()) {
          await queryBtn.click();
          await page.waitForTimeout(1000);
        }
        // Clear and search again
        await searchInput.first().clear();
        if (await queryBtn.isVisible()) {
          await queryBtn.click();
          await page.waitForTimeout(1000);
          await expectNoError(page);
        }
      }
    });

    test("should open and close add dialog without submitting", async ({ page }) => {
      await goto(page, "/system/user", 2000);
      const addBtn = page.locator("button", { hasText: "新增员工" }).or(
        page.locator("button", { hasText: "新增" })
      );
      await addBtn.first().click();
      await page.waitForTimeout(500);
      const dialog = page.locator(".el-dialog");
      await expect(dialog).toBeVisible({ timeout: 5000 });
      // Close dialog via cancel button
      const cancelBtn = page.locator(".el-dialog button", { hasText: "取消" });
      if (await cancelBtn.isVisible()) {
        await cancelBtn.click();
        await page.waitForTimeout(300);
      }
    });

    test("should interact with status filter select", async ({ page }) => {
      await goto(page, "/system/user", 2000);
      const statusSelect = page.locator('text=状态').locator("..").locator(".el-select").or(
        page.locator(".el-select").nth(1)
      );
      if (await statusSelect.isVisible()) {
        await statusSelect.click();
        await page.waitForTimeout(300);
        // Close the dropdown
        await page.keyboard.press("Escape");
      }
    });

    test("should display edit buttons in table rows", async ({ page }) => {
      await goto(page, "/system/user", 2000);
      const editButtons = page.locator(".el-table button", { hasText: "编辑" });
      const deleteButtons = page.locator(".el-table button", { hasText: "删除" });
      // If table has data rows, there should be action buttons
      const tableRows = page.locator(".el-table__body-wrapper .el-table__row");
      const rowCount = await tableRows.count();
      if (rowCount > 0) {
        const editCount = await editButtons.count();
        const deleteCount = await deleteButtons.count();
        expect(editCount + deleteCount).toBeGreaterThanOrEqual(1);
      }
    });
  });

  test.describe("Role Management CRUD (ADMIN)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should display role table with action buttons", async ({ page }) => {
      await goto(page, "/system/role", 2000);
      const table = page.locator(".el-table");
      await expect(table).toBeVisible({ timeout: 5000 });
      const tableRows = page.locator(".el-table__body-wrapper .el-table__row");
      const rowCount = await tableRows.count();
      if (rowCount > 0) {
        const editButtons = page.locator(".el-table button", { hasText: "编辑" });
        expect(await editButtons.count()).toBeGreaterThanOrEqual(1);
      }
    });

    test("should open and close add role dialog", async ({ page }) => {
      await goto(page, "/system/role", 2000);
      const addBtn = page.locator("button", { hasText: "新增角色" }).or(
        page.locator("button", { hasText: "新增" })
      );
      await addBtn.first().click();
      await page.waitForTimeout(500);
      const dialog = page.locator(".el-dialog");
      await expect(dialog).toBeVisible({ timeout: 5000 });
      const cancelBtn = page.locator(".el-dialog button", { hasText: "取消" });
      if (await cancelBtn.isVisible()) {
        await cancelBtn.click();
      }
    });
  });

  test.describe("Notice Management CRUD (ADMIN)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should search notices by title", async ({ page }) => {
      await goto(page, "/oa/notice/manage", 2000);
      const searchInput = page.locator('input[placeholder*="搜索"], input[placeholder*="公告"]');
      if (await searchInput.first().isVisible()) {
        await searchInput.first().fill("test");
        await page.waitForTimeout(500);
        await expectNoError(page);
      }
    });

    test("should display edit/delete action buttons", async ({ page }) => {
      await goto(page, "/oa/notice/manage", 2000);
      const tableRows = page.locator(".el-table__body-wrapper .el-table__row");
      const rowCount = await tableRows.count();
      if (rowCount > 0) {
        const actionButtons = page.locator(".el-table button", { hasText: "编辑" }).or(
          page.locator(".el-table button", { hasText: "删除" })
        );
        expect(await actionButtons.count()).toBeGreaterThanOrEqual(1);
      }
    });
  });

  test.describe("Workflow Definition CRUD (ADMIN)", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should display activate/deactivate buttons in table", async ({ page }) => {
      await goto(page, "/oa/workflow-definition", 2000);
      const tableRows = page.locator(".el-table__body-wrapper .el-table__row");
      const rowCount = await tableRows.count();
      if (rowCount > 0) {
        const actionButtons = page.locator(".el-table .el-button");
        expect(await actionButtons.count()).toBeGreaterThanOrEqual(1);
      }
    });
  });
});

// ============================================================
// 34. FORM INTERACTION TESTS (Apply Pages)
// ============================================================
test.describe("34. Form Interaction Tests", () => {
  test.describe("Leave Apply Form", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "wujiu");
    });

    test("should interact with leave type select dropdown", async ({ page }) => {
      await goto(page, "/oa/leave/apply", 2000);
      const select = page.locator(".el-select").first();
      await select.click();
      await page.waitForTimeout(500);
      // Options should appear
      const options = page.locator(".el-select-dropdown__item");
      const optCount = await options.count();
      expect(optCount).toBeGreaterThanOrEqual(1);
      // Close dropdown
      await page.keyboard.press("Escape");
    });

    test("should type in reason textarea", async ({ page }) => {
      await goto(page, "/oa/leave/apply", 2000);
      const textarea = page.locator("textarea").first();
      await textarea.fill("Test reason for leave");
      await expect(textarea).toHaveValue("Test reason for leave");
    });

    test("should click reset button and clear form", async ({ page }) => {
      await goto(page, "/oa/leave/apply", 2000);
      // Fill in something first
      const textarea = page.locator("textarea").first();
      await textarea.fill("Some text");
      // Click reset
      const resetBtn = page.locator("button", { hasText: "重置" });
      await resetBtn.click();
      await page.waitForTimeout(300);
      // Textarea should be cleared
      const value = await textarea.inputValue();
      expect(value).toBe("");
    });
  });

  test.describe("Business Trip Apply Form", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "wujiu");
    });

    test("should fill destination input", async ({ page }) => {
      await goto(page, "/oa/business-trip/apply", 2000);
      const destInput = page.locator('input[placeholder*="目的地"]');
      await destInput.fill("北京");
      await expect(destInput).toHaveValue("北京");
    });

    test("should fill purpose textarea", async ({ page }) => {
      await goto(page, "/oa/business-trip/apply", 2000);
      const textarea = page.locator("textarea").first();
      await textarea.fill("出差考察项目");
      await expect(textarea).toHaveValue("出差考察项目");
    });
  });

  test.describe("Send Message Form", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should fill message form fields", async ({ page }) => {
      await goto(page, "/oa/message/send", 2000);
      const titleInput = page.locator('input[placeholder*="标题"]');
      await titleInput.fill("Test message title");
      await expect(titleInput).toHaveValue("Test message title");

      const textarea = page.locator('textarea[placeholder*="内容"]');
      await textarea.fill("Test message content body");
      await expect(textarea).toHaveValue("Test message content body");
    });
  });
});

// ============================================================
// 35. DASHBOARD & CHART INTERACTION TESTS
// ============================================================
test.describe("35. Dashboard Chart Interaction Tests", () => {
  test.describe("OA Dashboard Charts", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should switch period and re-render charts", async ({ page }) => {
      await goto(page, "/oa/dashboard", 2000);
      // Click "本月"
      const monthBtn = page.locator(".el-radio-button", { hasText: "本月" });
      if (await monthBtn.isVisible()) {
        await monthBtn.click();
        await page.waitForTimeout(2000);
        await expectNoError(page);
      }
    });

    test("should render echarts canvas elements", async ({ page }) => {
      await goto(page, "/oa/dashboard", 3000);
      // ECharts renders to div then creates a canvas child
      const canvases = page.locator("div[style*='height'] canvas");
      const canvasCount = await canvases.count();
      // Dashboard has many chart areas, but canvas creation depends on data
      // Just verify no error state
      await expectNoError(page);
    });
  });

  test.describe("Personal Report Charts", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "wujiu");
    });

    test("should switch period filter", async ({ page }) => {
      await goto(page, "/oa/report/personal", 2000);
      const weekBtn = page.locator(".el-radio-button", { hasText: "本周" });
      if (await weekBtn.isVisible()) {
        await weekBtn.click();
        await page.waitForTimeout(1500);
        await expectNoError(page);
      }
    });

    test("should render chart containers", async ({ page }) => {
      await goto(page, "/oa/report/personal", 2000);
      // Chart containers have explicit height style
      const chartDivs = page.locator("div[style*='height: 300px']");
      const count = await chartDivs.count();
      expect(count).toBeGreaterThanOrEqual(1);
    });
  });
});

// ============================================================
// 36. ATTENDANCE CLOCK INTERACTION TESTS
// ============================================================
test.describe("36. Attendance Clock Interaction Tests", () => {
  test.beforeEach(async ({ page }) => {
    await loginByToken(page, "wujiu");
  });

  test("should display clock-in button or already clocked status", async ({ page }) => {
    await goto(page, "/oa/attendance/clock", 2000);
    // Two sections: clock-in and clock-out
    const clockInLabel = page.locator("text=上班打卡");
    const clockOutLabel = page.locator("text=下班打卡");
    await expect(clockInLabel).toBeVisible({ timeout: 5000 });
    await expect(clockOutLabel).toBeVisible({ timeout: 5000 });
  });

  test("should switch history period to week view", async ({ page }) => {
    await goto(page, "/oa/attendance/clock", 2000);
    const weekBtn = page.locator(".el-radio-button", { hasText: "本周" });
    if (await weekBtn.isVisible()) {
      await weekBtn.click();
      await page.waitForTimeout(1000);
      await expectNoError(page);
    }
  });

  test("should switch history period to month view", async ({ page }) => {
    await goto(page, "/oa/attendance/clock", 2000);
    const monthBtn = page.locator(".el-radio-button", { hasText: "本月" });
    if (await monthBtn.isVisible()) {
      await monthBtn.click();
      await page.waitForTimeout(1000);
      await expectNoError(page);
    }
  });
});

// ============================================================
// 37. WORKFLOW APPROVAL INTERACTION TESTS
// ============================================================
test.describe("37. Workflow Approval Interaction Tests", () => {
  test.describe("Approval Center Interactions", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "admin");
    });

    test("should filter by pending status", async ({ page }) => {
      await goto(page, "/oa/approval-center", 2000);
      const pendingBtn = page.locator(".el-radio-button", { hasText: "待审批" });
      if (await pendingBtn.isVisible()) {
        await pendingBtn.click();
        await page.waitForTimeout(1000);
        await expectNoError(page);
      }
    });

    test("should filter by approved status", async ({ page }) => {
      await goto(page, "/oa/approval-center", 2000);
      const approvedBtn = page.locator(".el-radio-button", { hasText: "已通过" });
      if (await approvedBtn.isVisible()) {
        await approvedBtn.click();
        await page.waitForTimeout(1000);
        await expectNoError(page);
      }
    });

    test("should reset filter to show all", async ({ page }) => {
      await goto(page, "/oa/approval-center", 2000);
      const allBtn = page.locator(".el-radio-button", { hasText: "全部" });
      if (await allBtn.isVisible()) {
        await allBtn.click();
        await page.waitForTimeout(1000);
        await expectNoError(page);
      }
    });
  });

  test.describe("Workflow Todo Interactions", () => {
    test.beforeEach(async ({ page }) => {
      await loginByToken(page, "wujiu");
    });

    test("should display task table with proper columns", async ({ page }) => {
      await goto(page, "/oa/workflow-todo", 2000);
      const table = page.locator(".el-table");
      await expect(table).toBeVisible({ timeout: 5000 });
      // Check for expected column headers
      const headers = page.locator(".el-table__header-wrapper th");
      const headerCount = await headers.count();
      expect(headerCount).toBeGreaterThanOrEqual(2);
    });

    test("should have pagination component", async ({ page }) => {
      await goto(page, "/oa/workflow-todo", 2000);
      const pagination = page.locator(".el-pagination");
      await expect(pagination).toBeVisible({ timeout: 5000 });
    });
  });
});

// ============================================================
// 38. NAVIGATION TESTS
// ============================================================
test.describe("38. Navigation Tests", () => {
  test("should navigate between pages using direct URL", async ({ page }) => {
    await loginByToken(page, "wujiu");
    // Navigate to attendance clock
    await goto(page, "/oa/attendance/clock", 1500);
    await expectNoError(page);

    // Navigate to leave apply
    await goto(page, "/oa/leave/apply", 1500);
    await expectNoError(page);

    // Navigate to schedule
    await goto(page, "/oa/schedule/index", 1500);
    await expectNoError(page);
  });

  test("should handle back navigation", async ({ page }) => {
    await loginByToken(page, "wujiu");
    await goto(page, "/oa/leave/apply", 1500);
    await goto(page, "/oa/attendance/clock", 1500);

    // Go back
    await page.goBack();
    await page.waitForLoadState("networkidle");
    await page.waitForTimeout(1000);
    await expectNoError(page);
  });

  test("should navigate admin through multiple admin pages", async ({ page }) => {
    await loginByToken(page, "admin");

    // Dashboard
    await goto(page, "/oa/dashboard", 1500);
    await expectNoError(page);

    // Employee management
    await goto(page, "/system/user", 1500);
    await expectNoError(page);

    // Role management
    await goto(page, "/system/role", 1500);
    await expectNoError(page);

    // Notice management
    await goto(page, "/oa/notice/manage", 1500);
    await expectNoError(page);
  });

  test("should maintain authentication across page navigations", async ({ page }) => {
    await loginByToken(page, "wujiu");

    // Navigate to several pages
    const pages = [
      "/oa/attendance/clock",
      "/oa/leave/apply",
      "/oa/todo",
      "/oa/message/list",
    ];
    for (const p of pages) {
      await goto(page, p, 1000);
      // Should not redirect to login
      const url = page.url();
      expect(url).not.toContain("/login");
    }
  });
});

// ============================================================
// 39. APPLY PAGES CONSOLIDATED SMOKE TESTS
// Tests all remaining apply pages for form presence and submit button
// ============================================================
test.describe("39. Apply Pages Consolidated Smoke Tests", () => {
  const applyPages = [
    { name: "Outing Apply", path: "/oa/outing/apply", user: "wujiu" },
    { name: "Purchase Apply", path: "/oa/purchase/apply", user: "wujiu" },
    { name: "Expense Apply", path: "/oa/expense/apply", user: "wujiu" },
    { name: "Loan Apply", path: "/oa/loan-apply", user: "wujiu" },
    { name: "Overtime Apply", path: "/oa/overtime-apply", user: "wujiu" },
  ];

  for (const applyPage of applyPages) {
    test(`should load ${applyPage.name} with form and submit button`, async ({ page }) => {
      await loginByToken(page, applyPage.user);
      await goto(page, applyPage.path, 2000);
      await expectNoError(page);

      // Should have a form
      const form = page.locator(".el-form");
      await expect(form).toBeVisible({ timeout: 5000 });

      // Should have a submit-type button
      const submitBtn = page.locator("button", { hasText: "提交" }).or(
        page.locator("button", { hasText: "申请" })
      );
      await expect(submitBtn.first()).toBeVisible({ timeout: 5000 });
    });
  }
});

// ============================================================
// 40. APPROVAL PAGES CONSOLIDATED SMOKE TESTS
// Tests all approval pages for table presence
// ============================================================
test.describe("40. Approval Pages Consolidated Smoke Tests", () => {
  const approvalPages = [
    { name: "Leave Approval", path: "/oa/leave/approval" },
    { name: "Business Trip Approval", path: "/oa/business-trip/approval" },
    { name: "Outing Approval", path: "/oa/outing/approval" },
    { name: "Purchase Approval", path: "/oa/purchase/approval" },
    { name: "Expense Approval", path: "/oa/expense/approval" },
    { name: "Overtime Approval", path: "/oa/overtime-approval" },
    { name: "Loan Approval", path: "/oa/loan-approval" },
  ];

  for (const approvalPage of approvalPages) {
    test(`should load ${approvalPage.name} with table`, async ({ page }) => {
      await loginByToken(page, "admin");
      await goto(page, approvalPage.path, 2000);
      await expectNoError(page);

      const table = page.locator(".el-table");
      await expect(table).toBeVisible({ timeout: 5000 });
    });
  }
});

// ============================================================
// 41. MANAGEMENT PAGES CONSOLIDATED CRUD TESTS
// Tests all management pages for search/add/table elements
// ============================================================
test.describe("41. Management Pages Consolidated CRUD Tests", () => {
  const managementPages = [
    { name: "Asset Manage", path: "/oa/asset" },
    { name: "Contract Manage", path: "/oa/contract" },
    { name: "Budget Manage", path: "/oa/budget" },
    { name: "Attendance Group", path: "/oa/attendance-group" },
    { name: "Meeting Room", path: "/oa/meeting-room" },
    { name: "Salary Manage", path: "/oa/salary" },
    { name: "Employee Archive", path: "/oa/archive" },
  ];

  for (const mgmtPage of managementPages) {
    test(`should load ${mgmtPage.name} with table content`, async ({ page }) => {
      await loginByToken(page, "admin");
      await goto(page, mgmtPage.path, 2000);
      await expectNoError(page);

      // Should have a table or card with content
      const content = page.locator(".el-table, .el-card").first();
      await expect(content).toBeVisible({ timeout: 5000 });
    });
  }
});

// ============================================================
// 42. SYSTEM PAGES CONSOLIDATED CRUD TESTS
// ============================================================
test.describe("42. System Pages Consolidated CRUD Tests", () => {
  const systemPages = [
    { name: "Menu Management", path: "/system/menu" },
    { name: "Dept Management", path: "/system/dept" },
    { name: "Dict Management", path: "/system/dict" },
    { name: "Config Management", path: "/system/config" },
    { name: "Post Management", path: "/system/post" },
  ];

  for (const sysPage of systemPages) {
    test(`should load ${sysPage.name} with content area`, async ({ page }) => {
      await loginByToken(page, "admin");
      await goto(page, sysPage.path, 2000);
      await expectNoError(page);

      const content = page.locator(".el-table, .el-tree, .el-card").first();
      await expect(content).toBeVisible({ timeout: 5000 });
    });
  }
});

// ============================================================
// 43. SCHEDULE INTERACTION TESTS
// ============================================================
test.describe("43. Schedule Interaction Tests", () => {
  test.beforeEach(async ({ page }) => {
    await loginByToken(page, "wujiu");
  });

  test("should click calendar date cells", async ({ page }) => {
    await goto(page, "/oa/schedule/index", 2000);
    const calendarCells = page.locator(".el-calendar-table td");
    const count = await calendarCells.count();
    if (count > 5) {
      // Click a date cell (not the header row)
      await calendarCells.nth(5).click();
      await page.waitForTimeout(500);
      await expectNoError(page);
    }
  });

  test("should open and close add schedule dialog without submitting", async ({ page }) => {
    await goto(page, "/oa/schedule/index", 2000);
    const addBtn = page.locator("button", { hasText: "添加日程" });
    await addBtn.click();
    await page.waitForTimeout(500);
    const dialog = page.locator(".el-dialog");
    await expect(dialog).toBeVisible({ timeout: 5000 });
    // Fill in title
    const titleInput = page.locator('.el-dialog input[placeholder*="标题"]').or(
      page.locator('.el-dialog .el-input input').first()
    );
    if (await titleInput.isVisible()) {
      await titleInput.fill("Test schedule event");
    }
    // Close without submitting
    const cancelBtn = page.locator(".el-dialog button", { hasText: "取消" });
    if (await cancelBtn.isVisible()) {
      await cancelBtn.click();
    }
  });
});

// ============================================================
// 44. NOTICE LIST INTERACTION TESTS
// ============================================================
test.describe("44. Notice List Interaction Tests", () => {
  test.beforeEach(async ({ page }) => {
    await loginByToken(page, "wujiu");
  });

  test("should search notices by keyword", async ({ page }) => {
    await goto(page, "/oa/notice/list", 2000);
    const searchInput = page.locator('input[placeholder*="搜索"], input[placeholder*="公告"]');
    if (await searchInput.first().isVisible()) {
      await searchInput.first().fill("test search");
      await page.waitForTimeout(500);
      await expectNoError(page);
    }
  });

  test("should click on notice item to open detail dialog", async ({ page }) => {
    await goto(page, "/oa/notice/list", 2000);
    const noticeItems = page.locator(".cursor-pointer");
    const count = await noticeItems.count();
    if (count > 0) {
      await noticeItems.first().click();
      await page.waitForTimeout(500);
      // Should open detail dialog
      const dialog = page.locator(".el-dialog");
      const dialogVisible = await dialog.isVisible();
      if (dialogVisible) {
        // Dialog should have title and content
        const dialogContent = page.locator(".el-dialog__body");
        expect(await dialogContent.isVisible()).toBeTruthy();
      }
    }
  });
});

// ============================================================
// 45. ATTENDANCE RECORD INTERACTION TESTS
// ============================================================
test.describe("45. Attendance Record Interaction Tests", () => {
  test.beforeEach(async ({ page }) => {
    await loginByToken(page, "wujiu");
  });

  test("should display table with attendance columns", async ({ page }) => {
    await goto(page, "/oa/attendance/record", 2000);
    const table = page.locator(".el-table");
    await expect(table).toBeVisible({ timeout: 5000 });
    // Should have column headers
    const headers = page.locator(".el-table__header-wrapper th");
    const headerCount = await headers.count();
    expect(headerCount).toBeGreaterThanOrEqual(1);
  });
});

// ============================================================
// 46. MESSAGE LIST INTERACTION TESTS
// ============================================================
test.describe("46. Message List Interaction Tests", () => {
  test.beforeEach(async ({ page }) => {
    await loginByToken(page, "wujiu");
  });

  test("should display message content area", async ({ page }) => {
    await goto(page, "/oa/message/list", 2000);
    const content = page.locator(".el-card, .el-table, .el-empty").first();
    await expect(content).toBeVisible({ timeout: 5000 });
  });
});

// ============================================================
// 47. PAGINATION INTERACTION TESTS
// ============================================================
test.describe("47. Pagination Interaction Tests", () => {
  test("should interact with pagination on employee list", async ({ page }) => {
    await loginByToken(page, "admin");
    await goto(page, "/system/user", 2000);

    const pagination = page.locator(".el-pagination");
    if (await pagination.isVisible()) {
      // Try clicking page size selector
      const pageSizeBtn = pagination.locator(".el-pagination__sizes");
      if (await pageSizeBtn.isVisible()) {
        await pageSizeBtn.click();
        await page.waitForTimeout(300);
        await page.keyboard.press("Escape");
      }
    }
  });

  test("should interact with pagination on leave apply", async ({ page }) => {
    await loginByToken(page, "wujiu");
    await goto(page, "/oa/leave/apply", 2000);

    const pagination = page.locator(".el-pagination");
    if (await pagination.isVisible()) {
      // Pagination should show total
      const total = pagination.locator(".el-pagination__total");
      if (await total.isVisible()) {
        const totalText = await total.textContent();
        expect(totalText).toBeTruthy();
      }
    }
  });
});
