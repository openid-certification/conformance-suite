package net.openid.conformance.vci10issuer;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallPAREndpoint;
import net.openid.conformance.condition.client.CheckErrorFromParEndpointResponseErrorInvalidClientOrInvalidRequest;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400or401;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VCIClientAuthType;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantSetup;
import net.openid.conformance.vci10issuer.condition.clientattestation.AddClientAttestationClientAuthWithInvalidPopSignature;

/**
 * Negative test that verifies the authorization server properly rejects client attestation
 * proof-of-possession JWTs with invalid signatures.
 * <p>
 * This test invalidates the signature on the client attestation pop JWT and expects the
 * authorization server to respond with an invalid_client error at the PAR endpoint.
 * <p>
 * Note: This test only runs when client_auth_type=client_attestation is selected.
 * If a different client authentication method is used, the test skips.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth">OAuth 2.0 Attestation-Based Client Authentication</a>
 */
@PublishTestModule(
	testName = "oid4vci-1_0-issuer-fail-invalid-client-attestation-pop-signature",
	displayName = "OID4VCI 1.0: Issuer fail on invalid client attestation pop signature",
	summary = "This test case checks for proper error handling when a client attestation " +
		"proof-of-possession JWT with an invalid signature is submitted. The test sends a PAR " +
		"request with a client attestation pop JWT where the signature has been modified to be invalid. " +
		"The authorization server must reject this request with an invalid_client error. " +
		"Note: This test requires client_auth_type=client_attestation variant. " +
		"If a different client authentication method is used, the test will be skipped.",
	profile = "OID4VCI-1_0"
)
@VariantNotApplicable(parameter = VCIClientAuthType.class, values = {"mtls", "private_key_jwt"})
public class VCIIssuerFailOnInvalidClientAttestationPopSignature extends VCIIssuerHappyFlow {

	@VariantSetup(parameter = VCIClientAuthType.class, value = "client_attestation")
	@Override
	public void setupClientAttestation() {
		// Use the modified sequence that invalidates the pop signature
		addTokenEndpointClientAuthentication = AddClientAttestationClientAuthWithInvalidPopSignature.class;
		addParEndpointClientAuthentication = AddClientAttestationClientAuthWithInvalidPopSignature.class;
	}

	@Override
	protected void processParResponse() {
		// Expect an error response when client attestation pop signature is invalid
		// The PAR endpoint should reject with invalid_client
		env.mapKey("endpoint_response", CallPAREndpoint.RESPONSE_KEY);
		callAndContinueOnFailure(EnsureHttpStatusCodeIs400or401.class, Condition.ConditionResult.FAILURE, "OAuth2-ATCA07-1");
		callAndContinueOnFailure(CheckErrorFromParEndpointResponseErrorInvalidClientOrInvalidRequest.class, Condition.ConditionResult.FAILURE, "OAuth2-ATCA07-1");
		env.unmapKey("endpoint_response");

		fireTestFinished();
	}
}
