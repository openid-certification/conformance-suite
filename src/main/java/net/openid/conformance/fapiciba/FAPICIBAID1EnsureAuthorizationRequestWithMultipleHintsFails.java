package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.client.AddMultipleHintsToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddScopeToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CreateEmptyAuthorizationEndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-ensure-authorization-request-with-multiple-hints-fails",
	displayName = "FAPI-CIBA-ID1: Try sending two hints value to authorization endpoint request, should return an error",
	summary = "This test should return an error that try sending two hints value to authorization endpoint request",
	profile = "FAPI-CIBA-ID1",
	configurationFields = {
		"server.discoveryUrl",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)
public class FAPICIBAID1EnsureAuthorizationRequestWithMultipleHintsFails extends AbstractFAPICIBAID1EnsureSendingInvalidBackchannelAuthorizationRequest {

	@Override
	protected void createAuthorizationRequest() {

		callAndStopOnFailure(CreateEmptyAuthorizationEndpointRequest.class);
		callAndStopOnFailure(AddScopeToAuthorizationEndpointRequest.class, "CIBA-7.1");
		callAndStopOnFailure(AddMultipleHintsToAuthorizationEndpointRequest.class, "CIBA-7.2-3");

		// The spec also defines these parameters that we don't currently set:
		// acr_values
		// binding_message
		// user_code
		// requested_expiry

		modeSpecificAuthorizationEndpointRequest();

		performProfileAuthorizationEndpointSetup();

	}

}
