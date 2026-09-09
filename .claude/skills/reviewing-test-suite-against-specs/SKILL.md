---
name: reviewing-test-suite-against-specs
description: Use when asked whether a whole conformance-suite test family (a spec's plans, e.g. SSF/CAEP, AuthZEN, Federation, VCI) is correct and complete against its specifications, has missing or wrong test cases, or is ready to launch a certification programme — not for reviewing a branch diff (that is the openid-review skill).
---

# Reviewing a test family against its specs

Deep, whole-family audit of one test family in this repository. Answers three questions: which tests are **wrong**, which are **missing**, and whether the plans are **ready for certification**. Output is a verdict-first report, published as an artifact, backed by verbatim spec clauses and hand-verified file references.

Paths below are relative to the repository root. The helper script is `.claude/skills/reviewing-test-suite-against-specs/fetch-spec.py` (Python 3 stdlib only). Put downloaded spec text and other working files under `tmp/spec-review/<family>/` (`tmp/` is gitignored) or the session scratchpad.

## Step 1 — Inventory the family (you, before any fan-out)

- Package under `src/main/java/net/openid/conformance/<family>/`: plans (`@PublishTestPlan`), modules, conditions, variants, `*_UnitTest` files.
- **Shared and inherited code.** Families rarely live in one package. Find what the abstract module classes extend (`grep -n 'extends' <family>/*.java`) and which condition/sequence packages they import (`grep -rhoE '^import net\.openid\.conformance\.(condition|sequence|fapi2spfinal|...)\.[A-Za-z0-9_.]+;' <family>/ | sort | uniq -c`). VCI, for example, keeps most of its behaviour in `condition/as/VCI*`, `condition/client/VCI*`, `fapi2spfinal/VCI*ProfileBehavior` and the FAPI2 base classes. The reviewer briefs must name those files explicitly or the reviewers will not look there.
- Requirement-link prefixes in `src/main/java/net/openid/conformance/export/LogEntryHelper.java` (`specLinks`). Count which prefixes the family actually uses with a regex that allows hyphens inside the prefix — `grep -rhoE '"[A-Za-z0-9-]+-[0-9]+(\.[0-9]+)*"' <family>/ | sed -E 's/"(.*)-[0-9]+(\.[0-9]+)*"/\1/' | sort | uniq -c` — because `OID4VCI-1FINAL-8.2` style tags are missed by a single-hyphen pattern. Note **which document version each prefix links to**; this is the single most common source of wrong anchors. Bare `"SSF"` strings are not tags.
- CI: the family's function in `.gitlab-ci/run-tests.sh` (`grep -n 'make.*Tests()'` to get the exact casing, e.g. `makeSsfTests`, `makeVcTests`) — grep it for **every** config path it names (families spread configs over `scripts/test-configs-<family>/`, `scripts/test-configs-rp-against-op/`, and the private repo `../conformance-suite-private/`); `.gitlab-ci/expected-{failures,skips}-<family>.json` (read every entry's comment; wildcard `variant: "*"` entries can mask real bugs); the `<family>_test` job in `.gitlab-ci.yml`. Report which mechanism governs the untested-module report: the `--show-untested-test-modules` flag in run-tests.sh, or `scripts/run-test-plan.py` stripping the family's modules unconditionally (the latter makes the flag moot; it does this for OID4VCI wallet modules).
- Recent history: `git log --oneline -- src/main/java/.../<family> | head -60`.
- Certification state of each plan: whether it overrides `TestPlan.certificationProfileName(VariantSelection)`. Without a non-empty override the server returns 422 for a certification package and the display name must carry exactly one of: `(not part of certification program)` (never), `(not part of certification program - use the <X> plan to certify)` (route exists elsewhere), `(not currently part of certification program - please email certification@oidf.org)` (may open later). Record which each plan carries; a plan that says "use the X plan to certify" where X itself has no profile is a finding.
- Certification status outside the repo: start from `https://openid.net/how-to-certify-your-implementation/` and use `fetch-spec.py links <url> <substring>` to get the family page's real URL (the text converter drops hrefs, and the URL patterns vary: `ssf_testing`, `federation_testing`, but `conformance-testing-for-openid-for-verifiable-credential-issuance`). Also check the WG GitHub repo's issues/PRs for `certification`, `conformance`, `profile`. Do not guess URLs.

## Step 2 — Get the spec text verbatim

Never review from WebFetch summaries or memory. Download every document the code tags (all `specLinks` prefixes the family uses, plus the RFCs it builds on) as plain text:

```bash
S=.claude/skills/reviewing-test-suite-against-specs/fetch-spec.py
python3 $S fetch \
  vci-final https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0-final.html \
  rfc9449 https://www.rfc-editor.org/rfc/rfc9449.txt --out tmp/spec-review/vci/specs
python3 $S diff-headings specs/vci-final.txt specs/vci-10-wg.txt
python3 $S map-tags OID4VCI-1FINAL src/main/java/net/openid/conformance/vci10issuer specs/vci-final.txt
python3 $S links https://openid.net/how-to-certify-your-implementation/ verifiable
```

Fetch **both** the version LogEntryHelper links to and the latest published version on openid.net (Final > Implementer's Draft > WG draft). Published openid.net URLs follow `openid-<spec>-1_0-final.html`, `-ID2.html`, `-ID1.html`, `-NN.html` (numbered draft), and bare `-1_0.html` (latest alias; recommend the immutable form when they are identical). Working-group draft URLs are not guessable — read them out of the spec repo's README (`curl -s https://raw.githubusercontent.com/openid/<Repo>/main/README.md | grep -oE 'https://openid\.github\.io/[^ )]+'`); repos publish `-1_0-wg-draft` (errata) and `-1_1-wg-draft` separately, and renamed repos leave redirect stubs that fetch as a six-line page. Read the HTTP status line `fetch` prints (a 404 page still converts to text; on an error the script deletes any stale file of that name). The MUST/SHOULD counts are a sanity signal, not a measure.

For IETF drafts the code links, find the current revision from the datatracker page (`fetch <name>-dt https://datatracker.ietf.org/doc/<draft-name>/` then grep `Latest revision`) and fetch `https://www.ietf.org/archive/id/<draft-name>-NN.txt` for both the linked and the current revision. Then find which revision the **certification target** pins: grep the References section of the Final spec and of any profile (HAIP) for `draft-ietf-...-NN`; a profile's "versions mentioned here override" clause wins. The linked version is right if it matches the pinned one, whatever the datatracker says.

Run `diff-headings` between versions, then `map-tags` for each prefix the family uses against every fetched version. Reading the table: a `—` in every column is a section that does not exist at all (usually a typo such as 7.2.1 for 7.2, or a pre-Final section number); a `—` in one column, or titles that differ across columns, is version drift and the link lands on the wrong text. Hand the table to the reviewers. `headings` output is tab-separated (number, title); it ignores indented numbered lines so RFC list items are not mistaken for sections.

## Step 3 — Fan out four reviewers in parallel

Use the brief in `reviewer-brief.md`, one Agent per dimension, all read-only, all given the spec file paths and the explicit file list from Step 1 (including the shared code):

1. **Role A** (suite emulates the counterparty, tests the real server/issuer/transmitter/PDP).
2. **Role B** (suite emulates the server, tests the real client/wallet/receiver). This reviewer must also look at the **emulated side**, but only for defects that make a correct implementation fail, hang, or be misled (an AS that issues tokens after a failed client authentication, a token response missing a REQUIRED field the wallet needs next, a transaction id that is never invalidated). The emulator is allowed to be non-conformant on purpose and does not need polishing beyond what drives the scenario (AGENTS.md, "Emulated side vs side under test"); cosmetic deviations in what it sends are not findings. In the VCI run the load-bearing findings were of the first kind.
3. **Payload / profile validation** (event or credential payloads, the certification/interop profile, sibling specs like RISC or SD-JWT VC), including the version-drift question from Step 2 and, where the suite mints artefacts, whether the minted artefacts are themselves conformant.
4. **Infrastructure**: CI coverage matrix (which modules and variant values never run), unit-test coverage per condition, config fields read vs declared vs `src/main/resources/static/js/config-field-catalog.json`, module summaries, open GitLab issues/MRs (`~/.gitlab-token` if present, read-only), TODOs, docs, ownership.

Each reviewer returns: wrong checks (severity, file:line, verbatim clause), missing tests (clause + feasibility), anchor errors, unit-test gaps, spec ambiguities.

## Step 4 — Verify before you report

Subagent line numbers are unreliable even when the brief tells them to grep: in the VCI run all four reviewers cited wrong lines for some findings (one cited line 371 of a 70-line file). Before publishing, open the source for **every critical finding** and confirm the behaviour and the clause yourself. Cite `file:line` only where you verified it; otherwise cite the file. Also confirm publication status of any draft you name (curl the URL), and say which library-internal claims (e.g. what multipaz or an Authlete decoder swallows) you did not re-check.

## Step 5 — Report, publish, remember

Follow `report-contract.md`: verdict first, the three questions answered in one line each, then *conformant implementations fail* (wrong tests), *non-conformant implementations pass* (missing checks), missing-test tables with feasibility, anchor table, infrastructure, **questions for the WG**, suggested order. Publish as an HTML artifact (load `artifact-design` first). Save a `project` memory note with the verdict and blocker list, linked to sibling reviews.

## Common mistakes

| Mistake | Fix |
|---|---|
| Grepping `static/` for the requirement-link map | It is `LogEntryHelper.java` |
| Reviewing only the family package | Trace base classes and imported condition packages (Step 1) |
| Counting tags with a single-hyphen regex | Use the hyphen-tolerant grep in Step 1 |
| Fetching only the main spec | Fetch every tagged prefix and the underlying RFCs |
| Treating the newest IETF draft as the target | The target is the revision the Final spec or profile pins |
| Linking to a WG head that has moved on | `diff-headings` published vs linked; recommend the immutable URL |
| Guessing WG-draft or certification-page URLs | Repo README and `links` on the how-to-certify page |
| Assuming the emulator is right | Check whether a correct implementation survives the emulator; report only defects that break one, not every deviation |
| Treating a skip as harmless | A skip that lets a mandatory behaviour go unexercised under the selected profile is a certification gap (AGENTS.md, "Skips vs failures") |
| Proposing a fix that was already rejected | Check "Deliberate non-features" in AGENTS.md before suggesting it |
| Reporting subagent line numbers unverified | Step 4 |
| Grading a MUST as WARNING because "the check exists" | Severity must match clause strength; a WARNING is a pass at certification |
| "Missing" list without feasibility | Say which need a real trigger, multi-tenant setup, or a WG decision |
| Trusting a green pipeline as coverage | Build the module × variant matrix; check what the untested report strips |
