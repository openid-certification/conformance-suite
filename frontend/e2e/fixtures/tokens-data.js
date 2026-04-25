/** Mock data for tokens.html E2E tests. */

/**
 * A non-admin, non-guest user so cts-token-manager renders the action
 * buttons and the tokens table (the `is-admin` branch hides both and
 * shows a read-only message instead).
 */
export const MOCK_TOKEN_USER = {
  iss: "https://accounts.google.com",
  sub: "token-user-001",
  principal: "tokenuser@example.com",
  displayName: "Token User",
  isAdmin: false,
  isGuest: false,
};

/**
 * Sample tokens returned by GET /api/token. The endpoint returns a plain
 * array — cts-token-manager fetches the array directly via `fetch()` and
 * iterates it in the Lit template.
 */
export const MOCK_TOKENS = [
  {
    _id: "token-abc-001",
    expires: "2027-01-01T00:00:00.000Z",
  },
  {
    _id: "token-xyz-002",
    expires: null,
  },
];
