package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.testmodule.PublishTestModule;
import org.springframework.http.HttpStatus;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-invalid-request-unknown-user-id-test",
	displayName = "FAPI-CIBA-ID1: Client test - expired_id_token_hint returned in backchannel response",
	summary = "The client should perform OpenID discovery from the displayed discoveryUrl and then " +
		"call the backchannel endpoint. The client must detect that the response is a HTTP 400 Bad Request " +
		"with error unknown_user_id and not make further requests after that.",
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
public class FAPICIBAClientBackchannelUnknownUserIdTest extends AbstractFAPICIBAClientTest {

	@Override
	protected HttpStatus createBackchannelResponse() {
		callAndStopOnFailure(CreateBackchannelEndpointResponseWithUnknownUserIdError.class);
		return HttpStatus.BAD_REQUEST;
	}

	@Override
	protected void backchannelEndpointCallComplete() {
		fireTestFinished();
	}

}
