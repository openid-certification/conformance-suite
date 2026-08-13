package net.openid.conformance.vci10issuer;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VariantSetup;
import net.openid.conformance.vci10issuer.condition.clientattestation.AddClientAttestationClientAuthWithInvalidPopSignature;

/**
 * Negative test that verifies the authorization server properly rejects client attestation
 * proof-of-possession JWTs with invalid signatures.
 * <p>
 * This test invalidates the signature on the client attestation pop JWT and expects the
 * authorization server to respond with an invalid_client error — at the PAR endpoint for the
 * authorization code grant, or at the token endpoint for the pre-authorized code grant.
 * <p>
 * Note: This test only runs when client_auth_type=client_attestation is selected.
 * If a different client authentication method is used, the test skips.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth">OAuth 2.0 Attestation-Based Client Authentication</a>
 */
@PublishTestModule(
	testName = "oid4vci-1_0-issuer-fail-invalid-client-attestation-pop-signature",
	displayName = "OID4VCI 1.0: Issuer fail on invalid client attestation pop signature",
	summary = """
		This test case checks for proper error handling when a client attestation \
		proof-of-possession JWT with an invalid signature is submitted. The test sends a \
		client attestation pop JWT where the signature has been modified to be invalid — to \
		the PAR endpoint for the authorization code grant, or to the token endpoint for the \
		pre-authorized code grant (which does not use PAR). \
		The authorization server must reject this request with an invalid_client error. \
		Note: This test requires client_auth_type=client_attestation variant. \
		If a different client authentication method is used, the test will be skipped.""",
	profile = "OID4VCI-1_0"
)
public class VCIIssuerFailOnInvalidClientAttestationPopSignature extends AbstractVCIIssuerClientAttestationNegativeTest {

	@VariantSetup(parameter = ClientAuthType.class, value = "client_attestation")
	@Override
	public void setupClientAttestation() {
		// Use the modified sequence that invalidates the pop signature
		addClientAuthentication = AddClientAttestationClientAuthWithInvalidPopSignature.class;
	}
}
