package io.fintechlabs.testframework.condition.client;

import static org.assertj.core.api.Assertions.assertThat;

import io.fintechlabs.testframework.testmodule.OIDFJSON;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class AddFAPIInteractionIdToResourceEndpointRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddFAPIInteractionIdToResourceEndpointRequest cond;

	private String interactionId = "c770aef3-6784-41f7-8e0e-ff5f97bddb3a"; // Example from FAPI 1

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		cond = new AddFAPIInteractionIdToResourceEndpointRequest();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate() {

		env.putString("fapi_interaction_id", interactionId);

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("fapi_interaction_id");

		assertThat(env.getString("resource_endpoint_request_headers", "x-fapi-interaction-id")).isEqualTo(interactionId);
	}

	@Test
	public void testEvaluate_existingHeaders() {

		env.putString("fapi_interaction_id", interactionId);
		env.putObject("resource_endpoint_request_headers",	new JsonObject());

		cond.evaluate(env);

		JsonObject req = env.getObject("resource_endpoint_request_headers");

		assertNotNull(req);
		assertTrue(req.has("x-fapi-interaction-id"));
		assertEquals(interactionId, OIDFJSON.getString(req.get("x-fapi-interaction-id")));

	}

	@Test
	public void testEvaluate_existingHeadersOverwrite() {

		env.putString("fapi_interaction_id", interactionId);
		env.putObject("resource_endpoint_request_headers",	new JsonParser().parse("{\"x-fapi-interaction-id\":\"foo-bar\"}").getAsJsonObject());

		cond.evaluate(env);

		JsonObject req = env.getObject("resource_endpoint_request_headers");

		assertNotNull(req);
		assertTrue(req.has("x-fapi-interaction-id"));
		assertEquals(interactionId, OIDFJSON.getString(req.get("x-fapi-interaction-id")));

	}

}
