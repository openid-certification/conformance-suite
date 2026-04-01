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
- Condition result severity must map to spec requirement language:
  - `ConditionResult.FAILURE` for mandatory clauses ("shall", "must")
  - `ConditionResult.WARNING` for recommended clauses ("should")
  - `ConditionResult.INFO` for optional behavior ("may")
- Every code path in a condition must either call `error()` (to fail) or `logSuccess()`/`log()` (to pass) before returning
- New or modified condition calls must include a `requirements` string array referencing the relevant specification section, e.g., `callAndStopOnFailure(Cond.class, "RFC6749-4.1.3")`

### Conformance Suite Conventions
- Use `callAndStopOnFailure` vs `callAndContinueOnFailure` appropriately: `StopOnFailure` when the problem would prevent later test steps executing correctly; `ContinueOnFailure` when downstream steps are unaffected
- Environment paths navigated correctly (e.g., `env.getString("object", "nested.path")`)
- Error messages for configuration issues should reference UI labels from `schedule-test.html`, not internal JSON key names, and include "in the test configuration"
- Unit test files must follow the `*_UnitTest.java` naming convention
- New conditions require unit tests with thorough coverage, including edge cases
- WARNING severity requires a separate condition that calls `error()`, invoked by the caller with `ConditionResult.WARNING` — a condition's own `log()` is INFO-level, not a warning
- `@PublishTestModule` summary field must clearly communicate: what the test does, why, and the expected outcome
- New test modules must be added to relevant test plans and have associated configurations for automated regression testing
- American English throughout (e.g., "authorization" not "authorisation")
- **TLS certificate failures**: if new code handles TLS errors, verify it does not hard-fail on certificate rejections — the test should accept refusals at the SSL/TLS layer or HTML error responses where JSON is expected (warn, not fail)

### HTTP Endpoint Validation
- Use a **single condition** to make the HTTP call; perform **all validation** (status codes, headers, body) in **separate subsequent conditions** — this keeps each check isolated so the caller controls severity
- When calling an external endpoint: validate HTTP status code, `Content-Type`, `Cache-Control` (where spec requires it), required response body fields, and flag unknown fields as WARNING via a separate condition
- When receiving a request at an emulated endpoint: validate HTTP method, query parameters (empty if spec defines none), request body (empty if spec defines none), and relevant request headers (`Content-Type`, `Accept`)

### Spec Compliance
- RFC/spec references cited in conditions match the actual requirement
- Sender-vs-receiver validation distinction respected (suite validates senders)
- JSON schemas use `additionalProperties: false` for sender validation — the schema condition throws on unknown fields, but the **caller** should invoke it with `ConditionResult.WARNING` so unknown fields are warnings, not test failures
- When specs are ambiguous, flag it rather than assuming one interpretation
- **Spec version targeting**: prefer final specification > implementers draft > numbered draft. Only reference working-group drafts when the code explicitly targets a draft spec variant
- **Unknown elements**: when specs require ignoring unknown elements, maintain a list of expected keys and warn (not fail) about unexpected ones — this catches common typos (e.g., "claim" instead of "claims")
- **Strictness balance for error paths**: enforce error checks where clients must recover from the error (e.g., expired access tokens), but for error-path responses specifically (e.g., error response format after rejecting a bad JAR signature), the response format may be less critical than the rejection itself — use WARNING rather than FAILURE for error-response formatting checks

### Test Design Principles
- **Invalid test values** must be "invalid only in the intended way" — e.g., a bad JWS signature should still be valid base64url with the correct length. Either use a valid value from a different client or deliberately invalidate a known good value in a targeted way
- **JWT `aud` values**: test both array and string formats unless the spec explicitly prohibits one
- **Random strings** (state, nonce, jti, etc.):
  - Character set: use base64url plus `.` and `~`; avoid non-URL-safe, control, or non-ASCII characters unless explicitly allowed or the value is user-visible
  - When receiving: warn about potential interoperability issues with non-standard characters; fail where specs permit it
  - Length: normal tests should use specification-example lengths; include at least one "sensibly large" test (e.g., 512 characters for JWS-based values)
  - Check that received values exceeding the longest value sent in happy-flow tests trigger a warning

### Security
- Extra scrutiny for JWT, JWK, DPoP, mTLS, and crypto-related changes
- No accidental weakening of validation (e.g., removing required checks)
- OWASP top 10 concerns (injection, XSS) in any HTTP-handling code

### Timestamp and Duration Validation
- When validating any time-based value, always check **both bounds** — a missing bound means the conformance suite won't catch an implementation that produces nonsensical values:
  - `iat`: must not be in the future (allow clock skew); if the token should be freshly issued, must not be too far in the past; at minimum, must not be before ~2012 (the JWT spec era)
  - `exp`: must not be in the past (allow clock skew); must not be unreasonably far in the future (e.g., more than ~50 years)
  - `nbf`: must not be unreasonably far in the future or past
  - `expires_in`: must be positive; must not be zero or unreasonably large (e.g., 50 years of seconds)
- This applies to any protocol field carrying a timestamp or duration, not just JWTs
- Check that clock skew allowances are reasonable (typically a few minutes, not hours)

### API Compatibility
- Any change to a REST endpoint's URL, method, request parameters, or response shape may be a breaking change for external API users — flag explicitly
- Removing or renaming fields in JSON responses breaks callers relying on those fields
- Adding required request parameters or changing optional ones to required is breaking
- Changing HTTP status codes or error response formats might be breaking
- Non-breaking additions (new optional response fields, new optional query params) are fine but worth noting
- In particular, changes to run-tests.sh may be a sign of a breaking change for external users - for example a variant changing name

### Git Hygiene
- Commits should be logical and complete — no "fix" commits that correct earlier commits in the same MR (these should be squashed/fixup'd before review)
- Clean, meaningful commit messages following [best practices](https://chris.beams.io/posts/git-commit/)
- Branch should be rebased onto latest origin/master

### Scope
- No unnecessary changes outside the purpose of the PR
