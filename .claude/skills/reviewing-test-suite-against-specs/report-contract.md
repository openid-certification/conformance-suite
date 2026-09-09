# Report contract

The published report has these parts, in this order. A section with nothing in it says so in one line rather than disappearing.

1. **Verdict** — "Ready" or "Not yet", then one paragraph on why, then the three questions (missing? wrong? ready?) each answered in a sentence with counts.
2. **Tally strip** — modules run in CI / total, variant values never run, unit-test coverage, negative tests on each role, wrong anchors, third-party counterparties.
3. **Certification target** — which document version the code implements, which one the log links point at, what openid.net says is certifiable, and whether the plans carry a certification profile. Contradictions between these are always the first finding.
4. **Wrong tests: conformant implementations fail, hang or crash** — grouped by role. Each item: title, what the code does, verbatim clause, file:line (verified) or file.
5. **Missing checks: non-conformant implementations pass** — grouped by role; MUSTs graded WARNING/INFO listed together; sibling specs the suite tolerates without validating (e.g. RISC) noted.
6. **Missing test cases by clause** — one table per role: clause, requirement, feasibility (yes / partly / needs real trigger / needs multi-tenant / WG decision).
7. **Requirement anchors pointing at the wrong section** — table: where, cited, should be. Include non-existent sections and prefixes linked to a moved draft.
8. **CI, unit tests, configuration** — coverage matrix table, never-run modules and variants, unit-test numbers, config fields read-but-undeclared / declared-but-unread / catalog orphans, TODO count.
9. **Questions for the working group** — decisions that are not code: ambiguous clauses, profile scope, SLA values, which document to certify against.
10. **Suggested order of work** — target document first, then false failures, then MUSTs to FAILURE, then value checks and negatives, then CI widening, then anchors and summaries.
11. **Method and caveats** — what was reviewed, which claims were re-verified by hand, what was not changed.

Terminal summary: under 500 words, verdict first, the artifact link, the three headline groups, no code in prose.
