# CLAUDE.md

This file provides guidance to coding agents when working with code in this repository.

## Project Overview

This is the OpenID Foundation conformance suite - a Spring Boot application that validates implementations of OpenID Connect, FAPI1, FAPI2, FAPI-CIBA, OpenID for Identity Assurance (eKYC), Verifiable Credentials (VCI), and Verifiable Presentations (VP).

## Workflow Rules

After making code changes, always run the project build and tests before committing. If tests fail, fix them before presenting the result as complete.

## Build and Development Commands

```bash
# Build (skip tests)
mvn -B -Dmaven.test.skip -Dpmd.skip clean package

# Run all tests (includes PMD and checkstyle checks)
mvn test

# Run a single test class
mvn test -Dtest=ClassName_UnitTest

# Run tests matching a pattern
mvn test -Dtest="*FAPI*_UnitTest"

# Run ArchUnit tests (quote pattern to avoid shell glob expansion)
mvn test -Dtest='*ArchUnit*'

# Skip PMD during test
mvn test -Dpmd.skip

# Skip checkstyle during test
mvn test -Dcheckstyle.skip

# Build final JAR
mvn package
```

The built JAR is `target/fapi-test-suite.jar`.

## Running Locally

The conformance suite requires MongoDB and an HTTPS reverse proxy. Use one of:

1. **devenv (Nix-based)**: `devenv up` - Sets up MongoDB, Nginx, TLS certs, and host aliases
2. **Docker Compose**: `docker-compose -f docker-compose-dev.yml up`

Then run the Spring Boot application from your IDE with profile `dev` or:
```bash
java -jar target/fapi-test-suite.jar --spring.profiles.active=dev
```

The app runs at `https://localhost.emobix.co.uk:8443` (regular) and `:8444` (mTLS).

### Dev loop (save-and-see)

The `dev` profile activates `spring-boot-devtools` plus a source-tree static handler so edits under `src/main/resources/static/` reflect on the next browser load (LiveReload reloads the tab automatically; plain F5 also works), and Java edits trigger a fast classloader restart in ~5 seconds.

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

In IntelliJ, set the Spring Boot run config's "Active profiles" to `dev` and enable "Build project automatically" + the Registry flag `compiler.automake.allow.when.app.running` so save triggers a recompile.

LiveReload runs on port 35729; install a LiveReload browser extension or rely on auto-injected `livereload.js` (DevTools serves it on the same origin). Static-only edits do not need a JVM restart — DevTools watches `src/main/resources/static` per `spring.devtools.restart.additional-paths` in `application-dev.properties`.

If save-and-see does not work, you are most likely running the packaged fat JAR (`java -jar target/fapi-test-suite.jar`) instead of `spring-boot:run` — the production JAR intentionally excludes DevTools.

**Production-parity invariant.** `spring-boot-devtools` MUST stay `<scope>provided</scope>` in `pom.xml`. The `maven-enforcer-plugin` rule `enforce-devtools-scope` fails the build at `validate` phase if the scope drifts to `compile`/`runtime`/`test`/`system`. Do not bypass the rule.

**Never set `SPRING_PROFILES_ACTIVE=dev` in a non-dev environment.** The dev profile activates `DummyUserFilter` (`fintechlabs.devmode=true`), which injects a synthetic admin-level user on every request and bypasses real authentication. The DevTools properties added alongside that flag do not change this risk, but they do live in the same file — read `application-dev.properties` end-to-end before deploying any environment that loads it.

### Running integration tests

Use `scripts/run-integration-tests.sh`, which handles building, server lifecycle, readiness checks, and test execution in one command. Output is automatically captured to `/tmp/integration-test-<timestamp>.log` — the script prints the log path before redirecting, then use `Read` to inspect results.

Available options (passed through to `.gitlab-ci/run-tests.sh`):
`--client-tests`, `--oidcc-tests`, `--fapi-tests`, `--ciba-tests`, `--local-provider-tests`, `--panva-tests`, `--ekyc-tests`, `--authzen-tests`, `--federation-tests`, `--ssf-tests`, `--vc-tests`

```bash
# List numbered plans to find the right --rerun number (no build/server needed)
.gitlab-ci/run-tests.sh --vc-tests --list

# Rerun a specific plan by its number from the last run
./scripts/run-integration-tests.sh --ekyc-tests --rerun 3

# Rerun a specific module within a plan
./scripts/run-integration-tests.sh --ekyc-tests --rerun 3:2

# Rerun multiple plans
./scripts/run-integration-tests.sh --ekyc-tests --rerun 1,3
```

The tests take a long time to run - always start by identifying a
relevant happy flow test module using `--list` to find the plan number,
then run it using `--rerun`. If that completes successfully then run a
fuller set.

Items tagged as "expected" errors, warnings or skips are not a problem.

Prerequisites:
- MongoDB running on `127.0.0.1:27017` (via `devenv up` or docker-compose)
- Nginx HTTPS proxy running (ports 8443/8444 -> 8080)
- `../conformance-suite-private` checkout (for test configs)
- Python 3 with `pyparsing >= 3` (on macOS: `PATH=/opt/homebrew/bin:$PATH`)

### Running VP-only tests

There is no dedicated `--vp-tests` option. To run only VP plans from `--vc-tests`, the --rerun option can be used.

## Architecture

### Test Module System

Tests are organized as **Test Modules** that extend `AbstractTestModule`. Each module:
- Is annotated with `@PublishTestModule` for discovery
- Composes reusable **Conditions** (single validation units)
- Uses an **Environment** object for state management
- Can be parameterized via **Variants** to generate multiple test configurations

```
AbstractTestModule
└── Protocol-specific base class (e.g., AbstractFAPI2SPFinalClientTest)
    └── Concrete test class
```

### Key Base Classes

- `AbstractTestModule` (`testmodule/`) - Core test lifecycle, condition calling, threading
- `AbstractCondition` (`condition/`) - Base for validation units with environment access and logging
- `ConditionSequence` (`sequence/`) - Composes conditions into reusable sequences
- `Environment` (`testmodule/`) - JSON object storage with path navigation

### Condition Calling Patterns

```java
// Fail test immediately on condition failure
callAndStopOnFailure(MyCondition.class);

// Log failure but continue execution
callAndContinueOnFailure(MyCondition.class, Condition.ConditionResult.WARNING);

// Skip if required environment values are missing
skipIfMissing(new String[]{"required_key"}, null, Condition.ConditionResult.INFO,
              MyCondition.class, Condition.ConditionResult.FAILURE);

// Call a sequence of conditions
call(sequence(MySequence.class));
```

### Environment Usage

```java
// Store objects
env.putObject("key", jsonObject);
env.putString("key", "value");

// Retrieve with path navigation
String value = env.getString("object", "nested.path");
JsonObject obj = getJsonObjectFromEnvironment(env, "object", "path");
```

### Package Organization

- `condition/` - Reusable validation conditions (subdivided by `as/` for server-side, `client/` for client-side)
- `testmodule/` - Core framework classes (TestModule, Environment, AbstractTestModule)
- `sequence/` - Reusable condition sequences
- `variant/` - Variant parameter enums and service
- `fapi1advancedfinal/`, `fapi2spfinal/`, `fapi2spid2/`, `fapiciba/` - FAPI protocol tests
- `openid/` - OpenID Connect tests
- `vci10issuer/`, `vci10wallet/` - Verifiable Credentials tests
- `vp1finalverifier/`, `vp1finalwallet/`, `vpid2*/`, `vpid3*/` - Verifiable Presentations tests
- `runner/` - Test execution and HTTP routing
- `plan/` - Test plan organization

### Kotlin Sources

The project is primarily Java but contains Kotlin source files for multipaz library integration:
- `src/main/java/com/android/identity/testapp/` - VP test credential provisioning (TestAppUtils.kt)
- `src/main/java/org/multipaz/testapp/` - VCI mdoc credential creation (VciMdocUtils.kt)

These use the [multipaz](https://github.com/openwallet-foundation/multipaz) library for mdoc/SD-JWT credential operations.

### JSON Schema Validation

Spec compliance checks can be implemented using JSON Schema validation. Schemas live in `src/main/resources/json-schemas/` and conditions extend `AbstractJsonSchemaBasedValidation`:

```java
public class ValidateDCQLQuery extends AbstractJsonSchemaBasedValidation {
    @Override
    protected JsonSchemaValidationInput createJsonSchemaValidationInput(Environment env) {
        JsonObject dcql = (JsonObject) env.getElementFromObject("client", "dcql");
        return new JsonSchemaValidationInput("DCQL query",
            "json-schemas/oid4vp/dcql_request.json", dcql);
    }
}
```
- Keep validation strict where the specification defines fixed fields.
- Unknown properties should raise warnings (not errors)
- A condition's own `log()` / `logSuccess()` calls are INFO-level — they do **not** produce warnings in the test log. The only way to surface a WARNING is for the **caller** to invoke the condition with `ConditionResult.WARNING` via `onFail(ConditionResult.WARNING)` or `callAndContinueOnFailure(..., ConditionResult.WARNING, ...)`. Therefore, if a check must appear as a warning, put it in a **separate condition** that `throw error(...)` on the finding, and have the caller set the severity to WARNING. Do not try to "warn" from inside a condition with `log()` — it will be invisible as a warning.

### Variants

Tests use `@VariantParameters` to generate multiple configurations from one class:

```java
@VariantParameters({ClientAuthType.class, FAPIResponseMode.class, ...})
public abstract class AbstractFAPI2SPFinalClientTest extends AbstractTestModule {
    // getVariant(ClientAuthType.class) returns the selected variant
}
```

Use `@VariantNotApplicable` to exclude invalid combinations.

### Test Plans

Test plans group related tests for certification via `@PublishTestPlan`:

```java
@PublishTestPlan(testPlanName = "fapi2-security-profile-final", testModules = {...})
public class FAPI2SPFinalTestPlan implements TestPlan {}
```

## Technical Standards & RFCs

When interpreting RFCs or technical specifications, present multiple defensible interpretations with trade-offs rather than committing to a single answer. Flag areas of ambiguity explicitly.

Key specifications for VP/VCI work:
- **OID4VP 1.0 Final**: https://openid.net/specs/openid-4-verifiable-presentations-1_0.html
- **OID4VCI 1.0 Final**: https://openid.net/specs/openid-4-verifiable-credentials-issuance-1_0.html
- **HAIP 1.0 Final**: https://openid.net/specs/openid4vc-high-assurance-interoperability-profile-1_0.html
- **OID4VP WG Draft**: https://openid.github.io/OpenID4VP/openid-4-verifiable-presentations-wg-draft.html
- **OID4VP GitHub**: https://github.com/openid/OpenID4VP

Identity Assurance spec locations used in this codebase:
- https://openid.net/specs/openid-connect-4-identity-assurance-1_0.html
- https://openid.net/specs/openid-connect-4-ida-attachments-1_0.html
- https://openid.net/specs/openid-connect-4-ida-claims-1_0.html
- https://openid.net/specs/openid-ida-verified-claims-1_0.html

OpenID Federation spec locations:
- **OpenID Federation 1.0**: https://openid.net/specs/openid-federation-1_0.html

## Error Messages for Configuration Issues

When a condition fails because of missing or invalid test configuration (fields the user fills in on schedule-test.html), error messages should reference the UI labels the user sees, not internal JSON key names. Include "in the test configuration" so the user knows where to look. For example:

- Good: `"'Payment consent request JSON' field is missing from the 'Resource' section in the test configuration"`
- Bad: `"brazilPaymentConsent not found in resource configuration"`

Check `src/main/resources/static/schedule-test.html` for the field labels displayed to users.

## Test-Suite Behavior Expectations

- This repository is a conformance test suite; explicit failures for invalid protocol behavior are expected.
- Ignored catches can be acceptable if they still lead to a clear and meaningful test failure.
- Generic `error(...)` text is acceptable when `args(...)` includes actionable detail.

### Sender vs Receiver Validation

When specs say "MUST ignore unknown properties", that applies to **receivers** (e.g., wallets processing DCQL queries). The conformance suite validates **senders** (e.g., verifiers constructing DCQL queries), so JSON schemas SHOULD use `additionalProperties: false` to flag unknown or misspelled fields as warnings (not errors) — senders should not include undefined properties.

### HTTP Endpoint Validation Checklist

When the test suite **calls an external endpoint** (e.g., a credential issuer's challenge endpoint), validate everything in the response:
- HTTP status code (e.g., `EnsureHttpStatusCodeIs200`)
- Response headers: `Content-Type` (e.g., `EnsureContentTypeJson`), `Cache-Control` where the spec requires it
- Response body: required fields present and valid, unknown fields flagged as a WARNING via a separate condition

When an external client **calls a test-suite endpoint** (e.g., a wallet calling the emulated challenge endpoint), validate everything in the request:
- HTTP method (e.g., `EnsureIncomingRequestMethodIsPost`)
- URL query parameters — if the spec defines none, check they are empty (`EnsureIncomingUrlQueryIsEmpty`)
- Request body — if the spec defines none, check it is empty (`EnsureIncomingRequestBodyIsEmpty`)
- Request headers where the spec defines requirements (e.g., `Content-Type`, `Accept`)

Each check should be a separate condition so the caller controls the severity (FAILURE vs WARNING).

## Code Quality

- **Checkstyle**: Google Java Style (configured in `.checkstyle.xml`)
- **PMD**: Rules in `.pmd.ruleset.xml`
- **Error Prone**: Enabled at compile time with specific exclusions
- **ArchUnit**: Architecture tests in `src/test/java/net/openid/conformance/archunit/`
- **JSON access in Java**: Avoid `JsonElement.getAsString/getAsInt/getAsLong/...`; use `OIDFJSON` helpers instead (e.g., `OIDFJSON.getString(...)`) to satisfy ArchUnit and avoid implicit conversions.

Tests compile with `-Werror` so all warnings must be resolved.

## Code Review

When asked to review a commit or branch, structure the review by file and call out: correctness issues (especially dead code or unreachable paths), API misuse, and behavioral changes. Don't just summarize — actively look for bugs.

## Git Workflow Preference

- To fix up a non-HEAD commit, use `git commit -m "fixup! <target commit message>"` then `git -c sequence.editor=true rebase -i --autosquash <base>` (`--autosquash` requires `-i`; `sequence.editor=true` suppresses the editor). This is safer than manual `reset --soft` + re-staging workflows.

## Git Operations

When making multi-file changes or library upgrades, create separate atomic commits per logical change. Before committing, verify the build passes for each commit independently.

If the change closes or fixes a GitLab issue — either one the user named when asking for the work, or one that's obviously the driver from the context — end the commit message with a trailer line like `Closes #1650` or `Fixes #1650` (just the `#N`, not a URL). GitLab auto-closes the issue when the MR merges. If the connection to an issue isn't obvious, ask rather than guess.

## Test Naming Convention

Unit test files follow the pattern `*_UnitTest.java` (e.g., `MyCondition_UnitTest.java`).

## Frontend E2E Tests

Playwright E2E tests in `frontend/e2e/` validate the legacy static HTML pages (`src/main/resources/static/*.html`) with mocked API responses. No backend required.

```bash
# Install frontend E2E dependencies deterministically
cd frontend && npm ci --ignore-scripts

# Run E2E tests
cd frontend && npm run test:e2e

# Run a single spec file
cd frontend && ./node_modules/.bin/playwright test e2e/home.spec.js
```

**When to run:** After modifying any file in `src/main/resources/static/` — HTML pages, `js/fapi.ui.js`, `templates/`, or `css/`. These tests catch regressions in page-level behavior.

**When to update tests:** If you change an API response shape consumed by the frontend, update the corresponding fixture in `frontend/e2e/fixtures/`. If you change page structure (DOM IDs, CSS classes used by JS), update the affected spec files.

**Key conventions:**
- Each spec file covers a single page; `journeys.spec.js` covers cross-page flows
- Route helpers in `frontend/e2e/helpers/routes.js` — `setupFailFast()` must be called FIRST (Playwright matches routes in reverse registration order), then specific routes
- All `page.route()` calls must happen before `page.goto()` because `fapi.ui.js` fires an API call at script parse time
- Fixture data lives in `frontend/e2e/fixtures/` as ES modules
- The `wrapDataTablesResponse()` helper wraps plain arrays in the `{draw, recordsTotal, recordsFiltered, data}` envelope for pages using jQuery DataTables with `serverSide: true` (plans.html, logs.html)

## Icons

All icons render via `<cts-icon name="<kebab>" size="16|20|24">`. The icon library is **coolicons v4.1**, vendored as per-icon SVG files at `src/main/resources/static/vendor/coolicons/icons/{name}.svg` — one file per icon, each ~3 KB, served individually. HTTP/2 multiplexes the small parallel requests, the browser caches per URL, and only the icons actually used hit the network.

- **Usage:** `<cts-icon name="external-link" size="20">` — the `name` attribute is the filename (without `.svg`); the `size` value is one of `16` / `20` / `24` (legacy aliases `sm`/`md`/`lg` still work). Sizes track `--space-4`/`--space-5`/`--space-6` and stroke colour follows `currentColor`, so consumers do not need any additional styling for theming.
- **Discoverability:** browse the AllIcons grid in Storybook (**Primitives/cts-icon → AllIcons**) for the full list of vendored names. Or `ls src/main/resources/static/vendor/coolicons/icons/`.
- **Adding a new icon:** when a call site needs an icon that isn't already vendored, follow the one-shot Python snippet documented in `src/main/resources/static/vendor/coolicons/README.md` (extract one symbol from the upstream sprite into a per-icon file). Do NOT add a build step.
- **Brand glyphs (Google, GitLab):** intentionally outside `cts-icon`. coolicons does not ship brand marks. Brand SVGs are inlined as `html\`<svg ...>\`` constants in `src/main/resources/static/components/cts-login-page.js` and used only there. If a future call site needs a brand mark, follow the same colocation pattern; do NOT add brand glyphs to the coolicons set.
- **Do NOT:** hand-roll inline `<svg>` paths for icons (use `cts-icon`); reference Bootstrap Icons (`bi-*` classes — removed); construct icon classes via string concatenation (no `className = \`bi bi-\${name}\`` — create a `cts-icon` element instead).

## Badges

All status pills, label chips, and count badges render via `<cts-badge variant="<name>">`. The status palette (`pass` / `fail` / `warn` / `running` / `skip` / `review`) and the utility variants (`primary` / `secondary` / `danger` / `info-subtle`) are token-routed through `oidf-tokens.css`. See `src/main/resources/static/components/cts-badge.js` and Storybook **Primitives/cts-badge** for the full inventory.

**Affordance rule:** every variant supports two visual states. The state must reflect whether clicking the badge does anything.

- **Read-only (default):** fill only, no border. The badge is a label for state — pass/fail/warn/running/skip status, role marker (`ADMIN`), spec requirement chip, count summary, etc. The user does not click on it to do anything.
- **Interactive:** fill + 1px inset `box-shadow` ring + hover/focus. The badge is itself a click target, or it sits inside a wrapper that has stripped its own affordance (e.g., an `<a>` with `text-decoration: none`) so the badge silhouette is what the user perceives as clickable.

**When to use which attribute:**
- **`interactive`** — visual only. Adds the ring without `role="button"`. Use when the badge sits inside an `<a>` or `<button>` whose own affordance is invisible (no underline, no hover) so the badge needs to carry the affordance signal itself. Existing example: `cts-plan-modules.js` wraps the module status pill in a no-decoration anchor to log-detail; the badge is marked `interactive` so the affordance reads.
- **`clickable`** — semantic + visual. Adds `role="button"`, `tabindex="0"`, keyboard activation, and emits `cts-badge-click`. Implies `interactive` visually — a clickable badge always renders the ring even when `interactive` is not set. Use when the badge IS the click target and is not already wrapped in an `<a>`/`<button>`/parent click handler.

**Affordance decision tree (from the badge sweep plan, `docs/plans/2026-04-27-001-feat-badge-affordance-rule-plan.md`):**
1. Is the cts-badge itself the click target? Yes → `clickable`. No → step 2.
2. Is the badge wrapped in an interactive element (`<a>`, `<button>`, parent click handler)? No → leave read-only. Yes → step 3.
3. Does the wrapper provide its own visible affordance (link underline, button background, hover state)? Yes → leave read-only (the wrapper is doing the work; adding the ring is redundant noise). No → `interactive`.

**Token deviation:** The readonly `b-rev` (Review) chip uses `var(--bg-muted)` (#F8F7F5) as its background fill. The token system does not currently define a `--status-review-bg`; if one lands in the archive, switch the fill to that token. Update the JSDoc block in `cts-badge.js` if the deviation is resolved.

- **Do NOT:** hand-roll a 1px `border` around a chip-like element to fake the affordance ring — use `cts-badge` with `interactive`/`clickable`. The component implements the ring as an inset `box-shadow` so the box-model dimensions are identical in both states; a real `border` would shift the box by 1px when toggling affordance. Do NOT add `clickable` to a badge that is already inside a clickable parent (`<a>` or `<button>`) — that nests `role="button"` inside link/button semantics and produces ambiguous keyboard activation. Do NOT use `bg-warning` / `bg-info` / `bg-info-subtle` / `border-info-subtle` / `text-info-emphasis` Bootstrap utility classes — those are removed; use the canonical variants instead.

## Frontend quality gates

Lint, format, and type-check for the frontend are covered by the `frontend_lint` GitLab job, mirrored locally by `npm run test:ci` from `frontend/` (format:check → lint → type-check → lint:jsdoc → lint:lit-analyzer). See `frontend/README.md` for the command reference and failure-mode decoder. `lit-analyzer` provides Lit-aware template diagnostics (unknown elements, wrong binding sigils, unclosed tags); `ts-lit-plugin` exposes the same diagnostics inside TypeScript-language-service IDEs.

Severity ladder: default is `error`; R8 light-DOM preset warnings from `eslint-plugin-lit` / `eslint-plugin-wc` stay at `warn`; a named Legacy Overrides block in `frontend/eslint.config.js` tracks per-file exceptions to zero — never blanket `"off"`.

The CI job currently runs with `allow_failure: true` and is due to flip to blocking on 2026-06-12. The promotion criteria and flip procedure live in the comment block above the `frontend_lint` job in `.gitlab-ci.yml`. The date reminder lives on the owner's calendar, not in the repo.

## Key Dependencies

- **multipaz** (CBOR/COSE/mdoc library): Source is at https://github.com/openwallet-foundation/multipaz — use this to look up API details rather than unpacking JARs.
- **Nimbus JOSE+JWT** (JWT/JWK/JWS/JWE library): Source is at https://bitbucket.org/connect2id/nimbus-jose-jwt/src/master/ — use this to look up API details rather than unpacking JARs.
- **Lit** (web-components runtime): Vendored bundle at `src/main/resources/static/vendor/lit/lit.js` is the full [`lit-all.min.js`](https://github.com/lit/dist) release, so every directive (`classMap`, `repeat`, `when`, `ifDefined`, `ref`, …) is available at runtime without a bundler. Bump via `frontend/scripts/update-vendor-lit.sh` (pinned by git tag + SHA-256 digest).
- **Monaco editor** (JSON editor for `schedule-test.html`): Vendored AMD distribution at `src/main/resources/static/vendor/monaco-editor/vs/`. The single supported entry point is the `<cts-json-editor>` Lit primitive — pages must NEVER call `monaco.editor.create(...)` directly, since that bypasses the wrapper's lazy-load, fallback-to-textarea, and disposal lifecycle. Bump via `frontend/scripts/update-vendor-monaco.sh` (pinned by version + tarball SHA-256). See `src/main/resources/static/vendor/monaco-editor/README.md` for the curated minimal subset rationale.

## Creating New Tests

1. Create a condition class extending `AbstractCondition` with `@PreEnvironment`/`@PostEnvironment` annotations
2. Create a test module extending the appropriate abstract base class
3. Annotate with `@PublishTestModule`
4. Add to a test plan
