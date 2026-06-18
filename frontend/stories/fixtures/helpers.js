/**
 * Creates a Storybook decorator that intercepts fetch calls matching a URL
 * pattern and returns a mock response. Restores real fetch after the story
 * renders (via queueMicrotask) unless `persistent: true` is set.
 *
 * Use `persistent: true` for components that poll (e.g. cts-log-viewer) so the
 * mock survives across multiple fetch calls. The play function is responsible
 * for restoring real fetch when done (store the return value of getRealFetch()
 * before installing, or read window.fetch.__realFetch).
 *
 * Follows the pattern established by cts-navbar.stories.js.
 */
export function withMockFetch(
  urlPattern,
  mockResponse,
  { delay = 0, status = 200, persistent = false } = {},
) {
  return (storyFn) => {
    const realFetch = window.fetch;
    const mockFetch = (url, opts) => {
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
    mockFetch.__realFetch = realFetch;
    window.fetch = mockFetch;

    const result = storyFn();

    if (!persistent) {
      queueMicrotask(() => {
        window.fetch = realFetch;
      });
    }

    return result;
  };
}

/**
 * Creates a Storybook decorator where fetch responses are controlled by a
 * mutable state object. Use for testing polling components that need to flip
 * between failure and success mid-test.
 *
 * Usage:
 *   const state = { responder: () => new Response(null, { status: 500 }) };
 *   export const MyStory = {
 *     decorators: [withProgrammableFetch("/api/log/", state)],
 *     async play({ canvasElement }) {
 *       // ...wait for failures...
 *       state.responder = () => new Response("[]", { status: 200 });
 *       // ...wait for recovery...
 *       window.fetch = window.fetch.__realFetch;
 *     },
 *   };
 */
export function withProgrammableFetch(urlPattern, state) {
  return (storyFn) => {
    const realFetch = window.fetch;
    const mockFetch = (url, opts) => {
      if (typeof url === "string" && url.includes(urlPattern)) {
        return Promise.resolve(state.responder(url, opts));
      }
      return realFetch(url, opts);
    };
    mockFetch.__realFetch = realFetch;
    window.fetch = mockFetch;
    return storyFn();
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
