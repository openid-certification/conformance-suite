# CTS UX Modernization — Component Architecture Design

## Status

**Proposal** — pending review by core maintainers (Joseph, Thomas)

## Problem

The CTS frontend is 10 standalone HTML pages with no shared components, no module system, and no development tooling. The largest file (`schedule-test.html`, 3,353 lines) contains 411+ hardcoded form `<input>` elements shown/hidden by JavaScript. The API sends field *names* (like `"server.issuer"`) but no types, descriptions, or validation rules. This creates the UX pain points identified in the February 2026 UX review:

- **Trial-and-error config loop** — config errors only surface as test failures (R7)
- **Confusing test plan selection** — 77 plans in a 4-dropdown cascade, no search or guidance (R6)
- **No config sharing** — share URLs are 9,522 characters and break browsers (R8/R10)
- **No automation support** — AI agents and CI scripts can't validate config before execution (P6)
- **No batch execution** — each test module must be run individually (R17)
- **Overwhelming log display** — flat list, no collapsibility, failures buried in noise (R27/R28)

## Decision

Three sequential phases, each with internal parallelism for subagent execution:

1. **Phase 1: Schema Infrastructure** (Java) — Build the machinery to generate JSON Schema from annotations. Hybrid metadata: Java annotations define field types, JSON Schema files define validation rules.
2. **Phase 2: Component Primitives** (JS + Storybook) — Build reusable Web Components with Storybook interaction tests. UI primitives first, then feature components.
3. **Phase 3: Page Integration** (HTML) — Wire components into pages, replacing monolithic JavaScript with component composition.

Each phase completes before the next begins. Within each phase, subagents work in parallel on non-overlapping files.

## Architecture

### Metadata Strategy: Hybrid Annotations + JSON Schema

**Java annotations** declare the basic field type (via a `ConfigFieldType` enum):
- `STRING` — plain text input
- `URI` — URL with format validation
- `JSON_OBJECT` — JSON editor with syntax validation
- `JSON_ARRAY` — JSON array editor
- `CERTIFICATE` — PEM certificate with expiry display
- `FILE_UPLOAD` — file picker
- `SELECT` — dropdown from enum values
- `BOOLEAN` — checkbox
- `SECRET` — masked input

**A `ConfigFieldRegistry`** maps every known field name to its metadata (type, label, description, section, required). Initially populated from a static mapping file (`config-fields.json`). The registry is the bridge between annotations (which declare field names) and the API (which serves field metadata).

**JSON Schema files** (in `src/main/resources/json-schemas/config/`) define validation rules for 3-5 representative plan families. These use `$ref` to share common field definitions (`common-server.json`, `common-client.json`). This follows the existing pattern used for spec compliance validation (e.g., DCQL query schemas).

### API Surface

**`GET /api/plan/schema/{planName}`** — Returns JSON Schema for a plan's configuration, including field metadata (type, title, description, validation constraints) and a `uiSchema` object defining section layout and field order.

Response shape:
```json
{
  "planName": "oidcc-basic-certification-test-plan",
  "schema": {
    "type": "object",
    "properties": {
      "server": {
        "type": "object",
        "properties": {
          "issuer": {
            "type": "string",
            "format": "uri",
            "title": "Issuer URL",
            "description": "The OpenID Provider's issuer identifier"
          }
        },
        "required": ["issuer"]
      }
    }
  },
  "uiSchema": {
    "sections": [
      { "key": "server", "title": "Server Configuration" },
      { "key": "client", "title": "Client Configuration" }
    ],
    "fieldOrder": ["server.issuer", "server.jwks", "client.client_id"]
  }
}
```

**`POST /api/plan/validate`** — Validates config against the plan's schema before test creation. Returns structured errors distinguishing field-level validation failures from missing required fields.

Response shape:
```json
{
  "valid": false,
  "errors": [
    { "field": "server.issuer", "message": "Required field", "type": "required" },
    { "field": "client.jwks", "message": "Invalid JSON", "type": "format" }
  ]
}
```

### Component Hierarchy

#### UI Primitives

Light DOM components that inherit Bootstrap CSS and provide a consistent API surface. These are the building blocks for feature components.

**`<cts-button>`** — Button with variant (primary/secondary/danger/info), loading state, disabled state, icon slot. Replaces the scattered `btn btn-sm btn-info bg-gradient border border-secondary` pattern.

Props: `variant`, `loading`, `disabled`, `type` (button/submit). Slots: `icon`, default (label text).

**`<cts-link-button>`** — Anchor element styled as a button. The `<a class="btn ...">` pattern used throughout the codebase.

Props: `href`, `variant`, `disabled`. Slots: `icon`, default.

**`<cts-badge>`** — Status and label badges. Covers test result badges (passed/failed/warning/review/skipped), the ADMIN badge, and count indicators.

Props: `variant` (success/failure/warning/review/info/danger), `count`, `clickable`. Events: `cts-badge-click`.

**`<cts-icon>`** — Wraps Bootstrap Icons with consistent sizing. Replaces `<span class="bi bi-files">`.

Props: `name` (Bootstrap Icon name), `size` (sm/md/lg).

**`<cts-card>`** — Content container with optional header, body, and footer. Used in plan-detail module cards and log entry containers.

Slots: `header`, default (body), `footer`.

**`<cts-modal>`** — Dialog component replacing the 7 copy-pasted error modal blocks. Manages open/close state and focus trapping.

Props: `open`, `title`. Events: `cts-modal-close`. Slots: default (body), `footer`.

**`<cts-tooltip>`** — Popper-powered tooltip for non-form contexts (status help text, truncated names). Form field tooltips are handled internally by `<cts-form-field>`.

Props: `content`, `placement` (top/bottom/left/right). Slots: default (trigger element).

#### Feature Components

Composed from primitives. Each is a self-contained unit with its own Storybook stories and interaction tests.

**`<cts-form-field>`** — Schema-driven form field. Renders the appropriate input type based on the field's JSON Schema definition. Handles validation error display and help text (absorbs the 137 tooltip icons in schedule-test.html).

Props: `schema` (JSON Schema for this field), `value`, `error`, `disabled`. Events: `cts-field-change { field, value }`.

**`<cts-config-form>`** — Complete configuration editor. Owns both Form Tab and JSON Tab views internally, with two-way sync between them. Renders fields from schema, groups them by section, and manages field visibility based on variant selection. Calls the validation endpoint on submit.

Props: `schema`, `ui-schema`, `config`, `variant`, `errors`. Events: `cts-config-change { config }`, `cts-validate`.

**`<cts-test-selector>`** — Guided test plan selection. Replaces the 4-dropdown cascade with search, filtering by spec family, and inline variant selectors.

Props: `plans` (available plans array), `selected`, `variant`. Events: `cts-plan-select { plan }`, `cts-variant-change { variant }`.

**`<cts-log-viewer>`** — Structured log display with collapsible condition blocks, status summaries per section, and clickable failure badges that jump to the relevant entry. Handles streaming via polling `/api/log/{id}`.

Props: `test-id`, `auto-scroll`. Internal: polls API, manages collapse state, renders `<cts-badge>` for results.

**`<cts-batch-runner>`** — Batch test execution with Run All / Run Remaining, progress indicator ("6 of 30"), and module status grid.

Props: `plan-id`, `modules`. Events: `cts-run-all`, `cts-run-remaining`.

### Storybook Testing Strategy

Every component gets Storybook stories with `play` function interaction tests (using `storybook/test`). Stories must cover:

- **Happy path** — component renders correctly with valid props
- **Edge cases** — empty data, long strings, missing optional props
- **Error states** — validation errors, API failures, loading states
- **Interaction** — user clicks, form input, tab switching, collapse/expand

API calls are mocked via fetch interception in story decorators (established pattern from `cts-navbar` stories). Shared mock fixtures live in `frontend/stories/fixtures/`.

## Implementation Phases

### Phase 1: Schema Infrastructure (Java)

No frontend changes. Subagent parallelism within the phase.

| Unit | Description | Dependencies | Parallel? |
|------|-------------|--------------|-----------|
| 1A | `ConfigFieldType` enum + `ConfigFieldRegistry` + static field mapping | None | Serial (foundation) |
| 1B | `GET /api/plan/schema/{planName}` endpoint | 1A | Parallel with 1C |
| 1C | JSON Schema validation files for 3-5 representative plans | 1A | Parallel with 1B |
| 1D | `POST /api/plan/validate` endpoint | 1B + 1C | Serial |

Files touched: `src/main/java/net/openid/conformance/` (new service classes, API additions), `src/main/resources/json-schemas/config/` (new schema files).

### Phase 2: Component Primitives (JS + Storybook)

No page integration. All components built and tested in Storybook. Subagent parallelism: 5 agents, no file conflicts.

| Unit | Components | Dependencies | Files |
|------|-----------|--------------|-------|
| 2P | UI Primitives: `cts-button`, `cts-link-button`, `cts-badge`, `cts-icon`, `cts-card`, `cts-modal`, `cts-tooltip` | None | `components/`, `stories/primitives/` |
| 2A | `cts-form-field` + `cts-config-form` | Phase 1 schema format, 2P primitives | `components/`, `stories/` |
| 2B | `cts-test-selector` | Phase 1 API shape, 2P primitives | `components/`, `stories/` |
| 2C | `cts-log-viewer` (composes `cts-badge`) | Existing `/api/log` response, 2P primitives | `components/`, `stories/` |
| 2D | `cts-batch-runner` (composes `cts-badge`) | Phase 1 batch API (or mocked), 2P primitives | `components/`, `stories/` |

Note: 2P (primitives) must complete before 2A-2D begin, since feature components compose primitives. 2A-2D run in parallel.

### Phase 3: Page Integration

Wire components into pages. Subagent parallelism: 3 agents, each owns different HTML files.

| Unit | Page(s) | Components | What it replaces |
|------|---------|------------|-----------------|
| 3A | `schedule-test.html` | `cts-test-selector`, `cts-config-form` | 4 cascade dropdowns, 411 hardcoded inputs, ~2000 lines of JS |
| 3B | `plan-detail.html`, `running-test.html` | `cts-batch-runner`, `cts-badge` | Per-module run buttons, template-rendered status |
| 3C | `log-detail.html` | `cts-log-viewer`, `cts-badge` | Flat log entry list, template rendering |

## Requirements Coverage

| Req | Description | Component | Phase |
|-----|-------------|-----------|-------|
| R5 | Primary navigation on all pages | `<cts-navbar>` | **Done** |
| R6 | Guided test selection | `<cts-test-selector>` | 2B → 3A |
| R7 | Pre-run config validation | Schema + `<cts-config-form>` | 1D → 2A → 3A |
| R13 | New plans start blank | `<cts-config-form>` default state | 3A |
| R17 | Run All / Run Remaining | `<cts-batch-runner>` | 2D → 3B |
| R19 | "Waiting for user input" | `<cts-badge>` + running page | 3B |
| R20 | Progress indicator "6 of 30" | `<cts-batch-runner>` | 2D → 3B |
| R22 | Remove "no longer running" | running-test.html rework | 3B |
| R27 | Collapsible log sections | `<cts-log-viewer>` | 2C → 3C |
| R28 | Clickable failure jump-links | `<cts-log-viewer>` | 2C → 3C |
| R31 | Prioritize log content | `<cts-log-viewer>` layout | 2C → 3C |
| R51 | JSON Schema for config | Schema infra + files | 1A-1D |
| R52 | Batch execution API | Backend + `<cts-batch-runner>` | 1 → 2D |
| P6 | Agent/automation support | Schema API + validation endpoint | 1B + 1D |

## Scope Boundaries

- **In scope:** Schema infrastructure, component library, Storybook tests, page integration for schedule-test.html, plan-detail.html, log-detail.html, running-test.html
- **Out of scope:** Remaining Quadrant 1 quick wins (R14, R15, R19, R22, R23, R26 — these are small targeted changes, not component architecture), accessibility fixes (R41-R50 — separate track), certification flow (R33-R35 — blocked on OIDF decisions), documentation platform (R37-R38), organization accounts (R56)
- **Not changing:** Java test module logic, condition/sequence code, Kotlin/multipaz code, existing API endpoint contracts (all changes are additive)

## Risks

| Risk | Mitigation |
|------|------------|
| Schema infrastructure scope creep — trying to schema all 77 plans | Explicitly scoped to 3-5 representative plans. The machinery is the deliverable, not complete coverage. |
| `schedule-test.html` is the most-churned file (72 edits/year) — component migration may conflict with concurrent spec work | Phase 3A should be done on a feature branch with a short integration window. Coordinate with core team. |
| Feature components may need primitives that weren't anticipated | Primitives (2P) are delivered first. If a feature component needs a new primitive, it adds one — this is additive, not a redesign. |
| Storybook mocks may diverge from real API | Phase 1 delivers the real API. Phase 2 components can be validated against it. Mock fixtures include a contract test that verifies shape. |

## Sources & References

- **Origin requirements:** `docs/brainstorms/2026-04-13-cts-ux-improvement-plan-requirements.md`
- **Parent plan:** `docs/plans/2026-04-13-001-feat-cts-ux-30-day-sprint-plan.md`
- **Web Components ADR:** `docs/superpowers/specs/2026-04-13-web-components-frontend-modernization-design.md`
- **Visual design:** `docs/cts-ux-modernization-full-design.png`
- Current form architecture: `src/main/resources/static/schedule-test.html`
- Config field annotations: `src/main/java/net/openid/conformance/testmodule/PublishTestModule.java`
- Variant service: `src/main/java/net/openid/conformance/variant/VariantService.java`
- Test plan API: `src/main/java/net/openid/conformance/info/TestPlanApi.java`
- Existing JSON Schema validation: `src/main/java/net/openid/conformance/util/validation/JsonSchemaValidation.java`
