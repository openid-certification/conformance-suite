package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.testmodule.PublishTestModule;
import org.springframework.http.HttpStatus;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-invalid-missing-expiresin-test",
	displayName = "FAPI-CIBA-ID1: Client test - expires_in not present in backchannel response",
	summary = "The client should perform OpenID discovery from the displayed discoveryUrl and then " +
		"call the backchannel endpoint. The client must detect that the response is missing the required property expires_in.",
	profile = "FAPI-CIBA-ID1"
)
public class FAPICIBAClientBackchannelResponseInvalidMissingExpiresInTest extends AbstractFAPICIBAClientTest {

	@Override
	protected HttpStatus createBackchannelResponse() {
		callAndStopOnFailure(CreateBackchannelEndpointResponseWithoutExpiresIn.class);
		return HttpStatus.OK;
	}

	@Override
	protected void backchannelEndpointCallComplete() {
		fireTestFinished();
	}
}
