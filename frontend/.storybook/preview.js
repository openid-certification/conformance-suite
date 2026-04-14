import { definePreview } from "@storybook/web-components-vite";
import { setupWorker } from "msw/browser";

// Create a shared MSW worker instance (no default handlers — stories provide them)
const worker = setupWorker();
let started = false;

/**
 * Custom MSW loader that replaces msw-storybook-addon.
 * Stories declare handlers via parameters.msw.handlers; the loader
 * starts the worker on first run and swaps handlers per-story.
 */
async function mswLoader(context) {
  const { parameters } = context;
  const handlers = parameters?.msw?.handlers ?? [];
  const handlerArray = Array.isArray(handlers)
    ? handlers
    : Object.values(handlers).flat();

  if (!started) {
    await worker.start({ onUnhandledRequest: "bypass", quiet: true });
    started = true;
  }

  worker.resetHandlers();
  if (handlerArray.length > 0) {
    worker.use(...handlerArray);
  }
}

export default definePreview({
  parameters: {
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
    },
  },
  loaders: [mswLoader],
});
