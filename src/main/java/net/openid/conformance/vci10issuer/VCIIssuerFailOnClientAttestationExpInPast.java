package net.openid.conformance.vci10issuer;

import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.vci10issuer.condition.clientattestation.CreateClientAttestationJwt;
import net.openid.conformance.vci10issuer.condition.clientattestation.CreateClientAttestationJwtWithExpInPast;

/**
 * Negative test that verifies the authorization server rejects client attestations whose
 * 'exp' claim is in the past. Mirrors the private_key_jwt test
 * FAPI2SPFinalEnsureClientAssertionWithExpIs5MinutesInPastFails for client_attestation.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth-07">OAuth 2.0 Attestation-Based Client Authentication</a>
 */
@PublishTestModule(
	testName = "oid4vci-1_0-issuer-fail-client-attestation-exp-in-past",
	displayName = "OID4VCI 1.0: Issuer fail on client attestation with exp in the past",
	summary = """
		Sends a client attestation JWT whose 'exp' claim is 10 minutes in the past — to the \
		PAR endpoint for the authorization code grant, or to the token endpoint for the \
		pre-authorized code grant (which does not use PAR). The authorization server must \
		reject the request with an invalid_client, invalid_request, invalid_client_attestation \
		or use_fresh_attestation error.""",
	profile = "OID4VCI-1_0"
)
public class VCIIssuerFailOnClientAttestationExpInPast extends AbstractVCIIssuerClientAttestationNegativeTest {

	@Override
	protected ConditionSequence createClientAuthenticationSequence() {
		return super.createClientAuthenticationSequence()
			.replace(CreateClientAttestationJwt.class,
				condition(CreateClientAttestationJwtWithExpInPast.class).requirement("OAuth2-ATCA07-5.1"));
	}

	// An expired attestation is the one mutation for which use_fresh_attestation is a valid
	// response (OAuth2-ATCA §6.2: the attestation "is deemed to be not fresh enough").
	@Override
	protected boolean acceptsUseFreshAttestation() {
		return true;
	}
}
