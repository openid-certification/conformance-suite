---
title: "Anonymous /api/* fetches must not poison the login request cache"
date: 2026-06-04
category: best-practices
module: spring-security
problem_type: bug_root_cause
component: backend_security
severity: high
applies_when:
  - "A public/anonymous page (login.html, plans.html?public, logs.html) loads a component that fetches an authenticated /api/* endpoint"
  - "The user subsequently logs in via OAuth (GitHub/Google/GitLab) in the same browser session"
  - "Any new Spring Security filter chain is added or the API chain's request-cache/session config is touched"
symptoms:
  - "After OAuth login, the browser lands on a raw JSON API response at /api/<something>?continue instead of plans.html"
  - "The ?continue query parameter on the post-login URL (the fingerprint of Spring Security's saved-request replay)"
  - "Bug reproduces only when the user visited a public page (which fired anonymous API fetches) before logging in"
root_cause: cross_chain_session_state
resolution_type: code_fix
related_components:
  - WebSecurityResourceServerConfig
  - WebSecurityOidcLoginConfig
  - cts-footer
  - cts-navbar
tags:
  - spring-security
  - request-cache
  - saved-request
  - oauth-login
  - session
  - anonymous-api-fetch-side-effects
---

# Anonymous /api/* fetches must not poison the login request cache

## Context

The suite runs two Spring Security filter chains: the API chain
(`WebSecurityResourceServerConfig`, `@Order(1)`, everything under `/api/`) and
the OIDC login chain (`WebSecurityOidcLoginConfig`, `@Order(2)`, everything
else). They are separate configs, but both default to
`HttpSessionRequestCache`, which reads and writes the **same** `HttpSession`
attribute (`SPRING_SECURITY_SAVED_REQUEST`).

The redesigned frontend chrome fires API fetches from pages anonymous users
sit on: `cts-footer` fetches `/api/server` (version line) on every page
including `login.html`; `cts-navbar` fetches `/api/currentuser` on the public
listing pages. Both endpoints require authentication, so for anonymous
visitors those fetches 401 — visually harmless (the components fail soft).

## The bug

`ExceptionTranslationFilter` calls `requestCache.saveRequest()` **before**
invoking the 401 entry point. So every anonymous fetch to an authenticated
`/api/*` endpoint silently wrote `SPRING_SECURITY_SAVED_REQUEST` into the
shared session. When the user then logged in with GitHub, the OIDC chain's
default `SavedRequestAwareAuthenticationSuccessHandler` found the saved
request and "returned" the user to it: `302 /api/server?continue` — raw JSON
instead of plans.html.

Three non-obvious mechanics make this trap easy to re-introduce:

1. **The AJAX carve-outs don't cover `fetch()`.** Spring Security's default
   saved-request matcher excludes `X-Requested-With: XMLHttpRequest` (a
   jQuery convention) and `Accept: application/json` requests. Native
   `fetch()` sends `Accept: */*` and no `X-Requested-With` — and the matcher
   explicitly ignores `*/*` — so the request IS saved.
2. **`SessionCreationPolicy.NEVER` does not help.** The default
   `HttpSessionRequestCache` has `createSessionAllowed=true`, and
   `RequestCacheConfigurer` never propagates the `NEVER` policy to the cache.
   Only `STATELESS` makes Spring auto-install a `NullRequestCache`. (Verified
   against Spring Security 6.5.9.)
3. **The chains poison each other through the session.** A request saved by
   the API chain is replayed by the OIDC chain's success handler — the
   filter-chain separation does not isolate this state.

**Diagnostic fingerprint:** a post-login landing URL carrying `?continue` is
Spring Security 6's `matchingRequestParameterName` marker — it always means
"this navigation came from saved-request replay."

## The fix

`NullRequestCache` on the API chain
(`WebSecurityResourceServerConfig.filterChainResourceServer`):

- An API URL is never a sensible browser navigation target, so the API chain
  has no legitimate use for the request cache.
- Server-side placement neutralizes **every** current and future anonymous
  `/api/*` fetcher, not just the two known callers.
- The OIDC chain's request cache is deliberately untouched: anonymous
  navigation to a protected HTML page, then login, still returns the user to
  that page (desirable deep-link replay).

Guarded by `ResourceServerRequestCache_UnitTest`, which drives the real
filter chain (built in a minimal `@EnableWebSecurity` context with mocked
collaborators) through a `FilterChainProxy` and asserts an anonymous
fetch-style GET gets a 401 AND leaves no `SPRING_SECURITY_SAVED_REQUEST`
behind. The test pre-creates the session so the assertion proves suppression
rather than absence-of-session.

## Rejected alternatives

- **Suppress the frontend fetches** (skip when anonymous): fixes only the
  known callers; the session-poisoning footgun stays armed for the next
  public-page component that touches `/api/*`.
- **`defaultSuccessUrl("/", true)` on oauth2Login**: also discards the
  *legitimate* saved request for protected HTML pages, regressing
  deep-link-after-login.
- **Make `/api/server` public**: leaks version/revision/external-IP to
  anonymous users and does nothing for other endpoints.

## Recurring pattern

This is the second instance of **anonymous public-page `/api/*` fetch side
effects** — a public page fires a fetch whose side effect nobody modeled.
First instance (client-side): stale-response race in
[fetch-generation-guard-for-page-driven-components](../web-components/fetch-generation-guard-for-page-driven-components.md).
This instance (server-side): request-cache poisoning. When adding fetches to
pages reachable anonymously, ask: what does this request *do* on the server
besides return data, and what does its failure path mutate?
