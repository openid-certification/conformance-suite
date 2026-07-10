package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EnsureLoginHintEqualsConsentId_UnitTest {

	private static final String CONSENT_ID = "urn:ofbr:consent:123e4567-e89b-12d3-a456-426614174000";

	private Environment env;
	private EnsureLoginHintEqualsConsentId condition;

	@BeforeEach
	public void setup() {
		env = mock(Environment.class);
		when(env.getString("consent_id")).thenReturn(CONSENT_ID);

		condition = new EnsureLoginHintEqualsConsentId();
		condition.setProperties("testId", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);
	}

	@Test
	public void succeedsWhenLoginHintMatchesConsentId() {
		when(env.getElementFromObject("backchannel_request_object", "claims.login_hint"))
			.thenReturn(new JsonPrimitive(CONSENT_ID));

		condition.evaluate(env);
	}

	@Test
	public void failsWhenLoginHintIsMissing() {
		assertThatThrownBy(() -> condition.evaluate(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("login_hint is missing or not a string");
	}

	@Test
	public void failsWhenLoginHintIsJsonNull() {
		when(env.getElementFromObject("backchannel_request_object", "claims.login_hint"))
			.thenReturn(JsonNull.INSTANCE);

		assertThatThrownBy(() -> condition.evaluate(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("login_hint is missing or not a string");
	}

	@Test
	public void failsWhenLoginHintIsNotAString() {
		when(env.getElementFromObject("backchannel_request_object", "claims.login_hint"))
			.thenReturn(new JsonPrimitive(42));

		assertThatThrownBy(() -> condition.evaluate(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("login_hint is missing or not a string");
	}

	@Test
	public void failsWhenLoginHintDoesNotMatchConsentId() {
		when(env.getElementFromObject("backchannel_request_object", "claims.login_hint"))
			.thenReturn(new JsonPrimitive("urn:ofbr:consent:different"));

		assertThatThrownBy(() -> condition.evaluate(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("login_hint does not match consent_id");
	}
}
