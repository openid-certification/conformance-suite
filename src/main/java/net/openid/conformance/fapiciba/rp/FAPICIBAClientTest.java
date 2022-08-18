package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-test",
	displayName = "FAPI-CIBA-ID1: Client test",
	summary = "Tests a 'happy path' flow; the client should perform OpenID discovery from the displayed discoveryUrl, " +
		"call the backchannel endpoint and either poll the token endpoint or wait to get pinged " +
		"and then use the access token from the token endpoint response in a resource endpoint request.",
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

public class FAPICIBAClientTest extends AbstractFAPICIBAClientTest {

	@Override
	protected void addCustomValuesToIdToken(){
		//Do nothing
	}

	@Override
	protected void createBackchannelResponse() {
		callAndStopOnFailure(CreateBackchannelEndpointResponse.class);
	}

	@Override
	protected void backchannelEndpointCallComplete() {
		setStatus(Status.WAITING);
	}

	@Override
	protected void createIntermediateTokenResponse() {
		callAndStopOnFailure(CreateAuthorizationPendingResponse.class);
	}

	@Override
	protected void createFinalTokenResponse() {
		callAndStopOnFailure(CreateTokenEndpointResponse.class);
	}

	@Override
	protected void sendPingRequestAndVerifyResponse() {
		callAndStopOnFailure(PingClientNotificationEndpoint.class, Condition.ConditionResult.FAILURE, "CIBA");
		callAndStopOnFailure(VerifyPingHttpResponseStatusCodeIsNot3XX.class, Condition.ConditionResult.FAILURE, "CIBA-10.2");
		callAndContinueOnFailure(VerifyPingHttpResponseStatusCodeIs204.class, Condition.ConditionResult.WARNING, "CIBA-10.2");
	}
}
