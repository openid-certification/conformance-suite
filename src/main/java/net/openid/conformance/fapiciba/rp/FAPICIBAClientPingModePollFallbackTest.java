package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.CIBAMode;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;
import org.springframework.http.HttpStatus;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-ping-mode-poll-fallback-test",
	displayName = "FAPI-CIBA-ID1: Client test - controlled poll fallback after a missing ping notification",
	summary = "The authorization server deliberately withholds the ping notification while the client remains " +
		"registered for ping mode. If the client uses poll as a fallback, it must wait for the returned interval, " +
		"observe the increased interval after slow_down, and stop polling after expired_token.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = CIBAMode.class, values = {"poll"})
@VariantNotApplicable(parameter = FAPICIBAProfile.class, values = {"plain_fapi", "openbanking_uk", "connectid_au"})
public class FAPICIBAClientPingModePollFallbackTest extends AbstractFAPICIBAClientTest {

	private static final int TERMINAL_POLL_COUNT = 3;

	@Override
	protected boolean shouldSendPingNotification() {
		return false;
	}

	@Override
	protected HttpStatus createBackchannelResponse() {
		callAndStopOnFailure(SetIntervalTo5Seconds.class, "CIBA-7.3", "BrazilCIBA-6.3.4.1");
		callAndStopOnFailure(CreateBackchannelEndpointResponse.class);
		callAndStopOnFailure(SetNextAllowedTokenRequest.class, "CIBA-7.3", "BrazilCIBA-6.3.4.1");
		return HttpStatus.OK;
	}

	@Override
	protected void createIntermediateTokenResponse() {
		int tokenPollCount = env.getInteger("token_poll_count");
		if (tokenPollCount == 2) {
			callAndStopOnFailure(CreateSlowDownResponse.class, "CIBA-11", "BrazilCIBA-6.3.4.1");
			callAndStopOnFailure(SetIntervalToPlus5Seconds.class, "CIBA-7.3", "BrazilCIBA-6.3.4.1");
		} else if (tokenPollCount == TERMINAL_POLL_COUNT) {
			callAndStopOnFailure(CreateExpiredTokenResponse.class, "CIBA-11", "BrazilCIBA-6.3.4.1");
		} else {
			callAndStopOnFailure(CreateAuthorizationPendingResponse.class, "CIBA-11");
		}
	}

	@Override
	protected void tokenEndpointCallComplete() {
		int tokenPollCount = env.getInteger("token_poll_count");
		if (tokenPollCount == TERMINAL_POLL_COUNT) {
			startWaitingForTimeout();
			setStatus(Status.WAITING);
			return;
		}
		super.tokenEndpointCallComplete();
	}
}
