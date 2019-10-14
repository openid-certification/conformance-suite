package net.openid.conformance.condition.client;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.gson.JsonObject;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class EnsureMatchingFAPIInteractionId_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureMatchingFAPIInteractionId cond;

	private final String interactionId = "93bac548-d2de-4546-b106-880a5018460d"; // Example from Open Banking spec

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new EnsureMatchingFAPIInteractionId();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		env.putString("fapi_interaction_id", interactionId);

	}

	/**
	 * Test method for {@link EnsureMatchingFAPIInteractionId#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		JsonObject headers = new JsonObject();
		headers.addProperty("x-fapi-interaction-id", interactionId);
		env.putObject("resource_endpoint_response_headers", headers);

		cond.execute(env);

	}

	/**
	 * Test method for {@link EnsureMatchingFAPIInteractionId#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_wrongId() {

		JsonObject headers = new JsonObject();
		headers.addProperty("x-fapi-interaction-id", "incorrect");
		env.putObject("resource_endpoint_response_headers", headers);

		cond.execute(env);

	}

	/**
	 * Test method for {@link EnsureMatchingFAPIInteractionId#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingId() {

		JsonObject headers = new JsonObject();
		env.putObject("resource_endpoint_response_headers", headers);

		cond.execute(env);

	}

}
