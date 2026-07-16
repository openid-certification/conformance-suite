# Add the Brazil CIBA DCR happy path and gate negative metadata coverage

## Background

Joseph confirmed on 2026-07-16 that the OFBR FAPI-CIBA work should include an FAPI1-style DCR happy path in the RP test plan and DCR coverage on the OP side. He also confirmed that the OP path must retrieve the Brazil software statement rather than relying on the generic CIBA dynamic-client setup.

That two-sided positive path is implemented and proven suite-to-suite. This does not by itself decide whether every Brazil OP module must reject static scheduling or whether a deliberate negative registration with `backchannel_user_code_parameter=true` must be published.

## Relevant profile areas

- Positive DCR is confirmed on both OP and RP certification surfaces
- Ping is the supported Brazil CIBA delivery mode
- Dynamic registration with `backchannel_user_code_parameter=true` remains a separately gated negative test
- Brazil discovery metadata must not advertise unsupported non-ping delivery modes

## Scope

- Preserve the implemented OP dynamic-client path that obtains a Directory token and SSA, registers over mTLS with `jwks_uri`, and then completes the normal Brazil CIBA flow.
- Preserve the dedicated RP happy-path module that validates the Directory SSA plus OFBR and CIBA registration metadata before accepting the client.
- Keep the positive path close to the existing OFBR FAPI1 Advanced DCR request shape, assertions, and configuration style.
- Keep existing static Brazil regression pairings until the dynamic-only scheduling gate is answered explicitly.
- Add a negative DCR test that mutates only `backchannel_user_code_parameter=true` only if beta1 certification requires it.
- If the negative test is scoped, validate HTTP 400 `invalid_client_metadata`.
- Add or correct Brazil discovery checks for non-ping delivery mode advertisement.

## Acceptance criteria

- The requirement matrix distinguishes implemented positive DCR capability, published coverage, and remaining policy gates.
- Registration with ping mode, notification endpoint, PS256 request signing, `backchannel_user_code_parameter=false`, and Brazil-required ID Token encryption metadata/key material succeeds on both test surfaces.
- The focused suite-to-suite pairing reaches successful `/resources` completion without a preconfigured client id.
- Existing static Brazil regression pairings remain available pending the dynamic-only scheduling decision.
- If negative DCR is in scope, dynamic registration with only `backchannel_user_code_parameter=true` changed fails with `invalid_client_metadata`.
- Discovery metadata advertising unsupported non-ping delivery modes fails Brazil discovery checks.

## Out of scope

- Dynamic Client Management update behavior, which is tracked separately.
- Using DCR to fix suite-vs-suite static configuration mismatches.
- Building a new CIBA-specific DCR framework when existing OFBR DCR patterns can be reused.
- Generic non-Brazil CIBA delivery-mode behavior.
- Enforcing dynamic-only scheduling across the complete Brazil OP plan without a separate policy answer.
