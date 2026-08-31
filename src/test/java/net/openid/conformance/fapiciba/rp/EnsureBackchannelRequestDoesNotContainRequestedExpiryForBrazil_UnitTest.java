package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
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

public class EnsureBackchannelRequestDoesNotContainRequestedExpiryForBrazil_UnitTest {

	private Environment env;
	private EnsureBackchannelRequestDoesNotContainRequestedExpiryForBrazil condition;

	@BeforeEach
	public void setup() {
		env = mock(Environment.class);

		condition = new EnsureBackchannelRequestDoesNotContainRequestedExpiryForBrazil();
		condition.setProperties("testId", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);
	}

	@Test
	public void succeedsWhenRequestedExpiryIsMissing() {
		condition.evaluate(env);
	}

	@Test
	public void failsWhenRequestedExpiryIsAnInteger() {
		JsonElement claim = new JsonPrimitive(30);
		when(env.getElementFromObject("backchannel_request_object", "claims.requested_expiry")).thenReturn(claim);

		assertThatThrownBy(() -> condition.evaluate(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("must not contain requested_expiry");
	}

	@Test
	public void failsWhenRequestedExpiryIsAString() {
		JsonElement claim = new JsonPrimitive("30");
		when(env.getElementFromObject("backchannel_request_object", "claims.requested_expiry")).thenReturn(claim);

		assertThatThrownBy(() -> condition.evaluate(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("must not contain requested_expiry");
	}

	@Test
	public void failsWhenRequestedExpiryIsJsonNull() {
		JsonElement claim = JsonNull.INSTANCE;
		when(env.getElementFromObject("backchannel_request_object", "claims.requested_expiry")).thenReturn(claim);

		assertThatThrownBy(() -> condition.evaluate(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("must not contain requested_expiry");
	}

	@Test
	public void failsWhenRequestedExpiryIsAnObject() {
		when(env.getElementFromObject("backchannel_request_object", "claims.requested_expiry"))
			.thenReturn(new JsonObject());

		assertThatThrownBy(() -> condition.evaluate(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("must not contain requested_expiry");
	}
}
