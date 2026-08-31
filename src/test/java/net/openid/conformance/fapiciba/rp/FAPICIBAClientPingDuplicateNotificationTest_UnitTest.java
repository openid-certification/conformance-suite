package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.TestModule.Status;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FAPICIBAClientPingDuplicateNotificationTest_UnitTest {

	@Test
	public void schedulesDuplicateOnlyAfterPingResponseAndResourceCallComplete() {
		TestableFAPICIBAClientPingDuplicateNotificationTest test =
			new TestableFAPICIBAClientPingDuplicateNotificationTest();

		test.completeResourceEndpointCall();

		assertThat(test.duplicatePingScheduleCount).isZero();
		assertThat(test.lastStatus).isEqualTo(Status.WAITING);

		test.completePingRequest();

		assertThat(test.duplicatePingScheduleCount).isOne();
		assertThat(test.getEnv().getBoolean("client_ping_response_validated")).isTrue();

		test.completePingRequest();
		test.completeResourceEndpointCall();

		assertThat(test.duplicatePingScheduleCount).isOne();
	}

	@Test
	public void schedulesDuplicateWhenResourceCallCompletesAfterPingResponse() {
		TestableFAPICIBAClientPingDuplicateNotificationTest test =
			new TestableFAPICIBAClientPingDuplicateNotificationTest();

		test.completePingRequest();

		assertThat(test.duplicatePingScheduleCount).isZero();

		test.completeResourceEndpointCall();

		assertThat(test.duplicatePingScheduleCount).isOne();
	}

	@Test
	public void treatsDuplicateResponseChecksAsWarningsAndStartsTimeoutAfterResponse() {
		TestableFAPICIBAClientPingDuplicateNotificationTest test =
			new TestableFAPICIBAClientPingDuplicateNotificationTest();

		test.sendDuplicatePingRequest();

		assertThat(test.startedRejectingFurtherClientInteractions).isTrue();
		assertThat(test.sentDuplicateAfterStartingRejection).isTrue();
		assertThat(test.duplicatePingResponseSeverity).isEqualTo(Condition.ConditionResult.WARNING);
		assertThat(test.not3xxSeverity).isEqualTo(Condition.ConditionResult.WARNING);
		assertThat(test.noContentSeverity).isEqualTo(Condition.ConditionResult.WARNING);
		assertThat(test.startedTimeoutAfterDuplicateResponse).isTrue();
	}

	private static class TestableFAPICIBAClientPingDuplicateNotificationTest
		extends FAPICIBAClientPingDuplicateNotificationTest {

		private int duplicatePingScheduleCount;
		private Status lastStatus;
		private boolean startedRejectingFurtherClientInteractions;
		private boolean sentDuplicateAfterStartingRejection;
		private boolean duplicateResponseReceived;
		private boolean startedTimeoutAfterDuplicateResponse;
		private Condition.ConditionResult duplicatePingResponseSeverity;
		private Condition.ConditionResult not3xxSeverity;
		private Condition.ConditionResult noContentSeverity;

		private void completePingRequest() {
			pingRequestComplete();
		}

		private void completeResourceEndpointCall() {
			resourceEndpointCallComplete();
		}

		private void sendDuplicatePingRequest() {
			sendDuplicatePingRequestAndVerifyResponse();
		}

		@Override
		protected void scheduleDuplicatePing() {
			duplicatePingScheduleCount++;
		}

		@Override
		protected void rejectFurtherClientInteractions() {
			startedRejectingFurtherClientInteractions = true;
		}

		@Override
		protected void callAndStopOnFailure(Class<? extends Condition> conditionClass,
			Condition.ConditionResult onFail, String... requirements) {
			if (PingClientNotificationEndpoint.class.isAssignableFrom(conditionClass)) {
				throw new AssertionError("Duplicate ping response handling must not stop the test");
			}
		}

		@Override
		protected void callAndContinueOnFailure(Class<? extends Condition> conditionClass,
			Condition.ConditionResult onFail, String... requirements) {
			if (PingClientNotificationEndpointAllowingHttpErrorResponse.class.equals(conditionClass)) {
				sentDuplicateAfterStartingRejection = startedRejectingFurtherClientInteractions;
				duplicateResponseReceived = true;
				duplicatePingResponseSeverity = onFail;
			} else if (VerifyPingHttpResponseStatusCodeIsNot3XX.class.equals(conditionClass)) {
				not3xxSeverity = onFail;
			} else if (VerifyPingHttpResponseStatusCodeIs204.class.equals(conditionClass)) {
				noContentSeverity = onFail;
			}
		}

		@Override
		protected void startWaitingForTimeout() {
			startedTimeoutAfterDuplicateResponse = duplicateResponseReceived;
		}

		@Override
		protected void setStatus(Status newStatus) {
			lastStatus = newStatus;
		}
	}
}
