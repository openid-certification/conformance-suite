# Gate Brazil CIBA DCM metadata coverage

## Background

Open Finance Brasil CIBA beta1 names both Dynamic Client Registration and Dynamic Client Management for CIBA metadata restrictions. The two-sided positive DCR happy path is implemented, but Joseph's 2026-07-16 confirmation did not separately activate DCM coverage.

The suite can technically test DCM against an authorization server under test: complete DCR, create a full RFC7592 PUT document, attach a fresh software statement if required, authenticate with the registration access token and mTLS certificate, and assert the response. There is no symmetric RP-side test because the standard suite interaction cannot cause an external client under test to issue that PUT. Cleanup DELETE after the happy flow is not DCM coverage.

## Relevant profile areas

- Confirm whether OP-side DCM is part of the active Brazil CIBA certification scope
- If scoped, CIBA metadata updates must not enable `backchannel_user_code_parameter=true`
- If scoped, Brazil clients must remain ping-mode clients
- Keep the RP-side trigger limitation explicit

## Scope

- Start only after the DCM publication gate confirms OP-side coverage is required.
- Reuse the existing Brazil DCR/DCM request-building and client-configuration endpoint conditions.
- Complete valid DCR, construct a full authenticated PUT from the registration response, refresh the software statement when required, and change only `backchannel_user_code_parameter` to true.
- Assert HTTP 400, JSON content type, and `error=invalid_client_metadata`.
- Keep the original registered client usable for cleanup after the negative update.
- Do not add an RP-side module or build a generic trigger framework merely to claim symmetric coverage.

## Acceptance criteria

- The requirement matrix records OP-side DCM as technically buildable but not published pending an explicit gate.
- The requirement matrix records that RP-side DCM cannot be triggered through the standard client-under-test interaction.
- If the gate is activated, updating a client to `backchannel_user_code_parameter=true` fails with HTTP 400 JSON and `invalid_client_metadata`.
- No note treats management credentials or cleanup DELETE as DCM coverage.

## Out of scope

- Positive DCR behavior, which is implemented and tracked separately.
- RP-side DCM trigger/framework work.
- Building a full generic DCM framework.
