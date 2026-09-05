package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.variant.FAPICIBAProfile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FAPICIBAClientPingWithoutMTLSCertificateTest_UnitTest {

	@Test
	public void missingCertificateDoesNotProhibitControlledPollFallback() {
		TestableModule module = new TestableModule();
		module.profile = FAPICIBAProfile.OPENBANKING_BRAZIL;

		module.sendPingRequestAndVerifyResponse();

		assertThat(module.startingShutdown).isFalse();
		assertThat(module.sentCondition).isEqualTo(PingClientNotificationEndpointWithoutMTLS.class);
		assertThat(module.warningCondition).isEqualTo(WarnIfNotificationRejectionWithoutMTLSIsUncertain.class);
	}

	@Test
	public void finishesAfterCertificateRejectionWithoutWaitingForNoClientActivity() {
		TestableModule module = new TestableModule();

		module.pingRequestComplete();

		assertThat(module.getEnv().getBoolean("client_ping_response_validated")).isTrue();
		assertThat(module.finished).isTrue();
		assertThat(module.waitingForTimeout).isFalse();
	}

	private static class TestableModule extends FAPICIBAClientPingWithoutMTLSCertificateTest {
		private Class<? extends Condition> sentCondition;
		private Class<? extends Condition> warningCondition;
		private boolean finished;
		private boolean waitingForTimeout;

		@Override
		protected void callAndStopOnFailure(Class<? extends Condition> conditionClass,
			Condition.ConditionResult onFail, String... requirements) {
			assertThat(onFail).isEqualTo(Condition.ConditionResult.FAILURE);
			assertThat(requirements).contains("BrazilCIBA-6.3.4");
			sentCondition = conditionClass;
		}

		@Override
		protected void callAndContinueOnFailure(Class<? extends Condition> conditionClass,
			Condition.ConditionResult onFail, String... requirements) {
			assertThat(onFail).isEqualTo(Condition.ConditionResult.WARNING);
			assertThat(requirements).contains("BrazilCIBA-6.3.4");
			warningCondition = conditionClass;
		}

		@Override
		protected void startWaitingForTimeout() {
			waitingForTimeout = true;
		}

		@Override
		protected void setStatus(Status newStatus) {
			// Lifecycle notifications are outside the scope of this module-control-flow test.
		}

		@Override
		public void fireTestFinished() {
			finished = true;
		}
	}
}
