package net.openid.conformance.condition.as;

import java.time.Instant;

import net.openid.conformance.testmodule.OIDFJSON;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class CreateIntrospectionResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject resource;

	private JsonObject introspectionRequest;

	private JsonObject introspectionRequestBadToken;

	private String accessTokenValue;

	private String clientId;

	private String scope;

	private CreateIntrospectionResponse cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new CreateIntrospectionResponse();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		accessTokenValue = "foo1234556";

		clientId = "client087234";

		scope = "foo bar baz";

		resource = new JsonParser().parse("{\n" +
			"  \"scope\": \"" + scope + "\"\n" +
			"}").getAsJsonObject();

		introspectionRequest = new JsonParser().parse("{\n" +
			"  \"query_string_params\":\n" +
			"  {\n" +
			"	\"token\": \"" + accessTokenValue + "\"\n" +
			"  }\n" +
			"}").getAsJsonObject();

		introspectionRequestBadToken = new JsonParser().parse("{\n" +
			"  \"query_string_params\":\n" +
			"  {\n" +
			"	\"token\": \"" + RandomStringUtils.randomAlphanumeric(10) + "\"\n" +
			"  }\n" +
			"}").getAsJsonObject();

	}

	@Test
	public void testEvaluate() {

		env.putObject("introspection_request", introspectionRequest);
		env.putObject("resource", resource);
		env.putString("access_token", accessTokenValue);
		env.putString("client_id", clientId);

		cond.execute(env);

		JsonObject res = env.getObject("introspection_response");

		assertNotNull(res);
		assertTrue(res.has("active"));
		assertTrue(res.has("scope"));
		assertTrue(res.has("client_id"));
		assertTrue(res.has("exp"));

		assertTrue(OIDFJSON.getBoolean(res.get("active").getAsJsonPrimitive()));
		assertEquals(scope, OIDFJSON.getString(res.get("scope").getAsJsonPrimitive()));
		assertEquals(clientId, OIDFJSON.getString(res.get("client_id").getAsJsonPrimitive()));

		Instant exp = Instant.ofEpochSecond(OIDFJSON.getLong(res.get("exp").getAsJsonPrimitive()));
		Instant now = Instant.now();

		// give a little bit of leeway
		assertTrue(exp.isAfter(now.minusSeconds(1)));

	}


	@Test
	public void testEvaluate_badToken() {

		env.putObject("introspection_request", introspectionRequestBadToken);
		env.putObject("resource", resource);
		env.putString("access_token", accessTokenValue);
		env.putString("client_id", clientId);

		cond.execute(env);

		JsonObject res = env.getObject("introspection_response");

		assertNotNull(res);
		assertTrue(res.has("active"));
		assertFalse(res.has("scope"));
		assertFalse(res.has("client_id"));
		assertFalse(res.has("exp"));

		assertFalse(OIDFJSON.getBoolean(res.get("active")));

	}

}
