/**
 * Mock token data matching the /api/token response shape.
 * GET /api/token returns an array of { _id, expires } objects.
 * POST /api/token returns { token: "generated-value" }.
 */

const DAY_MS = 86400000;

export const MOCK_TOKENS = [
  {
    _id: "token-abc-123-def-456",
    expires: Date.now() + 30 * DAY_MS,
  },
  {
    _id: "token-ghi-789-jkl-012",
    expires: Date.now() + 7 * DAY_MS,
  },
  {
    _id: "token-permanent-001",
    expires: null,
  },
];

export const MOCK_CREATED_TOKEN = {
  token: "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0dXNlckBleGFtcGxlLmNvbSJ9.mock-signature-value",
};
