package net.openid.conformance.vci10issuer;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallPAREndpoint;
import net.openid.conformance.condition.client.CheckErrorFromParEndpointResponseErrorInvalidClientOrInvalidRequest;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400or401;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VCIClientAuthType;
import net.openid.conformance.vci10issuer.condition.clientattestation.VCIInvalidateClientAttestationSignature;

/**
 * Negative test that verifies the authorization server properly rejects client attestations
 * with invalid signatures.
 *
 * This test invalidates the signature on the client attestation JWT and expects the
 * authorization server to respond with an invalid_client error at the PAR endpoint.
 *
 * Note: This test only runs when client_auth_type=client_attestation is selected.
 * If a different client authentication method is used, the test skips.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth">OAuth 2.0 Attestation-Based Client Authentication</a>
 */
@PublishTestModule(
	testName = "oid4vci-1_0-issuer-fail-invalid-client-attestation-signature",
	displayName = "OID4VCI 1.0: Issuer with invalid client attestation signature",
	summary = "This test case checks for proper error handling when a client attestation with " +
		"an invalid signature is submitted. The test sends a PAR request with a client " +
		"attestation JWT where the signature has been modified to be invalid. " +
		"The authorization server must reject this request with an invalid_client error. " +
		"Note: This test requires client_auth_type=client_attestation variant. " +
		"If a different client authentication method is used, the test will be skipped.",
	profile = "OID4VCI-1_0"
)
public class VCIIssuerFailOnInvalidClientAttestationSignature extends VCIIssuerHappyFlow {

	@Override
	public void start() {
		// Skip this test if client attestation is not used
		if (clientAuthType != VCIClientAuthType.CLIENT_ATTESTATION) {
			fireTestSkipped("This test requires client_auth_type=client_attestation variant. " +
				"Skipping because a different client authentication method is used.");
			return;
		}

		super.start();
	}

	@Override
	protected void afterClientAttestationGenerated() {
		super.afterClientAttestationGenerated();

		// Invalidate the client attestation signature
		callAndContinueOnFailure(VCIInvalidateClientAttestationSignature.class, Condition.ConditionResult.INFO, "OAuth2-ATCA07-1");
	}

	@Override
	protected void processParResponse() {
		// Expect an error response when client attestation signature is invalid
		// The PAR endpoint should reject with invalid_client
		env.mapKey("endpoint_response", CallPAREndpoint.RESPONSE_KEY);
		callAndContinueOnFailure(EnsureHttpStatusCodeIs400or401.class, Condition.ConditionResult.FAILURE, "OAuth2-ATCA07-1");
		callAndContinueOnFailure(CheckErrorFromParEndpointResponseErrorInvalidClientOrInvalidRequest.class, Condition.ConditionResult.FAILURE, "OAuth2-ATCA07-1");
		env.unmapKey("endpoint_response");

		fireTestFinished();
	}
}
