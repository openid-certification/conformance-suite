package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddIatNbfExpOver60SecondsInTheFutureToClientAuthenticationAssertionClaims;
import net.openid.conformance.condition.client.CallPAREndpoint;
import net.openid.conformance.condition.client.CheckErrorFromParEndpointResponseErrorInvalidClientOrInvalidRequest;
import net.openid.conformance.condition.client.CreateClientAuthenticationAssertionClaims;
import net.openid.conformance.condition.client.CreateClientAuthenticationAssertionClaimsWithIssAudience;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400or401;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionAndAddToPAREndpointRequest;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionWithIssAudAndAddToPAREndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-final-par-ensure-jwt-client-assertions-nbf-over-60-seconds-in-the-future-fails",
	displayName = "FAPI2-Security-Profile-Final: ensure jwt client assertions with nbf over 60 seconds in the future fails at the par endopint",
	summary = "This test checks the clock skew handling of the PAR endpoint The test makes a PAR request with a client assertion with iat, nbf and exp set > 60s into the future. We expect the request to fail and a server response with status code 400 or 401 with error codes invalid_request or invalid_client.",
	profile = "FAPI2-Security-Profile-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)
@VariantNotApplicable(parameter = ClientAuthType.class, values = {
	"mtls"
})
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = { "fapi_client_credentials_grant" })
public class FAPI2SPFinalPAREnsureJWTClientAssertionWithIatNbfOver60SecondsInTheFutureFails extends AbstractFAPI2SPFinalServerTestModule {
	@Override
	protected void addClientAuthenticationToPAREndpointRequest() {
		if (getVariant(FAPI2FinalOPProfile.class) == FAPI2FinalOPProfile.CBUAE){
			call(new CreateJWTClientAuthenticationAssertionWithIssAudAndAddToPAREndpointRequest().insertAfter(
					CreateClientAuthenticationAssertionClaimsWithIssAudience.class,
					condition(AddIatNbfExpOver60SecondsInTheFutureToClientAuthenticationAssertionClaims.class).requirements("PAR-2", "RFC7519-4.1.5", "RFC7519-4.1.6", "FAPI2-SP-FINAL-5.3.2.1")));
		} else {
			call(new CreateJWTClientAuthenticationAssertionAndAddToPAREndpointRequest().insertAfter(
					CreateClientAuthenticationAssertionClaims.class,
					condition(AddIatNbfExpOver60SecondsInTheFutureToClientAuthenticationAssertionClaims.class).requirements("PAR-2", "RFC7519-4.1.5", "RFC7519-4.1.6", "FAPI2-SP-FINAL-5.3.2.1")));
		}
	}

	@Override
	protected void processParResponse() {

		env.mapKey("endpoint_response", CallPAREndpoint.RESPONSE_KEY);
		callAndContinueOnFailure(EnsureHttpStatusCodeIs400or401.class, Condition.ConditionResult.FAILURE, "PAR-2.3", "RFC6749-4.1.2.1", "RFC6749-5.2");
		callAndContinueOnFailure(CheckErrorFromParEndpointResponseErrorInvalidClientOrInvalidRequest.class, Condition.ConditionResult.FAILURE, "PAR-2.3", "RFC6749-4.1.2.1", "RFC6749-5.2");
		env.unmapKey("endpoint_response");

		fireTestFinished();
	}
}
