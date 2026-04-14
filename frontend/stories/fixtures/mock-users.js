/**
 * Mock user data matching the /api/currentuser response shape.
 * Fields: iss, sub, principal, displayName, isAdmin, isGuest.
 */

export const MOCK_USER = {
  iss: "https://accounts.google.com",
  sub: "12345",
  principal: "testuser@example.com",
  displayName: "Test User",
  isAdmin: false,
  isGuest: false,
};

export const MOCK_ADMIN_USER = {
  iss: "https://accounts.google.com",
  sub: "admin-001",
  principal: "admin@openid.net",
  displayName: "Admin User",
  isAdmin: true,
  isGuest: false,
};

export const MOCK_GUEST_USER = {
  iss: "https://accounts.google.com",
  sub: "guest-001",
  principal: "guest@example.com",
  displayName: "Guest User",
  isAdmin: false,
  isGuest: true,
};
