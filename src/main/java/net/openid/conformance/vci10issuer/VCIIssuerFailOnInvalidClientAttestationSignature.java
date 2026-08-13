package net.openid.conformance.vci10issuer;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.vci10issuer.condition.clientattestation.VCIInvalidateClientAttestationSignature;

/**
 * Negative test that verifies the authorization server properly rejects client attestations
 * with invalid signatures.
 *
 * This test invalidates the signature on the client attestation JWT and expects the
 * authorization server to respond with an invalid_client error. The check happens at the PAR
 * endpoint for the authorization code grant and at the token endpoint for the pre-authorization
 * code grant (which skips PAR).
 *
 * This test is only applicable when client_auth_type=client_attestation is selected.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth">OAuth 2.0 Attestation-Based Client Authentication</a>
 */
@PublishTestModule(
	testName = "oid4vci-1_0-issuer-fail-invalid-client-attestation-signature",
	displayName = "OID4VCI 1.0: Issuer fail on invalid client attestation signature",
	summary = """
		This test case checks for proper error handling when a client attestation with \
		an invalid signature is submitted. The test sends a client attestation JWT where \
		the signature has been modified to be invalid. The authorization server must reject \
		this with an invalid_client error — at the PAR endpoint for the authorization code \
		grant, or at the token endpoint if PAR is not in use.""",
	profile = "OID4VCI-1_0"
)
public class VCIIssuerFailOnInvalidClientAttestationSignature extends AbstractVCIIssuerClientAttestationNegativeTest {

	@Override
	protected void afterClientAttestationGenerated() {
		super.afterClientAttestationGenerated();

		// Invalidate the client attestation signature
		callAndContinueOnFailure(VCIInvalidateClientAttestationSignature.class, Condition.ConditionResult.INFO, "OAuth2-ATCA07-1");
	}
}
