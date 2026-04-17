/** Mock data for tokens.html E2E tests. */

/**
 * A non-admin, non-guest user so tokenTable.html renders the action buttons
 * and the tokens DataTable (the admin branch hides both).
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
 * array, not a DataTables server-side envelope — the DataTable uses
 * `dataSrc: ''` to consume it directly.
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
