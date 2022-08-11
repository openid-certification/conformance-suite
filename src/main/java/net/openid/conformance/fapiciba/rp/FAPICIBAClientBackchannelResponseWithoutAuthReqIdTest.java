package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-no-authreqid-test",
	displayName = "FAPI-CIBA-ID1: Client test - auth_req_id not present in backchannel response",
	summary = "The client should perform OpenID discovery from the displayed discoveryUrl and then " +
		"call the backchannel endpoint. The client must detect that the response is missing the required property auth_req_id.",
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
public class FAPICIBAClientBackchannelResponseWithoutAuthReqIdTest extends AbstractFAPICIBAClientTest {

	@Override
	protected void addCustomValuesToIdToken() {	}

	@Override
	protected void createBackchannelResponse() {
		callAndStopOnFailure(CreateBackchannelEndpointResponseWithoutAuthReqId.class);
	}

	@Override
	protected void backchannelEndpointCallComplete() {
		fireTestFinished();
	}

	@Override
	protected void createIntermediateTokenResponse() {	}

	@Override
	protected void createFinalTokenResponse() {	}

	@Override
	protected void sendPingRequestAndVerifyResponse() {	}

}
