// Ambient declarations for vendor globals loaded via <script> tags in the legacy
// HTML templates (src/main/resources/static/*.html). These are NOT imported as
// ES modules; the components assume they exist on `window`. Declared as `any`
// here because we don't ship the vendor type packages (@types/bootstrap, etc.)
// and the surface area we touch is small (Modal, Tooltip, Popover constructors).
//
// If a new vendor global is needed, add it here; prefer the narrowest declaration
// that satisfies the consumers.

declare const bootstrap: any;

// Test-only side channels set by story decorators (`withProgrammableFetch`,
// `withMockFetch`) to coordinate state between a decorator and its story's
// `play` function. Not part of runtime component API.
interface Window {
  __ctsLogViewerFetchState?: any;
  __copiedText?: string | null;
}

// Playwright E2E specs run under Node and occasionally touch Node globals
// (e.g. `Buffer.alloc` for setInputFiles). @types/node would be overkill for
// a single reference; declare the narrow surface we actually use instead.
declare const Buffer: {
  alloc(size: number, fill?: number): Uint8Array;
};
