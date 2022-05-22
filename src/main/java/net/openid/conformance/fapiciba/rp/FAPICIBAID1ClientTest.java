package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-test-plan",
	displayName = "FAPI-CIBA-ID1: client test",
	summary = "Tests a 'happy path' flow; the client should perform OpenID discovery from the displayed discoveryUrl, call the backchannel endpoint and either poll the token endpoint or wait to get pinged, then use the access token from the token endpoint response in a GET request to the accounts endpoint displayed.",
	profile = "FAPI-CIBA-ID1",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.backchannel_client_notification_endpoint",
		"client.certificate",
		"client.jwks"
	}
)

public class FAPICIBAID1ClientTest extends AbstractFAPICIBAID1ClientTest {

	@Override
	protected void addCustomValuesToIdToken(){
		//Do nothing
	}

	@Override
	protected void createBackchannelResponse() {
		callAndStopOnFailure(CreateBackchannelEndpointResponse.class);
	}

	@Override
	protected void createIntermediateTokenResponse() {
		callAndStopOnFailure(CreateAuthorizationPendingResponse.class);
	}

	@Override
	protected void createFinalTokenResponse() {
		callAndStopOnFailure(CreateTokenEndpointResponse.class);
	}
}
