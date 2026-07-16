package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.TestModule.Status;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractFAPICIBAClientPingWithInvalidNotificationTest_UnitTest {

	@Test
	public void rejectsClientFollowUpBeforeSendingInvalidNotification() {
		TestableInvalidNotificationTest test = new TestableInvalidNotificationTest();

		test.sendInvalidPing();

		assertThat(test.startedRejectingFurtherClientInteractions).isTrue();
		assertThat(test.sentInvalidPingAfterStartingRejection).isTrue();
		assertThat(test.startedWaitingForTimeout).isFalse();
	}

	@Test
	public void waitsForNoClientFollowUpAfterInvalidNotificationResponse() {
		TestableInvalidNotificationTest test = new TestableInvalidNotificationTest();

		test.completePingRequest();

		assertThat(test.getEnv().getBoolean("client_ping_response_validated")).isTrue();
		assertThat(test.startedWaitingForTimeout).isTrue();
		assertThat(test.lastStatus).isEqualTo(Status.WAITING);
		assertThat(test.fireTestFinishedCalled).isFalse();
	}

	private static class TestableInvalidNotificationTest
		extends AbstractFAPICIBAClientPingWithInvalidNotificationTest {

		private boolean startedRejectingFurtherClientInteractions;
		private boolean sentInvalidPingAfterStartingRejection;
		private boolean startedWaitingForTimeout;
		private boolean fireTestFinishedCalled;
		private Status lastStatus;

		private void sendInvalidPing() {
			sendPingRequestAndVerifyResponse();
		}

		private void completePingRequest() {
			pingRequestComplete();
		}

		@Override
		protected void rejectFurtherClientInteractions() {
			startedRejectingFurtherClientInteractions = true;
		}

		@Override
		protected Class<? extends Condition> getPingNotificationCondition() {
			return PingClientNotificationEndpointWithBadBearerToken.class;
		}

		@Override
		protected void callAndStopOnFailure(Class<? extends Condition> conditionClass,
			Condition.ConditionResult onFail, String... requirements) {
			sentInvalidPingAfterStartingRejection = startedRejectingFurtherClientInteractions;
		}

		@Override
		protected void startWaitingForTimeout() {
			startedWaitingForTimeout = true;
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
