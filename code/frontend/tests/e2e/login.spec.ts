import { expect, test } from "@playwright/test";

test("login page renders the primary form controls", async ({ page }) => {
  await page.goto("/#/login");

  await expect(page.locator(".login-container")).toBeVisible();
  await expect(page.locator("input").first()).toBeVisible();
  await expect(page.locator("input").nth(1)).toBeVisible();
  await expect(page.locator("button.btn-primary")).toBeVisible();
});
