package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.testmodule.PublishTestModule;
import org.springframework.http.HttpStatus;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-respects-interval-test",
	displayName = "FAPI-CIBA-ID1: Client test - the backchannel response sets the polling interval to 31 seconds",
	summary = "The client should perform OpenID discovery from the displayed discoveryUrl and then " +
		"call the backchannel endpoint. The response will set the interval property to 31 seconds and fail " +
		"if it receives a token request before that.",
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
public class FAPICIBAClientRespectsPollingIntervalTest extends AbstractFAPICIBAClientTest {

	@Override
	protected HttpStatus createBackchannelResponse() {
		callAndStopOnFailure(SetIntervalTo31Seconds.class);
		callAndStopOnFailure(CreateBackchannelEndpointResponse.class);
		return HttpStatus.OK;
	}

}
