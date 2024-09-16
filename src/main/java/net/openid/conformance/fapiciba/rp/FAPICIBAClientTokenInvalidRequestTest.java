package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.CIBAMode;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-invalid-request-test",
	displayName = "FAPI-CIBA-ID1: Client test - invalid_request returned in token response",
	summary = """
		As per the CIBA specification, section 11: \
		“If a Client continually polls quicker than the interval, the OP may return an invalid_request error. \
		If a Client receives an invalid_request error it must not make any further requests for the same auth_req_id.”

		In this test, the client should perform OpenID discovery from the displayed discoveryUrl and then call the backchannel endpoint. \
		The client will start polling the token endpoint and receive an invalid_request response on the \
		third request. The client must detect this, and not make further requests to the token endpoint with that auth_req_id\
		""",
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
public class FAPICIBAClientTokenInvalidRequestTest extends AbstractFAPICIBAClientTest {

	@Override
	protected void createIntermediateTokenResponse() {
		int tokenPollCount = env.getInteger("token_poll_count");
		if (tokenPollCount == 3) {
			callAndStopOnFailure(CreateInvalidRequestResponse.class, "CIBA-11");
		} else {
			callAndStopOnFailure(CreateAuthorizationPendingResponse.class);
		}
	}

	@Override
	protected void tokenEndpointCallComplete() {
		int tokenPollCount = env.getInteger("token_poll_count");
		if (tokenPollCount == 3) {
			fireTestFinished();
		} else {
			setStatus(Status.WAITING);
		}
	}

	@Override
	protected boolean clientHasPolledEnough(int tokenPollCount) {
		return tokenPollCount > 3;
	}
}
