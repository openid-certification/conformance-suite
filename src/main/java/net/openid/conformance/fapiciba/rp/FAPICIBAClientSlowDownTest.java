package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.CIBAMode;
import net.openid.conformance.variant.VariantNotApplicable;
import org.springframework.http.HttpStatus;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-slow-down-test",
	displayName = "FAPI-CIBA-ID1: Client test - the token endpoint responds with slow_down",
	summary = "The client should perform OpenID discovery from the displayed discoveryUrl, " +
		"and then call the backchannel endpoint, which responds with an interval of 5 seconds. " +
		"The client will start polling the token endpoint and receive a slow_down response on the " +
		"second request. The client must then wait at least 10 seconds before polling again. " +
		"Finally, the client uses the access token from the token endpoint response in a resource endpoint request.",
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
@VariantNotApplicable(parameter = CIBAMode.class, values = { "ping" })
public class FAPICIBAClientSlowDownTest extends AbstractFAPICIBAClientTest {

	@Override
	protected HttpStatus createBackchannelResponse() {
		callAndStopOnFailure(SetIntervalTo5Seconds.class);
		callAndStopOnFailure(CreateBackchannelEndpointResponse.class);
		return HttpStatus.OK;
	}

	@Override
	protected void createIntermediateTokenResponse() {
		int tokenPollCount = env.getInteger("token_poll_count");
		if (tokenPollCount == 2) {
			callAndStopOnFailure(CreateSlowDownResponse.class);
			callAndStopOnFailure(SetIntervalToPlus5Seconds.class);
		} else {
			callAndStopOnFailure(CreateAuthorizationPendingResponse.class);
		}
	}

}
