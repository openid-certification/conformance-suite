/**
 * Uncaught page errors as a test failure. A crash inside the page's own JS
 * would otherwise hide behind a locator timeout.
 *
 * @module helpers/page-errors
 */

/**
 * Record uncaught page errors for {@link expectNoPageErrors} to check.
 * @param {import('@playwright/test').Page & { __pageErrors?: Error[] }} page
 */
export function watchPageErrors(page) {
  /** @type {Error[]} */
  const pageErrors = [];
  page.__pageErrors = pageErrors;
  page.on("pageerror", (err) => pageErrors.push(err));
}

/**
 * Fail the test if {@link watchPageErrors} recorded anything.
 * @param {import('@playwright/test').Page & { __pageErrors?: Error[] }} page
 */
export function expectNoPageErrors(page) {
  if (!page.__pageErrors) {
    throw new Error("watchPageErrors() was not called — page errors would not be detected");
  }
  if (page.__pageErrors.length > 0) {
    throw new Error(
      `Unexpected page errors:\n  ${page.__pageErrors.map((e) => e.message).join("\n  ")}`,
    );
  }
}
