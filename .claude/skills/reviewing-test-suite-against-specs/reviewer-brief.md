# Reviewer brief template

Fill the bracketed parts and dispatch one general-purpose Agent per dimension, all in one message so they run concurrently. Keep the conventions block verbatim; the rest is per family. Give absolute paths for the repository and the spec directory.

---

You are reviewing the [FAMILY] **[ROLE UNDER TEST]** conformance tests in the repo at [REPO ROOT] (Java, Spring Boot). In these tests the suite emulates a [COUNTERPARTY] and tests a real [ROLE UNDER TEST]. Goal: find (a) WRONG test cases (checks that contradict the spec, wrong severity, wrong or misnumbered spec references, checks that can never fail, dead code, brittle assumptions, [for emulator-side reviews: emulated behaviour that makes a correct implementation fail, hang or be misled — the emulator is allowed to be non-conformant on purpose and does not need to be polished, so report only deviations a correct counterpart would trip over]) and (b) MISSING test cases (normative requirements on [ROLE UNDER TEST] with no test), so the maintainer can judge whether the plan is ready for certification. Be rigorous and skeptical: every finding must cite a file:line and the verbatim spec clause. No style nits. Do not modify any files.

Spec texts (downloaded, plain text, grep them) in [SPEC DIR]:
- [name.txt — document, URL, note on which sections matter]
- [...]
[State which draft revision the certification target pins for each IETF draft, and which revision the code links. Paste the map-tags table for this family's prefixes. If two versions were fetched, ask the reviewer to report which version the code's behaviour actually corresponds to, and to treat every "—" or differing row as a finding to confirm.]

Code to review: [explicit file list: base modules, concrete modules, condition sub-packages, plans, unit tests, and the config-field declarations — AND the shared code outside the family package that the base classes extend or call (name the packages and the profile-behaviour classes)].

Project conventions you must apply (from AGENTS.md): severity must map to spec language for the behaviour under test (FAILURE for MUST/SHALL, WARNING for SHOULD, INFO for MAY; a MAY describes what the implementation may do, not what the suite is testing, and a server choice that prevents the behaviour under test from being exercised is a stop-on-failure FAILURE, not INFO or a skip); `fireTestSkipped` is only acceptable for a feature that is optional under every profile the module runs under, so a skip that lets an implementer certify without the mandatory behaviour being exercised is a finding; unit tests are for Conditions, module control flow is covered by CI pairings, so do not propose module-lifecycle unit tests; check the "Deliberate non-features" section of AGENTS.md before proposing a fix; a condition's own log() is INFO only, warnings need a separate condition invoked with ConditionResult.WARNING by the caller; every condition path must end in error() or logSuccess()/log(); requirement strings like "[PREFIX]-7.1.1" become links to [BASE URL]#section-7.1.1 — verify the section actually contains the cited requirement (a wrong number is a real finding); external-endpoint responses need status, Content-Type and body validated in separate conditions with unknown fields as WARNING; incoming requests to emulated endpoints need method, query, body and headers validated; timestamps need both bounds; invalid test inputs must be invalid only in the intended way; config error messages should reference UI labels; JSON access via OIDFJSON helpers. The suite tests SENDERS: unknown fields a sender includes should be flagged even where the spec says receivers ignore them.

Specific things to check, beyond your own reading:
1. [Metadata / discovery document: every REQUIRED and OPTIONAL field and constraint]
2. [Each management or protocol operation: request format, status codes, required response fields, error codes]
3. [Security artefacts: signature, alg, key size, iss/aud/iat/exp/jti, typ — including the ones the EMULATED side sends, where a defect would make a correct counterpart reject them]
4. [Delivery / transport RFC requirements]
5. [Variants: is every combination coherent; anything only working under one; does the certifiable plan pin values the profile does not mandate, or omit ones it does]
6. [Obvious non-conformances the plan would still pass]
7. [Unit-test coverage: list conditions with logic but no *_UnitTest]

Deliver a report with sections: (1) Wrong / over-strict / under-strict checks [and emulator non-conformance] (critical / important / minor, each with file:line, code behaviour, verbatim clause, why wrong). (2) Missing test cases mapped to verbatim clauses, one line on automation feasibility. (3) Spec-reference errors. (4) Unit-test and config-field gaps. (5) Spec ambiguities the suite had to resolve. Quote code minimally. Re-run `grep -n` on every line number immediately before writing it into the report; the maintainer will re-verify every critical item. Do not modify any files.

---

## Infrastructure reviewer (fourth agent)

Ask for: (1) CI coverage matrix from `make<Family>Tests` — modules executed, variant values exercised, modules never run, whether any third-party counterparty is used, every `expected-failures`/`expected-skips` entry for the family with its comment and whether it could mask a real bug, and whether the untested-module report covers the family (read `scripts/run-test-plan.py`, not just the flag); (2) the last ~5 successful master pipelines' `<family>_test` and `compare_<family>` jobs (one `/pipelines/<id>/jobs` call each; the pipelines list pages by 20) and the latest trace's failure/warning/skip/review counts for the family's plans only (GitLab token at `~/.gitlab-token`, read-only; skip this section if absent); (3) unit-test coverage table per condition sub-package, and `mvn -q test -Dtest='<pattern>'` result; (4) config paths read vs declared vs `src/main/resources/static/js/config-field-catalog.json`, plus keys set in the CI config files that no code reads; (5) module summaries and plan display names that are stale or inconsistent, and the `certificationProfileName` state; (6) open GitLab issues and MRs for the family with one-line gists, plus TODO/FIXME in the source; (7) docs and the openid.net certification page for the family (quote what it says is certifiable and compare with the code); (8) authorship and last substantive change, unmerged origin branches touching the package.
