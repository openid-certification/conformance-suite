package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class WarnIfPingNotificationWithWrongAuthReqIdWasAccepted_UnitTest {

	@Test
	public void reportsTwoXxAcceptance() {
		WarnIfPingNotificationWithWrongAuthReqIdWasAccepted condition = condition();
		Environment env = responseEnvironment(200);

		assertThatThrownBy(() -> condition.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("accepted a ping containing an unknown auth_req_id");
	}

	@Test
	public void acceptsClientErrorResponse() {
		WarnIfPingNotificationWithWrongAuthReqIdWasAccepted condition = condition();
		Environment env = responseEnvironment(409);

		assertThatCode(() -> condition.execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void wrongAuthReqIdModuleUsesWarningSeverity() {
		TestableWrongAuthReqIdTest test = new TestableWrongAuthReqIdTest();

		test.verifyPingResponse();

		assertThat(test.conditionClass).isEqualTo(WarnIfPingNotificationWithWrongAuthReqIdWasAccepted.class);
		assertThat(test.severity).isEqualTo(Condition.ConditionResult.WARNING);
		assertThat(test.requirements).containsExactly("CIBA-10.2");
	}

	private static WarnIfPingNotificationWithWrongAuthReqIdWasAccepted condition() {
		WarnIfPingNotificationWithWrongAuthReqIdWasAccepted condition =
			new WarnIfPingNotificationWithWrongAuthReqIdWasAccepted();
		condition.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), Condition.ConditionResult.WARNING);
		return condition;
	}

	private static Environment responseEnvironment(int statusCode) {
		Environment env = new Environment();
		env.putInteger("client_notification_endpoint_response_http_status", statusCode);
		return env;
	}

	private static class TestableWrongAuthReqIdTest extends FAPICIBAClientPingWithWrongAuthReqIdTest {
		private Class<? extends Condition> conditionClass;
		private Condition.ConditionResult severity;
		private List<String> requirements;

		@Override
		protected void callAndContinueOnFailure(Class<? extends Condition> calledCondition,
			Condition.ConditionResult onFail, String... calledRequirements) {
			conditionClass = calledCondition;
			severity = onFail;
			requirements = List.of(calledRequirements);
		}
	}
}
