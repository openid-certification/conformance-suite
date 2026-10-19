package net.openid.conformance.vci10issuer;

import net.openid.conformance.condition.client.CreateClientAttestationProofJwt;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.vci10issuer.condition.clientattestation.CreateClientAttestationProofJwtWithWrongAud;

/**
 * Negative test that verifies the authorization server rejects client attestation
 * proof-of-possession JWTs whose 'aud' claim is not the authorization server issuer.
 * Mirrors the private_key_jwt test FAPI2SPFinalEnsureClientAssertionWithWrongAudFails for
 * client_attestation — 'aud' is carried on the PoP JWT for client_attestation, not the
 * attestation JWT itself.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth-07#section-5.2-5.2.1">OAuth 2.0 Attestation-Based Client Authentication §5.2</a>
 */
@PublishTestModule(
	testName = "oid4vci-1_0-issuer-fail-client-attestation-pop-wrong-aud",
	displayName = "OID4VCI 1.0: Issuer fail on client attestation pop with wrong aud",
	summary = """
		Sends a client attestation proof-of-possession JWT whose 'aud' claim is not the \
		authorization server issuer — to the PAR endpoint for the authorization code grant, \
		or to the token endpoint for the pre-authorized code grant (which does not use PAR). \
		The authorization server must reject the request with an invalid_client, \
		invalid_request, or invalid_client_attestation error.""",
	profile = "OID4VCI-1_0"
)
public class VCIIssuerFailOnClientAttestationPopWrongAud extends AbstractVCIIssuerClientAttestationNegativeTest {

	@Override
	protected ConditionSequence createClientAuthenticationSequence() {
		return super.createClientAuthenticationSequence()
			.replace(CreateClientAttestationProofJwt.class,
				condition(CreateClientAttestationProofJwtWithWrongAud.class).requirement("OAuth2-ATCA07-5.2"));
	}
}
