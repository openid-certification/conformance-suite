package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CheckForFAPIInteractionIdInResourceResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckForFAPIInteractionIdInResourceResponse cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckForFAPIInteractionIdInResourceResponse();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test method for {@link CheckForFAPIInteractionIdInResourceResponse#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		JsonObject headers = new JsonObject();
		headers.addProperty("x-fapi-interaction-id", "c770aef3-6784-41f7-8e0e-ff5f97bddb3a"); // Example from FAPI 1
		env.putObject("resource_endpoint_response_headers", headers);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("resource_endpoint_response_headers", "x-fapi-interaction-id");
	}

	@Test
	public void testEvaluate_uppercaseNoError() {
		// This test reflects the current behaviour of accepting both upper & lower case UUIDs.
		// https://tools.ietf.org/html/rfc4122 says:
		// > The hexadecimal values "a" through "f" are output as
		// > lower case characters and are case insensitive on input.
		//
		// So possibly we should raise a warning/error if a server is returning upper case UUIDs
		// also see https://gitlab.com/openid/conformance-suite/-/issues/757

		JsonObject headers = new JsonObject();
		headers.addProperty("x-fapi-interaction-id", "C770AEF3-6784-41F7-8E0E-FF5F97BDDB3A"); // Example from FAPI 1, uppercased
		env.putObject("resource_endpoint_response_headers", headers);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("resource_endpoint_response_headers", "x-fapi-interaction-id");
	}

	/**
	 * Test method for {@link CheckForFAPIInteractionIdInResourceResponse#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_invalidValue() {
		assertThrows(ConditionError.class, () -> {

			JsonObject headers = new JsonObject();
			headers.addProperty("x-fapi-interaction-id", "this is not a uuid");
			env.putObject("resource_endpoint_response_headers", headers);

			cond.execute(env);
		});
	}

	/**
	 * Test method for {@link CheckForFAPIInteractionIdInResourceResponse#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_missingValue() {
		assertThrows(ConditionError.class, () -> {

			JsonObject headers = new JsonObject();
			env.putObject("resource_endpoint_response_headers", headers);

			cond.execute(env);
		});
	}

}
