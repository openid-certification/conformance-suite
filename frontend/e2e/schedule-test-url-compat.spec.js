import { test, expect } from "@playwright/test";

/**
 * Legacy `?edit-plan=<id>` URLs in the wild (bookmarks, GitLab issues,
 * certification submissions) must deep-link to plan-detail.html, not
 * land on the bare schedule-test list.
 *
 * The redirect runs in a synchronous inline <script> in <head> before
 * any module or API call, so these tests never need to mock /api/*.
 */
test.describe("schedule-test.html — legacy ?edit-plan= URL compat", () => {
  test("redirects ?edit-plan=<id> to plan-detail.html?plan=<id>", async ({ page }) => {
    await page.goto("/schedule-test.html?edit-plan=2EqLVMNo51cqh", {
      waitUntil: "commit",
    });
    await page.waitForURL("**/plan-detail.html?plan=2EqLVMNo51cqh");
    expect(new URL(page.url()).pathname).toBe("/plan-detail.html");
    expect(new URL(page.url()).searchParams.get("plan")).toBe("2EqLVMNo51cqh");
  });

  test("no redirect when ?edit-plan= is absent", async ({ page }) => {
    const responses = [];
    page.on("response", (r) => responses.push(r.url()));

    await page.goto("/schedule-test.html");
    expect(new URL(page.url()).pathname).toBe("/schedule-test.html");
  });

  test("strips other query params on redirect (keeps only plan=)", async ({ page }) => {
    await page.goto("/schedule-test.html?edit-plan=ABC&other=foo", {
      waitUntil: "commit",
    });
    await page.waitForURL("**/plan-detail.html?plan=ABC");
    const target = new URL(page.url());
    expect(target.searchParams.get("plan")).toBe("ABC");
    expect(target.searchParams.get("other")).toBeNull();
  });

  test("encodes URL-unsafe characters in the plan id", async ({ page }) => {
    await page.goto("/schedule-test.html?edit-plan=plan%20with%20spaces", {
      waitUntil: "commit",
    });
    await page.waitForURL("**/plan-detail.html?plan=*");
    expect(new URL(page.url()).searchParams.get("plan")).toBe("plan with spaces");
    expect(page.url()).toContain("plan-detail.html?plan=plan%20with%20spaces");
  });

  test("no redirect when ?edit-plan is present but empty", async ({ page }) => {
    await page.goto("/schedule-test.html?edit-plan=");
    expect(new URL(page.url()).pathname).toBe("/schedule-test.html");
  });
});
