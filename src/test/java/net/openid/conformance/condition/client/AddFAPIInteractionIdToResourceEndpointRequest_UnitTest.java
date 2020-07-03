package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;

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
		env.putObject("resource_endpoint_request_headers",	new JsonObject());

		cond.execute(env);

		JsonObject req = env.getObject("resource_endpoint_request_headers");

		assertNotNull(req);
		assertTrue(req.has("x-fapi-interaction-id"));
		assertEquals(interactionId, OIDFJSON.getString(req.get("x-fapi-interaction-id")));

	}

	@Test
	public void testEvaluate_existingHeadersOverwrite() {

		env.putString("fapi_interaction_id", interactionId);
		env.putObject("resource_endpoint_request_headers",	new JsonParser().parse("{\"x-fapi-interaction-id\":\"foo-bar\"}").getAsJsonObject());

		cond.execute(env);

		JsonObject req = env.getObject("resource_endpoint_request_headers");

		assertNotNull(req);
		assertTrue(req.has("x-fapi-interaction-id"));
		assertEquals(interactionId, OIDFJSON.getString(req.get("x-fapi-interaction-id")));

	}

}
