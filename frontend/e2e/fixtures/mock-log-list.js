/** Mock /api/log list response shape (for logs.html DataTable). */

const NOW = Date.now();
const DAY_MS = 86400000;

export const MOCK_LOG_LIST = [
  {
    testId: "test-log-001",
    testName: "oidcc-server",
    variant: { client_auth_type: "client_secret_basic", response_type: "code" },
    description: "Tests basic OpenID Connect server functionality",
    started: new Date(NOW - DAY_MS).toISOString(),
    planId: "plan-001",
    status: "FINISHED",
    result: "PASSED",
    owner: { sub: "12345", iss: "https://accounts.google.com" },
  },
  {
    testId: "test-log-002",
    testName: "oidcc-server-rotate-keys",
    variant: { client_auth_type: "client_secret_basic", response_type: "code" },
    description: "Tests key rotation behavior",
    started: new Date(NOW - DAY_MS / 2).toISOString(),
    planId: "plan-001",
    status: "FINISHED",
    result: "WARNING",
    owner: { sub: "12345", iss: "https://accounts.google.com" },
  },
];
