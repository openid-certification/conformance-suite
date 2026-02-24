---
name: openid-review
description: Review code changes for correctness, spec compliance, and conformance suite conventions
---

# OpenID Conformance Suite Code Review

Review the current branch's changes against a target branch. If an argument is provided, use it as the target branch; otherwise default to origin/master.

Limit findings to changes in scope — do not flag pre-existing issues in unchanged code.

## Process

1. Identify all changed files using `git diff --name-only` against the target branch
2. For each changed file, read the diff and enough surrounding context (callers, base classes) to understand the impact
3. Check each concern below
4. Present findings grouped by severity: critical, important, minor

## Review Concerns

### Correctness
- Dead code or unreachable paths
- Null/missing environment keys that would cause runtime failures
- `@PreEnvironment` and `@PostEnvironment` annotations matching actual usage
- Condition result severity (FAILURE vs WARNING) appropriate for the check

### Conformance Suite Conventions
- Use `callAndStopOnFailure` vs `callAndContinueOnFailure` appropriately
- Environment paths navigated correctly (e.g., `env.getString("object", "nested.path")`)

### Spec Compliance
- RFC/spec references cited in conditions match the actual requirement
- Sender-vs-receiver validation distinction respected (suite validates senders)
- JSON schemas use `additionalProperties: false` for sender validation
- When specs are ambiguous, flag it rather than assuming one interpretation

### Security
- Extra scrutiny for JWT, JWK, DPoP, mTLS, and crypto-related changes
- No accidental weakening of validation (e.g., removing required checks)
- OWASP top 10 concerns (injection, XSS) in any HTTP-handling code

### Scope
- No unnecessary changes outside the purpose of the PR
