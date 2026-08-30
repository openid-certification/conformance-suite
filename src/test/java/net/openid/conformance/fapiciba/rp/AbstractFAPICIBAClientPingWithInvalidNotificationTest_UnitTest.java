package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.TestModule.Status;
import net.openid.conformance.variant.FAPICIBAProfile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractFAPICIBAClientPingWithInvalidNotificationTest_UnitTest {

	@Test
	public void allowsGenericClientToPollAfterInvalidNotification() {
		TestableInvalidNotificationTest test = new TestableInvalidNotificationTest();
		test.setProfile(FAPICIBAProfile.PLAIN_FAPI);

		test.sendInvalidPing();

		assertThat(test.startedRejectingFurtherClientInteractions).isFalse();
		assertThat(test.sentInvalidPingAfterStartingRejection).isFalse();
		assertThat(test.rejectsFurtherInteractionsWhileWaiting()).isFalse();
		assertThat(test.startedWaitingForTimeout).isFalse();
	}

	@Test
	public void rejectsClientFollowUpForBrazilInvalidNotification() {
		TestableInvalidNotificationTest test = new TestableInvalidNotificationTest();
		test.setProfile(FAPICIBAProfile.OPENBANKING_BRAZIL);

		test.sendInvalidPing();

		assertThat(test.startedRejectingFurtherClientInteractions).isTrue();
		assertThat(test.sentInvalidPingAfterStartingRejection).isTrue();
		assertThat(test.rejectsFurtherInteractionsWhileWaiting()).isTrue();
	}

	@Test
	public void waitsForGenericClientAfterInvalidNotificationResponse() {
		TestableInvalidNotificationTest test = new TestableInvalidNotificationTest();
		test.setProfile(FAPICIBAProfile.PLAIN_FAPI);

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

		private void setProfile(FAPICIBAProfile profile) {
			this.profile = profile;
		}

		private boolean rejectsFurtherInteractionsWhileWaiting() {
			return shouldRejectFurtherClientInteractionsWhileWaitingForTimeout();
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
