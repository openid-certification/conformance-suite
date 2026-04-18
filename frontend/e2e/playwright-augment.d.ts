// Module augmentation for @playwright/test used by the fail-fast helper.
// `setupFailFast` stashes a per-page array on the Playwright Page so
// `expectNoUnmockedCalls` can surface unmocked-route failures loudly — raw
// `route.abort("failed")` can be swallowed by application fetch-error UI,
// so the test records call URLs and throws at the end.

import "@playwright/test";

declare module "@playwright/test" {
  interface Page {
    __unmockedApiCalls?: string[];
  }
}
