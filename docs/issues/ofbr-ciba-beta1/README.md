# Open Finance Brasil CIBA beta1 work overview

## Purpose

This folder contains the working overview, requirement matrix, and GitLab-ready issue drafts for aligning FAPI-CIBA Open Finance Brasil support with the beta1 CIBA profile.

The current suite already has an `openbanking_brazil` CIBA profile and suite-vs-suite OP/RP pairings. The beta1 inputs require a structured pass across OP tests, RP tests, scoped DCR/DCM metadata, notification behavior, and CI coverage before the certification engine is treated as current.

## Current Readiness

- The active happy path should be Customer Data API based: client credentials, Data Consent, CIBA with `login_hint` as the consent identifier, ping notification, token redemption, and `/resources`.
- Payments are not part of the active Brazil CIBA certification scope for this round.
- Brazil CIBA remains ping-primary. Controlled poll fallback should be tested only inside the ping profile, not by enabling `ciba_mode=poll` for Brazil certification.
- Joseph confirmed on 2026-07-16 that a narrow DCR happy path is needed on both OP and RP surfaces. It is implemented and proven suite-to-suite using the OFBR Directory SSA flow.
- Static Brazil OP regression coverage remains active until a separate dynamic-only scheduling decision is made. Negative initial-DCR and OP-side DCM PUT tests remain separately gated; RP-side DCM cannot be triggered through the standard suite interaction.
- GitLab issue creation was not performed in this run because `glab` is not installed and the configured remote is GitLab-style rather than GitHub. These markdown files are the durable fallback.

## Filing Notes

- [Planning artifact: overview and requirement matrix](001-overview-requirement-matrix.md) is not intended to become a GitLab issue.
- Use the remaining drafts as GitLab issue bodies, starting with `002`.

## Issue Index

1. [Align Brazil CIBA happy path to Data Consent and resources](002-happy-path-data-consent-resources.md)
2. [Add OP tests for forbidden request parameters](003-op-forbidden-request-parameters.md)
3. [Add the Brazil CIBA DCR happy path and gate negative metadata coverage](004-op-dcr-metadata-restrictions.md)
4. [Gate Brazil CIBA DCM metadata coverage](005-dcm-metadata-assessment.md)
5. [Align Brazil binding_message coverage](006-binding-message-coverage.md)
6. [Add RP tests for forbidden sender behavior](007-rp-forbidden-sender-behavior.md)
7. [Add OP ping retry coverage](008-op-ping-retry.md)
8. [Add RP notification authenticity and idempotency coverage](009-rp-notification-authenticity-idempotency.md)
9. [Add controlled poll fallback coverage under ping mode](010-controlled-poll-fallback.md)
10. [Update Brazil CIBA CI pairings and expected outcomes](011-ci-pairings.md)
11. [Track participant dry-run validation](012-participant-dry-run.md)

## Deferred

- Payments API CIBA certification behavior.
- Dynamic-only enforcement across the complete Brazil OP plan until the policy gate is answered.
- Negative initial-DCR and OP-side DCM PUT coverage until their certification gates are answered.
- RP-side DCM trigger work; the standard suite interaction cannot force an external RP to issue a management PUT.
- Participant logistics, support playbooks, and certification-window operations.
- Generic FAPI-CIBA poll-mode changes outside Brazil fallback coverage.

## Sensitive Data Handling

Use synthetic data in issue drafts and logs. Do not persist real access tokens, refresh tokens, client notification tokens, `auth_req_id`, consent IDs, CPF/CNPJ values, binding messages, or financial payloads unless the value is deliberately synthetic and labeled as such.
