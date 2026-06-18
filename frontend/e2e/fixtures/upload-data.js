/** Mock data for upload.html E2E tests. */

/** GET /api/log/:testId/images — list of image entries.
 * Each item is either:
 *   - a "pending" entry (has `upload` id, requires screenshot upload)
 *   - an "existing" entry (has `img` data URL / path)
 */
export const MOCK_IMAGES_PENDING = [
  {
    _id: "img-pending-001",
    upload: "placeholder-1",
    src: "AddScreenshotPlaceholder",
    msg: "Upload a screenshot showing the browser when the authorization endpoint was reached.",
  },
];

export const MOCK_IMAGES_EMPTY = [];

/** GET /api/info/:testId response for the upload page header. */
export const MOCK_UPLOAD_TEST_INFO = {
  _id: "test-upload-001",
  testId: "test-upload-001",
  testName: "oidcc-server",
  status: "RUNNING",
  result: null,
  summary: "Upload screenshots for this test run",
  owner: { sub: "12345", iss: "https://accounts.google.com" },
};

/** /api/info/ variant whose summary carries a ~100-char unbroken
 * base64-style token (mirroring real-world subject ids in AuthZEN
 * payload summaries) — exercises the 375px overflow-wrap regression
 * test. The token must contain no hyphens/slashes: those are natural
 * break opportunities and would let the test pass without
 * `overflow-wrap: anywhere`. */
export const MOCK_UPLOAD_TEST_INFO_LONG_TOKEN = {
  ...MOCK_UPLOAD_TEST_INFO,
  summary:
    'Evaluation API test 01 with payload { "subject": { "id": ' +
    '"CiRmWkRBMk1UUmtNeTFqTTJsaExUUTNPREV0WWpkaVpDMDVZakV3WmpRMFpqUTBaalEwWlRBaWZRWkRBMk1UUmtNeTFqTTJsaEx" } }',
};
