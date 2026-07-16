package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.TestModule.Status;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FAPICIBAClientPingModePollFallbackTest_UnitTest {

	@Test
	public void withholdsPingAndSetsFirstAllowedFallbackPollFromReturnedInterval() {
		TestableFallbackTest test = new TestableFallbackTest();

		assertThat(test.pingNotificationShouldBeSent()).isFalse();
		assertThat(test.createFallbackBackchannelResponse()).isEqualTo(HttpStatus.OK);
		assertThat(test.conditionCalls).containsExactly(
			SetIntervalTo5Seconds.class,
			CreateBackchannelEndpointResponse.class,
			SetNextAllowedTokenRequest.class
		);
	}

	@Test
	public void returnsPendingThenSlowDownThenExpiredToken() {
		TestableFallbackTest test = new TestableFallbackTest();

		test.createFallbackTokenResponse(1);
		assertThat(test.conditionCalls).containsExactly(CreateAuthorizationPendingResponse.class);

		test.clearConditionCalls();
		test.createFallbackTokenResponse(2);
		assertThat(test.conditionCalls).containsExactly(
			CreateSlowDownResponse.class,
			SetIntervalToPlus5Seconds.class
		);

		test.clearConditionCalls();
		test.createFallbackTokenResponse(3);
		assertThat(test.conditionCalls).containsExactly(CreateExpiredTokenResponse.class);
	}

	@Test
	public void updatesIntervalBeforeTerminalResponseAndThenRejectsFurtherPolling() {
		TestableFallbackTest test = new TestableFallbackTest();

		test.completeTokenEndpointCall(2);
		assertThat(test.conditionCalls).containsExactly(SetNextAllowedTokenRequest.class);
		assertThat(test.startedWaitingForTimeout).isFalse();
		assertThat(test.lastStatus).isEqualTo(Status.WAITING);

		test.clearConditionCalls();
		test.completeTokenEndpointCall(3);
		assertThat(test.conditionCalls).isEmpty();
		assertThat(test.startedWaitingForTimeout).isTrue();
		assertThat(test.lastStatus).isEqualTo(Status.WAITING);
	}

	private static class TestableFallbackTest extends FAPICIBAClientPingModePollFallbackTest {

		private final List<Class<? extends Condition>> conditionCalls = new ArrayList<>();
		private boolean startedWaitingForTimeout;
		private Status lastStatus;

		private boolean pingNotificationShouldBeSent() {
			return shouldSendPingNotification();
		}

		private HttpStatus createFallbackBackchannelResponse() {
			return createBackchannelResponse();
		}

		private void createFallbackTokenResponse(int tokenPollCount) {
			env.putInteger("token_poll_count", tokenPollCount);
			createIntermediateTokenResponse();
		}

		private void completeTokenEndpointCall(int tokenPollCount) {
			env.putInteger("token_poll_count", tokenPollCount);
			tokenEndpointCallComplete();
		}

		private void clearConditionCalls() {
			conditionCalls.clear();
		}

		@Override
		protected void callAndStopOnFailure(Class<? extends Condition> conditionClass, String... requirements) {
			conditionCalls.add(conditionClass);
		}

		@Override
		protected void startWaitingForTimeout() {
			startedWaitingForTimeout = true;
		}

		@Override
		protected void setStatus(Status newStatus) {
			lastStatus = newStatus;
		}
	}
}
