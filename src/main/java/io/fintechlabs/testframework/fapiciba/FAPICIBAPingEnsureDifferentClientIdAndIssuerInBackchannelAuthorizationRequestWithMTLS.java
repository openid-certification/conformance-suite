package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.condition.client.AddClientNotificationTokenToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.condition.client.CreateRandomClientNotificationToken;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-ping-ensure-different-client-id-and-issuer-in-backchannel-authorization-request-with-mtls",
	displayName = "FAPI-CIBA: Ping mode ensure different client_id and issuer in backchannel authorization request (MTLS client authentication)",
	summary = "This test passes a different client_id and issuer in the backchannel authorization parameters to the one inside the signed request object. The backchannel authorisation server returned an error message that the client is invalid.",
	profile = "FAPI-CIBA",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"client.hint_type",
		"client.hint_value",
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
public class FAPICIBAPingEnsureDifferentClientIdAndIssuerInBackchannelAuthorizationRequestWithMTLS extends AbstractFAPICIBAEnsureDifferentClientIdAndIssuerInBackchannelAuthorizationRequestWithMTLS {
	@Override
	protected void modeSpecificAuthorizationEndpointRequest() {
		callAndStopOnFailure(CreateRandomClientNotificationToken.class, "CIBA-7.1");

		callAndStopOnFailure(AddClientNotificationTokenToAuthorizationEndpointRequest.class, "CIBA-7.1");
	}
}
