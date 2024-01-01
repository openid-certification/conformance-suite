package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.testmodule.PublishTestModule;
import org.springframework.http.HttpStatus;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-invalid-id-token-hint-test",
	displayName = "FAPI-CIBA-ID1: Client test - invalid_id_token_hint returned in backchannel response",
	summary = "The client should perform OpenID discovery from the displayed discoveryUrl and then " +
		"call the backchannel endpoint. The client must detect that the response is a HTTP 400 Bad Request " +
		"with error invalid_id_token_hint and not make further requests after that.",
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
public class FAPICIBAClientBackchannelInvalidIdTokenHintTest extends AbstractFAPICIBAClientTest {

	@Override
	protected HttpStatus createBackchannelResponse() {
		callAndStopOnFailure(CreateBackchannelEndpointResponseWithInvalidIdTokenHintError.class, "BrazilCIBA-5.2.2");
		return HttpStatus.BAD_REQUEST;
	}

	@Override
	protected void backchannelEndpointCallComplete() {
		fireTestFinished();
	}

}
