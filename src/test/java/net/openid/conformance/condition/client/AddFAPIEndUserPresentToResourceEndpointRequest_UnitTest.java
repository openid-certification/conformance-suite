package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class AddFAPIEndUserPresentToResourceEndpointRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private AbstractAddFAPIEndUserPresentToResourceEndpointRequest createCondition(boolean endUserPresent) {
		AbstractAddFAPIEndUserPresentToResourceEndpointRequest cond = endUserPresent
			? new AddFAPIEndUserPresentTrueToResourceEndpointRequest()
			: new AddFAPIEndUserPresentFalseToResourceEndpointRequest();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		return cond;
	}

	@Test
	public void testEvaluate_true() {

		env.putObject("resource_endpoint_request_headers", new JsonObject());

		createCondition(true).execute(env);

		JsonObject req = env.getObject("resource_endpoint_request_headers");

		assertNotNull(req);
		assertTrue(req.has("x-fapi-end-user-present"));
		assertEquals("true", OIDFJSON.getString(req.get("x-fapi-end-user-present")));
	}

	@Test
	public void testEvaluate_false() {

		env.putObject("resource_endpoint_request_headers", new JsonObject());

		createCondition(false).execute(env);

		JsonObject req = env.getObject("resource_endpoint_request_headers");

		assertNotNull(req);
		assertTrue(req.has("x-fapi-end-user-present"));
		assertEquals("false", OIDFJSON.getString(req.get("x-fapi-end-user-present")));
	}

	@Test
	public void testEvaluate_existingHeaderOverwritten() {

		env.putObject("resource_endpoint_request_headers",
			JsonParser.parseString("{\"x-fapi-end-user-present\":\"true\"}").getAsJsonObject());

		createCondition(false).execute(env);

		JsonObject req = env.getObject("resource_endpoint_request_headers");

		assertNotNull(req);
		assertEquals("false", OIDFJSON.getString(req.get("x-fapi-end-user-present")));
	}

}
