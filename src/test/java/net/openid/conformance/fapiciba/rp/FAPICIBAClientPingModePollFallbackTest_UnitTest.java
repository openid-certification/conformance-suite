package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.ConditionCallBuilder;
import net.openid.conformance.testmodule.TestModule.Status;
import net.openid.conformance.variant.CIBAMode;
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
		assertThat(test.noFallbackPollCompletionScheduled).isTrue();
	}

	@Test
	public void capsRequestedExpiryWhenFallbackModuleOverridesResponseCreation() {
		TestableFallbackTest test = new TestableFallbackTest();

		test.createBrazilFallbackBackchannelResponse(100_000);

		assertThat(test.getEnv().getInteger("backchannel_endpoint_response", "expires_in"))
			.isEqualTo(86_400);
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
	public void controlledFallbackNeverIssuesTheFinalTokenResponse() {
		TestableFallbackTest test = new TestableFallbackTest();

		assertThat(test.shouldIssueFinalTokenResponse(1)).isFalse();
		assertThat(test.shouldIssueFinalTokenResponse(3)).isFalse();
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

	@Test
	public void completesSuccessfullyWhenClientDoesNotUseOptionalPollFallback() {
		TestableFallbackTest test = new TestableFallbackTest();

		test.completeIfFallbackPollingWasNotUsed();

		assertThat(test.fireTestFinishedCalled).isTrue();
		assertThat(test.lastStatus).isEqualTo(Status.RUNNING);
	}

	@Test
	public void keepsTestingFallbackBehaviorAfterClientStartsPolling() {
		TestableFallbackTest test = new TestableFallbackTest();
		test.getEnv().putInteger("token_poll_count", 1);

		test.completeIfFallbackPollingWasNotUsed();

		assertThat(test.fireTestFinishedCalled).isFalse();
		assertThat(test.lastStatus).isNull();
	}

	private static class TestableFallbackTest extends FAPICIBAClientPingModePollFallbackTest {

		private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
		private final List<Class<? extends Condition>> conditionCalls = new ArrayList<>();
		private boolean startedWaitingForTimeout;
		private boolean noFallbackPollCompletionScheduled;
		private boolean fireTestFinishedCalled;
		private boolean executeBackchannelResponseConditions;
		private Status lastStatus;

		private TestableFallbackTest() {
			cibaMode = CIBAMode.PING;
			setupOpenBankingBrazil();
		}

		private boolean pingNotificationShouldBeSent() {
			return shouldSendPingNotification();
		}

		private HttpStatus createFallbackBackchannelResponse() {
			return createBackchannelResponse();
		}

		private void createBrazilFallbackBackchannelResponse(int requestedExpiry) {
			executeBackchannelResponseConditions = true;
			env.putObject("backchannel_endpoint_http_request", new JsonObject());
			env.putObject("backchannel_request_object", new JsonObject());
			env.putInteger("requested_expiry", requestedExpiry);
			createProfileSpecificBackchannelResponse();
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

		private boolean shouldIssueFinalTokenResponse(int tokenPollCount) {
			return shouldIssueFinalCibaTokenResponse(tokenPollCount);
		}

		private void completeIfFallbackPollingWasNotUsed() {
			completeTestIfFallbackPollingWasNotUsed();
		}

		@Override
		protected void callAndStopOnFailure(Class<? extends Condition> conditionClass, String... requirements) {
			conditionCalls.add(conditionClass);
			if (executeBackchannelResponseConditions
				&& CreateBackchannelEndpointResponse.class.equals(conditionClass)) {
				executeCondition(new CreateBackchannelEndpointResponse());
			}
		}

		@Override
		protected void call(ConditionCallBuilder builder) {
			if (executeBackchannelResponseConditions
				&& SetOpenBankingBrazilCibaAuthenticationRequestMaximumExpiry.class.equals(
					builder.getConditionClass())) {
				executeCondition(new SetOpenBankingBrazilCibaAuthenticationRequestMaximumExpiry());
			}
		}

		@Override
		protected void call(ConditionSequence sequence) {
			if (sequence == null) {
				return;
			}
			sequence.evaluate();
			sequence.getTestExecutionUnits().stream()
				.filter(ConditionCallBuilder.class::isInstance)
				.map(ConditionCallBuilder.class::cast)
				.forEach(this::call);
		}

		private void executeCondition(Condition condition) {
			condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
			condition.execute(env);
		}

		@Override
		protected void startWaitingForTimeout() {
			startedWaitingForTimeout = true;
		}

		@Override
		protected void scheduleNoFallbackPollCompletion() {
			noFallbackPollCompletionScheduled = true;
		}

		@Override
		protected void setStatus(Status newStatus) {
			lastStatus = newStatus;
		}

		@Override
		public void fireTestFinished() {
			fireTestFinishedCalled = true;
		}
	}
}
