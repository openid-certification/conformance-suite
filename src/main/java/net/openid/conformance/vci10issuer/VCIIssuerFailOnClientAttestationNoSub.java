package net.openid.conformance.vci10issuer;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.vci10issuer.condition.clientattestation.CreateClientAttestationJwtWithoutSub;

/**
 * Negative test that verifies the authorization server rejects client attestations without
 * a 'sub' claim. Mirrors the private_key_jwt test
 * FAPI2SPFinalEnsureClientAssertionWithNoSubFails for client_attestation.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth-07">OAuth 2.0 Attestation-Based Client Authentication</a>
 */
@PublishTestModule(
	testName = "oid4vci-1_0-issuer-fail-client-attestation-no-sub",
	displayName = "OID4VCI 1.0: Issuer fail on client attestation without sub",
	summary = """
		Sends a client attestation JWT that omits the 'sub' claim — to the PAR endpoint for \
		the authorization code grant, or to the token endpoint for the pre-authorized code \
		grant (which does not use PAR). The authorization server must reject the request \
		with an invalid_client, invalid_request, or invalid_client_attestation error. \
		Only applicable when client_auth_type=client_attestation.""",
	profile = "OID4VCI-1_0"
)
public class VCIIssuerFailOnClientAttestationNoSub extends AbstractVCIIssuerClientAttestationNegativeTest {

	@Override
	protected void afterClientAttestationGenerated() {
		super.afterClientAttestationGenerated();

		callAndStopOnFailure(CreateClientAttestationJwtWithoutSub.class, Condition.ConditionResult.FAILURE,
			"OAuth2-ATCA07-5.1");
	}
}
