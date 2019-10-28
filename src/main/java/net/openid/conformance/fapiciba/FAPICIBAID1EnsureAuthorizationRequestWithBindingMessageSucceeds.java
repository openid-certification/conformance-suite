package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.client.AddBindingMessageToAuthorizationEndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-ensure-authorization-request-with-binding-message-succeeds",
	displayName = "FAPI-CIBA-ID1: Test with a binding message of '1234', the server must authenticate successfully",
	summary = "This test tries sending a binding message of '1234' to authorization endpoint request, the server must authenticate successfully.",
	profile = "FAPI-CIBA-ID1",
	configurationFields = {
		"server.discoveryUrl",
		"client.scope",
		"client.jwks",
		"client.hint_type",
		"client.hint_value",
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
public class FAPICIBAID1EnsureAuthorizationRequestWithBindingMessageSucceeds extends AbstractFAPICIBAID1 {

	@Override
	protected void createAuthorizationRequest() {
		super.createAuthorizationRequest();

		callAndStopOnFailure(AddBindingMessageToAuthorizationEndpointRequest.class, "CIBA-7.1");
	}
}
