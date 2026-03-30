---
name: openid-review
description: Review code changes for correctness, spec compliance, and conformance suite conventions
---

# OpenID Conformance Suite Code Review

Review the current branch's changes from the point the branch diverged from a target branch. If an argument is provided, use it as the target branch; otherwise default to origin/master.

Concentrate on findings related to changes made on the branch — flag pre-existing issues in unchanged code in a separate section.

## Process

1. Identify all changed files using `git diff --name-only <target>...HEAD` (triple-dot) against the target branch — this shows only changes introduced on the current branch since it diverged, not unrelated changes on the target
2. Get the actual diff with `git diff <target>...HEAD` (triple-dot) to review only the branch's own changes
3. For each changed file, read the diff and enough surrounding context (callers, base classes) to understand the impact
4. **Find and read the relevant specifications:**
   - Read `src/main/java/net/openid/conformance/export/LogEntryHelper.java` to find the spec URL mappings (the `specLinks` HashMap maps prefixes like `"AUTHZEN-"` to spec base URLs)
   - Grep the changed files for spec reference strings (e.g., `"AUTHZEN-7.1"`, `"FAPI2SP-5.2.2"`) to identify which specs and sections are referenced
   - Use WebFetch to fetch the full specification at each relevant URL
   - Read the specific sections referenced by the code and extract the normative requirements (MUST/SHOULD/MAY), field definitions, and request/response formats
   - Verify that the spec URL in LogEntryHelper actually contains the referenced sections — if the URL points to an older draft that lacks them, flag it as a critical finding (broken spec links in the test log)
5. Check each concern below, **verifying spec references against the actual spec text** — confirm section numbers match the right requirements, field required/optional status matches what the code enforces, and response formats match what the code expects
6. Present findings grouped by severity: critical, important, minor

## Review Concerns

### Correctness
- Dead code or unreachable paths
- Null/missing environment keys that would cause runtime failures
- `@PreEnvironment` and `@PostEnvironment` annotations matching actual usage
- Condition result severity (FAILURE vs WARNING) appropriate for the check
- Every code path in a condition must either call `error()` (to fail) or `logSuccess()`/`log()` (to pass) before returning
- Calls to conditions should include a `requirements` string array referencing the relevant specification section, e.g., `callAndStopOnFailure(Cond.class, "RFC6749-4.1.3")`

### Conformance Suite Conventions
- Use `callAndStopOnFailure` vs `callAndContinueOnFailure` appropriately. 'StopOnFailure' is used when the problem would prevent later test steps executing correctly.
- Environment paths navigated correctly (e.g., `env.getString("object", "nested.path")`)
- Error messages for configuration issues should reference UI labels from `schedule-test.html`, not internal JSON key names, and include "in the test configuration"
- Unit test files must follow the `*_UnitTest.java` naming convention
- WARNING severity requires a separate condition that calls `error()`, invoked by the caller with `ConditionResult.WARNING` — a condition's own `log()` is INFO-level, not a warning

### HTTP Endpoint Validation
- When calling an external endpoint: validate HTTP status code, `Content-Type`, `Cache-Control` (where spec requires it), required response body fields, and flag unknown fields as WARNING via a separate condition
- When receiving a request at an emulated endpoint: validate HTTP method, query parameters (empty if spec defines none), request body (empty if spec defines none), and relevant request headers (`Content-Type`, `Accept`)
- Each check should be a separate condition so the caller controls severity

### Spec Compliance
- RFC/spec references cited in conditions match the actual requirement
- Sender-vs-receiver validation distinction respected (suite validates senders)
- JSON schemas use `additionalProperties: false` for sender validation
- When specs are ambiguous, flag it rather than assuming one interpretation

### Security
- Extra scrutiny for JWT, JWK, DPoP, mTLS, and crypto-related changes
- No accidental weakening of validation (e.g., removing required checks)
- OWASP top 10 concerns (injection, XSS) in any HTTP-handling code

### API Compatibility
- Any change to a REST endpoint's URL, method, request parameters, or response shape may be a breaking change for external API users — flag explicitly
- Removing or renaming fields in JSON responses breaks callers relying on those fields
- Adding required request parameters or changing optional ones to required is breaking
- Changing HTTP status codes or error response formats might be breaking
- Non-breaking additions (new optional response fields, new optional query params) are fine but worth noting
- In particular, changes to run-tests.sh may be a sign of a breaking change for external users - for example a variant changing name

### Scope
- No unnecessary changes outside the purpose of the PR
