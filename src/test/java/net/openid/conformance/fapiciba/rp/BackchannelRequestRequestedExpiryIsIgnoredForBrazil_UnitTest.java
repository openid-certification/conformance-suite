package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BackchannelRequestRequestedExpiryIsIgnoredForBrazil_UnitTest {

	Environment env;
	BackchannelRequestRequestedExpiryIsIgnoredForBrazil condition;

	@BeforeEach
	public void setup() {
		env = mock(Environment.class);

		condition = new BackchannelRequestRequestedExpiryIsIgnoredForBrazil();
		condition.setProperties("testId", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);
	}

	@Test
	public void the_condition_succeeds_and_clears_requested_expiry_when_missing() {
		condition.evaluate(env);
		verify(env).removeObject("requested_expiry");
	}

	@Test
	public void the_condition_succeeds_and_clears_requested_expiry_when_integer() {
		JsonElement claim = new JsonPrimitive(30);
		when(env.getElementFromObject("backchannel_request_object", "claims.requested_expiry")).thenReturn(claim);

		condition.evaluate(env);
		verify(env).removeObject("requested_expiry");
	}

	@Test
	public void the_condition_succeeds_and_clears_requested_expiry_when_string() {
		JsonElement claim = new JsonPrimitive("30");
		when(env.getElementFromObject("backchannel_request_object", "claims.requested_expiry")).thenReturn(claim);

		condition.evaluate(env);
		verify(env).removeObject("requested_expiry");
	}

	@Test
	public void the_condition_succeeds_and_clears_requested_expiry_when_null() {
		JsonElement claim = JsonNull.INSTANCE;
		when(env.getElementFromObject("backchannel_request_object", "claims.requested_expiry")).thenReturn(claim);

		condition.evaluate(env);
		verify(env).removeObject("requested_expiry");
	}
}
