// Ambient declarations for vendor globals loaded via <script> tags in the legacy
// HTML templates (src/main/resources/static/*.html). These are NOT imported as
// ES modules; the components assume they exist on `window`. Declared as `any`
// here because we don't ship the vendor type packages (@types/bootstrap, etc.)
// and the surface area we touch is small (Modal, Tooltip, Popover constructors).
//
// If a new vendor global is needed, add it here; prefer the narrowest declaration
// that satisfies the consumers.

declare const bootstrap: any;

// Monaco editor — lazy-loaded by `<cts-json-editor>` via the AMD bundle at
// `/vendor/monaco-editor/vs/loader.js`. The wrapper is the only consumer;
// the type is intentionally `any` since we don't ship `@types/monaco-editor`
// and the surface we touch is small (`monaco.editor.create`, `defineTheme`,
// `setModelLanguage`, `EditorOption.readOnly`). The AMD loader registers
// `window.require` which we configure with the `vs/` path, then drive into
// `require(['vs/editor/editor.main'], …)`. `MonacoEnvironment.getWorkerUrl`
// is read by Monaco at editor-create time to resolve the JSON web worker.
interface Window {
  monaco?: any;
  require?: any;
  MonacoEnvironment?: { getWorkerUrl: (...args: string[]) => string };
}

// Test-only side channels set by story decorators (`withProgrammableFetch`,
// `withMockFetch`) to coordinate state between a decorator and its story's
// `play` function. Not part of runtime component API.
interface Window {
  __ctsLogViewerFetchState?: any;
  __copiedText?: string | null;
}

// R24 split-summary helper exposed for the lodash Mustache template
// (`templates/logHeader.html`). Components use the ES-module import
// directly; only the legacy template path needs the global. Declared
// here so TypeScript accepts the `window.CTS_summarySplit = ...`
// assignment in `components/test-summary-split.js`.
interface Window {
  CTS_summarySplit?: {
    SUMMARY_SPLIT_MARKER: string;
    splitTestSummary: (
      raw: string | null | undefined,
    ) => { description: string; instructions: string };
  };
}

// Playwright E2E specs run under Node and occasionally touch Node globals
// (e.g. `Buffer.alloc` for setInputFiles). @types/node would be overkill for
// a single reference; declare the narrow surface we actually use instead.
declare const Buffer: {
  alloc(size: number, fill?: number): Uint8Array;
};
