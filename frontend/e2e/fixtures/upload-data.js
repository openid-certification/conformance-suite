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
