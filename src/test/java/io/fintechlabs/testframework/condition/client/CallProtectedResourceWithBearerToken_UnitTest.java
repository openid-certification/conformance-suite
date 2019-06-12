package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import io.specto.hoverfly.junit.rule.HoverflyRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class CallProtectedResourceWithBearerToken_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	// Examples from RFC 6749

	private static JsonObject bearerToken = new JsonParser().parse("{"
		+ "\"value\":\"mF_9.B5f-4.1JqM\","
		+ "\"type\":\"Bearer\""
		+ "}").getAsJsonObject();

	private static JsonObject exampleToken = new JsonParser().parse("{"
		+ "\"value\":\"2YotnFZFEjr1zCsicMWpAA\","
		+ "\"type\":\"example\""
		+ "}").getAsJsonObject();

	@ClassRule
	public static HoverflyRule hoverfly = HoverflyRule.inSimulationMode(dsl(
		service("example.com")
			.get("/resource")
			.header("Authorization", "Bearer mF_9.B5f-4.1JqM")
			.willReturn(success("OK", "text/plain")),
		service("example.com")
			.post("/resource")
			.header("Authorization", "Bearer mF_9.B5f-4.1JqM")
			.willReturn(success("OK", "text/plain"))));

	private CallProtectedResourceWithBearerToken cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		hoverfly.resetJournal();

		cond = new CallProtectedResourceWithBearerToken();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		env.putObject("resource", new JsonObject());
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CallAccountsEndpointWithBearerToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		env.putObject("access_token", bearerToken);
		env.getObject("resource").addProperty("resourceUrl", "http://example.com/resource");

		cond.evaluate(env);

		hoverfly.verify(service("example.com")
			.get("/resource")
			.header("Authorization", "Bearer mF_9.B5f-4.1JqM"));

		verify(env, atLeastOnce()).getString("access_token", "value");
		verify(env, atLeastOnce()).getString("access_token", "type");
		verify(env, atLeastOnce()).getString("resource", "resourceUrl");

		assertThat(env.getString("resource_endpoint_response")).isEqualTo("OK");
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CallAccountsEndpointWithBearerToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_noErrorPost() {

		env.putObject("access_token", bearerToken);
		env.getObject("resource").addProperty("resourceUrl", "http://example.com/resource");
		env.getObject("resource").addProperty("resourceMethod","POST");

		cond.evaluate(env);

		hoverfly.verify(service("example.com")
			.post("/resource")
			.header("Authorization", "Bearer mF_9.B5f-4.1JqM"));

		verify(env, atLeastOnce()).getString("access_token", "value");
		verify(env, atLeastOnce()).getString("access_token", "type");
		verify(env, atLeastOnce()).getString("resource", "resourceUrl");

		assertThat(env.getString("resource_endpoint_response")).isEqualTo("OK");
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CallAccountsEndpointWithBearerToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_badToken() {

		env.putObject("access_token", exampleToken);
		env.getObject("resource").addProperty("resourceUrl", "http://example.com/resource");

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CallAccountsEndpointWithBearerToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_badServer() {

		env.putObject("access_token", bearerToken);
		env.getObject("resource").addProperty("resourceUrl", "http://invalid.org/");

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CallAccountsEndpointWithBearerToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingToken() {

		env.getObject("resource").addProperty("resourceUrl", "http://example.com/resource");

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CallAccountsEndpointWithBearerToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingUrl() {

		env.putObject("access_token", bearerToken);

		cond.evaluate(env);

	}
}
