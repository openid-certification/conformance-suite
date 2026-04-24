package net.openid.conformance.vci10issuer;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallPAREndpoint;
import net.openid.conformance.condition.client.CheckErrorFromParEndpointResponseErrorInvalidClientOrInvalidRequestOrInvalidClientAttestation;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400or401;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VariantNotApplicable;
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
		Sends a PAR request with a client attestation JWT that omits the 'sub' claim. \
		The authorization server must reject the request with an invalid_client, \
		invalid_request, or invalid_client_attestation error. \
		Only applicable when client_auth_type=client_attestation.""",
	profile = "OID4VCI-1_0"
)
@VariantNotApplicable(parameter = ClientAuthType.class, values = {"mtls", "private_key_jwt"})
public class VCIIssuerFailOnClientAttestationNoSub extends AbstractVCIIssuerTestModule {

	@Override
	protected void afterClientAttestationGenerated() {
		super.afterClientAttestationGenerated();

		callAndStopOnFailure(CreateClientAttestationJwtWithoutSub.class, Condition.ConditionResult.FAILURE,
			"OAuth2-ATCA07-5.1");
	}

	@Override
	protected void processParResponse() {
		env.mapKey("endpoint_response", CallPAREndpoint.RESPONSE_KEY);
		callAndContinueOnFailure(EnsureHttpStatusCodeIs400or401.class, Condition.ConditionResult.FAILURE, "OAuth2-ATCA07-6.2");
		callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE, "OAuth2-ATCA07-6.2");
		callAndContinueOnFailure(CheckErrorFromParEndpointResponseErrorInvalidClientOrInvalidRequestOrInvalidClientAttestation.class, Condition.ConditionResult.FAILURE, "OAuth2-ATCA07-6.2");
		env.unmapKey("endpoint_response");

		fireTestFinished();
	}
}
