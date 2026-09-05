package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class WarnIfNotificationRejectionWithoutMTLSIsUncertain_UnitTest {

	@ParameterizedTest
	@ValueSource(strings = { "http_error", "tls_error" })
	public void acceptsExplicitRejection(String rejection) {
		Environment env = new Environment();
		env.putString("notification_without_mtls_rejection", rejection);
		assertThatCode(() -> condition().execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void reportsAmbiguousConnectionClosure() {
		Environment env = new Environment();
		env.putString("notification_without_mtls_rejection", "connection_closed");
		assertThatThrownBy(() -> condition().execute(env)).isInstanceOf(ConditionError.class)
			.hasMessageContaining("can also be a transport failure");
	}

	@Test
	public void missingOutcomeDoesNotProduceSuccess() {
		assertThatThrownBy(() -> condition().execute(new Environment())).isInstanceOf(ConditionError.class);
	}

	@Test
	public void moduleReportsUncertaintyAsWarning() {
		TestableModule module = new TestableModule();
		module.verifyPingResponse();
		assertThat(module.calledCondition).isEqualTo(WarnIfNotificationRejectionWithoutMTLSIsUncertain.class);
		assertThat(module.severity).isEqualTo(Condition.ConditionResult.WARNING);
	}

	private static WarnIfNotificationRejectionWithoutMTLSIsUncertain condition() {
		var condition = new WarnIfNotificationRejectionWithoutMTLSIsUncertain();
		condition.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), Condition.ConditionResult.WARNING);
		return condition;
	}

	private static class TestableModule extends FAPICIBAClientPingWithoutMTLSCertificateTest {
		private Class<? extends Condition> calledCondition;
		private Condition.ConditionResult severity;

		@Override
		protected void callAndContinueOnFailure(Class<? extends Condition> condition,
			Condition.ConditionResult onFail, String... requirements) {
			calledCondition = condition;
			severity = onFail;
		}
	}
}
