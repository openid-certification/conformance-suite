package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class AddFAPIFinancialIdToRequestHeaders_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddFAPIFinancialIdToResourceEndpointRequest cond;

	private JsonObject resourceConfig;

	private String financialId;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new AddFAPIFinancialIdToResourceEndpointRequest();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		// create a random financial ID so we know it's getting copied
		financialId = RandomStringUtils.randomAlphanumeric(30);

		resourceConfig = new JsonObject();
		resourceConfig.addProperty("institution_id", financialId);


	}

	/**
	 */
	@Test
	public void testEvaluate() {

		env.putObject("resource", resourceConfig);
		env.putObject("resource_endpoint_request_headers", new JsonObject());

		cond.execute(env);

		JsonObject req = env.getObject("resource_endpoint_request_headers");

		assertNotNull(req);
		assertTrue(req.has("x-fapi-financial-id"));
		assertEquals(financialId, OIDFJSON.getString(req.get("x-fapi-financial-id")));

	}

	@Test
	public void testEvaluate_existingHeaders() {

		env.putObject("resource", resourceConfig);
		env.putObject("resource_endpoint_request_headers",	new JsonObject());

		cond.execute(env);

		JsonObject req = env.getObject("resource_endpoint_request_headers");

		assertNotNull(req);
		assertTrue(req.has("x-fapi-financial-id"));
		assertEquals(financialId, OIDFJSON.getString(req.get("x-fapi-financial-id")));

	}

	@Test
	public void testEvaluate_existingHeadersOverwrite() {

		env.putObject("resource", resourceConfig);
		env.putObject("resource_endpoint_request_headers",	new JsonParser().parse("{\"x-fapi-financial-id\":\"foo-bar\"}").getAsJsonObject());

		cond.execute(env);

		JsonObject req = env.getObject("resource_endpoint_request_headers");

		assertNotNull(req);
		assertTrue(req.has("x-fapi-financial-id"));
		assertEquals(financialId, OIDFJSON.getString(req.get("x-fapi-financial-id")));

	}


	@Test(expected = ConditionError.class)
	public void testEvaluate_noFinancialId() {

		env.putObject("resource", new JsonObject());

		cond.execute(env);

	}
}
