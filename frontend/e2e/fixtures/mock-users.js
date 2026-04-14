/** Mock /api/currentuser response shapes. */

export const MOCK_USER = {
  iss: "https://accounts.google.com",
  sub: "12345",
  principal: "testuser@example.com",
  displayName: "Test User",
  isAdmin: false,
  isGuest: false,
};

export const MOCK_ADMIN_USER = {
  ...MOCK_USER,
  sub: "admin-001",
  principal: "admin@openid.net",
  displayName: "Admin User",
  isAdmin: true,
};
