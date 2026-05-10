package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddArrayContainingIssuerAndAnotherValueAsAudToClientAuthenticationAssertionClaims;
import net.openid.conformance.condition.client.CallPAREndpoint;
import net.openid.conformance.condition.client.CheckErrorFromParEndpointResponseErrorInvalidClientOrInvalidRequest;
import net.openid.conformance.condition.client.CreateClientAuthenticationAssertionClaimsWithIssAudience;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400or401;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionWithIssAudAndAddToPAREndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
		testName = "fapi2-security-profile-final-par-test-array-as-audience-fails",
		displayName = "FAPI2-Security-Profile-Final: ensure jwt client assertions with array as audience fails at the par endpoint",
		summary = "This test checks if PAR endpoint does not accept a client assertion with an array as audience. We expect the request to fail and a server response with status code 400 or 401 with error codes invalid_request or invalid_client.",
		profile = "FAPI2-Security-Profile-Final"
)
@VariantNotApplicable(parameter = ClientAuthType.class, values = {
	"mtls", "client_attestation"
})
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = { "fapi_client_credentials_grant" })
public class FAPI2SPFinalPAREndpointAsArrayAudienceFails extends AbstractFAPI2SPFinalServerTestModule {
	@Override
	protected void addClientAuthenticationToPAREndpointRequest() {
		mapClientAuthKeys("pushed_authorization_request_form_parameters", "pushed_authorization_request_endpoint_request_headers");

			call(new CreateJWTClientAuthenticationAssertionWithIssAudAndAddToPAREndpointRequest().insertAfter(
					CreateClientAuthenticationAssertionClaimsWithIssAudience.class,
					condition(AddArrayContainingIssuerAndAnotherValueAsAudToClientAuthenticationAssertionClaims.class).requirements("PAR-2")));

		unmapClientAuthKeys();
	}

	@Override
	protected void processParResponse() {
		env.mapKey("endpoint_response", CallPAREndpoint.RESPONSE_KEY);
		callAndContinueOnFailure(EnsureHttpStatusCodeIs400or401.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.3.2.1-8");
		callAndContinueOnFailure(CheckErrorFromParEndpointResponseErrorInvalidClientOrInvalidRequest.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.3.2.1-8");
		env.unmapKey("endpoint_response");

		fireTestFinished();
	}
}
