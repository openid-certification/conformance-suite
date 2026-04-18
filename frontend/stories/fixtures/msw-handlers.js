/**
 * Shared MSW v2 request handlers for Storybook stories.
 *
 * Each handler set provides a "happy path default" response.
 * Stories override individual handlers for error/edge cases via
 * parameters.msw.handlers.
 *
 * Usage in stories:
 *   import { authHandlers, planHandlers } from "@fixtures/msw-handlers.js";
 *   parameters: { msw: { handlers: [...authHandlers, ...planHandlers] } }
 */

import { http, HttpResponse } from "msw";
import { MOCK_USER, MOCK_ADMIN_USER } from "./mock-users.js";
import { MOCK_TOKENS, MOCK_CREATED_TOKEN } from "./mock-tokens.js";
import { MOCK_PLANS, MOCK_PLAN_LIST } from "./mock-plans.js";
import { MOCK_LOG_ENTRIES } from "./mock-log-entries.js";
import {
  MOCK_PLAN_DETAIL,
  MOCK_TEST_STATUS,
  MOCK_RUNNING_TESTS,
  MOCK_SERVER_INFO,
} from "./mock-test-data.js";

// --- Auth handlers ---

export const authHandlers = [http.get("/api/currentuser", () => HttpResponse.json(MOCK_USER))];

export const adminAuthHandlers = [
  http.get("/api/currentuser", () => HttpResponse.json(MOCK_ADMIN_USER)),
];

export const unauthenticatedHandlers = [
  http.get("/api/currentuser", () => new HttpResponse(null, { status: 401 })),
];

// --- Server info handlers ---

export const serverHandlers = [http.get("/api/server", () => HttpResponse.json(MOCK_SERVER_INFO))];

// --- Plan handlers ---

export const planHandlers = [
  // Available plans for scheduling (spec cascade)
  http.get("/api/runner/available", () => HttpResponse.json(MOCK_PLANS)),

  // Plan list (supports DataTables-style params: draw, start, length)
  http.get("/api/plan", ({ request }) => {
    const url = new URL(request.url);
    const isPublic = url.searchParams.get("public") === "true";

    // Return the full list (client-side filtering in the component)
    const plans = isPublic ? MOCK_PLAN_LIST.filter((p) => p.publish) : MOCK_PLAN_LIST;
    return HttpResponse.json(plans);
  }),

  // Plan detail
  http.get("/api/plan/:planId", () => HttpResponse.json(MOCK_PLAN_DETAIL)),

  // Publish plan
  http.post("/api/plan/:planId/publish", () => new HttpResponse(null, { status: 200 })),

  // Share plan (private link)
  http.post("/api/plan/:planId/share", async ({ request }) => {
    const body = /** @type {{ days?: number } | null} */ (await request.json());
    const days = (body && body.days) || 30;
    return HttpResponse.json({
      url: `https://localhost.emobix.co.uk:8443/plan-detail.html?plan=plan-abc-123&token=mock-share-token-${days}d`,
    });
  }),

  // Make plan mutable
  http.post("/api/plan/:planId/makemutable", () => new HttpResponse(null, { status: 200 })),

  // Delete plan
  http.delete("/api/plan/:planId", () => new HttpResponse(null, { status: 200 })),

  // Certification package
  http.post("/api/plan/:planId/certificationpackage", () =>
    HttpResponse.json({ downloadUrl: "/api/plan/plan-abc-123/certificationpackage/download" }),
  ),
];

// --- Log handlers ---

export const logHandlers = [
  // Log entries (supports ?since= for incremental polling)
  http.get("/api/log/:testId", ({ request }) => {
    const url = new URL(request.url);
    const since = url.searchParams.get("since");
    if (since && Number(since) > 0) {
      // Subsequent poll: return empty (no new entries)
      return HttpResponse.json([]);
    }
    return HttpResponse.json(MOCK_LOG_ENTRIES);
  }),

  // Test info/status
  http.get("/api/info/:testId", () => HttpResponse.json(MOCK_TEST_STATUS)),

  // Image upload
  http.post("/api/log/:testId/uploadimage", () => new HttpResponse(null, { status: 200 })),
];

// --- Runner handlers ---

export const runnerHandlers = [
  // Start a test
  http.post("/api/runner", () =>
    HttpResponse.json({
      name: "oidcc-server",
      id: "test-new-001",
      url: "https://localhost.emobix.co.uk:8443/log-detail.html?log=test-new-001",
    }),
  ),

  // Running tests list
  http.get("/api/runner/running", () => HttpResponse.json(MOCK_RUNNING_TESTS)),

  // Running test detail
  http.get("/api/runner/:testId", () => HttpResponse.json(MOCK_RUNNING_TESTS[0])),
];

// --- Token handlers ---

export const tokenHandlers = [
  // Token list
  http.get("/api/token", () => HttpResponse.json(MOCK_TOKENS)),

  // Create token
  http.post("/api/token", () => HttpResponse.json(MOCK_CREATED_TOKEN)),

  // Delete token
  http.delete("/api/token/:tokenId", () => new HttpResponse(null, { status: 200 })),
];

// --- Convenience: all handlers combined ---

export const allHandlers = [
  ...authHandlers,
  ...serverHandlers,
  ...planHandlers,
  ...logHandlers,
  ...runnerHandlers,
  ...tokenHandlers,
];
