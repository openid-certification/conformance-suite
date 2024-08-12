package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BackchannelRequestRequestedExpiryIsAnInteger_UnitTest {

	Environment env;
	BackchannelRequestRequestedExpiryIsAnInteger condition;

	@BeforeEach
	public void setup(){
		env = mock(Environment.class);

		condition = new BackchannelRequestRequestedExpiryIsAnInteger();
		condition.setProperties("testId", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);
	}

	@Test
	public void the_condition_succeeds_when_there_is_no_requested_expiry_claim_in_environment() {
		condition.evaluate(env);
	}

	@Test
	public void the_condition_succeeds_when_there_is_an_integer_requested_expiry_claim_in_environment() {
		JsonElement claim = new JsonPrimitive(1);
		when(env.getElementFromObject("backchannel_request_object", "claims.requested_expiry")).thenReturn(claim);

		condition.evaluate(env);
	}

	@Test
	public void the_condition_succeeds_when_there_is_a_string_representation_of_an_integer_requested_expiry_claim_in_environment() {
		JsonElement claim = new JsonPrimitive("1");
		when(env.getElementFromObject("backchannel_request_object", "claims.requested_expiry")).thenReturn(claim);

		condition.evaluate(env);
	}

	@Test
	public void the_condition_fails_when_requested_expiry_is_a_decimal() {
		JsonElement claim = new JsonPrimitive(1.5);
		when(env.getElementFromObject("backchannel_request_object", "claims.requested_expiry")).thenReturn(claim);

		Exception expectedException = assertThrows(RuntimeException.class, () -> condition.evaluate(env));
		assertEquals("BackchannelRequestRequestedExpiryIsAnInteger: requested_expiry must be an integer or a string representing an integer", expectedException.getMessage());
	}

	@Test
	public void the_condition_fails_when_requested_expiry_is_an_invalid_string_representation_of_an_integer() {
		JsonElement claim = new JsonPrimitive("1,5");
		when(env.getElementFromObject("backchannel_request_object", "claims.requested_expiry")).thenReturn(claim);

		Exception expectedException = assertThrows(RuntimeException.class, () -> condition.evaluate(env));
		assertEquals("BackchannelRequestRequestedExpiryIsAnInteger: requested_expiry must be an integer or a string representing an integer", expectedException.getMessage());
	}

	@Test
	public void the_condition_fails_when_requested_expiry_is_explicitly_null() {
		JsonElement claim = JsonNull.INSTANCE;
		when(env.getElementFromObject("backchannel_request_object", "claims.requested_expiry")).thenReturn(claim);

		Exception expectedException = assertThrows(RuntimeException.class, () -> condition.evaluate(env));
		assertEquals("BackchannelRequestRequestedExpiryIsAnInteger: requested_expiry must not be JSON null", expectedException.getMessage());
	}

	@Test
	public void the_condition_fails_when_requested_expiry_is_a_non_positive_integer() {
		JsonElement claim = new JsonPrimitive(0);
		when(env.getElementFromObject("backchannel_request_object", "claims.requested_expiry")).thenReturn(claim);

		Exception expectedException = assertThrows(RuntimeException.class, () -> condition.evaluate(env));
		assertEquals("BackchannelRequestRequestedExpiryIsAnInteger: The 'requested_expiry' parameter must be a positive integer when present", expectedException.getMessage());
	}
}
