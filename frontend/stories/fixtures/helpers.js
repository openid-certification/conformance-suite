/**
 * Creates a Storybook decorator that intercepts fetch calls matching a URL
 * pattern and returns a mock response. Restores real fetch after the story
 * renders (via queueMicrotask).
 *
 * Follows the pattern established by cts-navbar.stories.js.
 */
export function withMockFetch(
  urlPattern,
  mockResponse,
  { delay = 0, status = 200 } = {},
) {
  return (storyFn) => {
    const realFetch = window.fetch;
    window.fetch = (url, opts) => {
      if (typeof url === "string" && url.includes(urlPattern)) {
        return new Promise((resolve) =>
          setTimeout(
            () =>
              resolve(
                new Response(JSON.stringify(mockResponse), {
                  status,
                  headers: { "Content-Type": "application/json" },
                }),
              ),
            delay,
          ),
        );
      }
      return realFetch(url, opts);
    };

    const result = storyFn();

    queueMicrotask(() => {
      window.fetch = realFetch;
    });

    return result;
  };
}

/**
 * Wait for a Lit component to finish its update cycle.
 * Call after the component is in the DOM and you need to assert on rendered output.
 */
export async function waitForLitRender(element) {
  if (element.updateComplete) {
    await element.updateComplete;
  }
  // Extra frame for DOM to settle
  await new Promise((r) => requestAnimationFrame(r));
}
