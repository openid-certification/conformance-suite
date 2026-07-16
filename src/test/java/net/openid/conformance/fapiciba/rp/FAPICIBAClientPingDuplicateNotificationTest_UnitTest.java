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
	public void rejectsFollowUpBeforeDuplicateAndStartsTimeoutAfterResponse() {
		TestableFAPICIBAClientPingDuplicateNotificationTest test =
			new TestableFAPICIBAClientPingDuplicateNotificationTest();

		test.sendDuplicatePingRequest();

		assertThat(test.startedRejectingFurtherClientInteractions).isTrue();
		assertThat(test.sentDuplicateAfterStartingRejection).isTrue();
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
			if (PingClientNotificationEndpoint.class.equals(conditionClass)) {
				sentDuplicateAfterStartingRejection = startedRejectingFurtherClientInteractions;
				duplicateResponseReceived = true;
			}
		}

		@Override
		protected void callAndContinueOnFailure(Class<? extends Condition> conditionClass,
			Condition.ConditionResult onFail, String... requirements) {
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
