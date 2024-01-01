package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.CIBAMode;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-expired-token-test",
	displayName = "FAPI-CIBA-ID1: Client test - expired_token returned in token response",
	summary = "The client should perform OpenID discovery from the displayed discoveryUrl and then call the backchannel endpoint. " +
		"The client will start polling the token endpoint and receive an expired_token response on the " +
		"third request, simulating an expired auth_req_id. " +
		"The client must detect this, and not make further requests to the token endpoint with that auth_req_id",
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
public class FAPICIBAClientTokenExpiredTokenTest extends AbstractFAPICIBAClientTest {

	@Override
	protected void createIntermediateTokenResponse() {
		int tokenPollCount = env.getInteger("token_poll_count");
		if (tokenPollCount == 3) {
			callAndStopOnFailure(CreateExpiredTokenResponse.class, "CIBA-11");
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
			super.tokenEndpointCallComplete();
		}
	}

	@Override
	protected boolean clientHasPolledEnough(int tokenPollCount) {
		return tokenPollCount > 3;
	}
}
