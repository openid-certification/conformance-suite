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
import org.mockito.runners.MockitoJUnitRunner;

import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.unauthorised;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * @author srmoore
 */
@RunWith(MockitoJUnitRunner.class)
public class CallProtectedResourceWithInactiveBearerToken_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	// Examples from RFC 6749

	private static JsonObject bearerToken = new JsonParser().parse("{"
		+ "\"value\":\"mF_9.B5f-4.1JqM\","
		+ "\"type\":\"Bearer\""
		+ "}").getAsJsonObject();

	private static JsonObject inactiveBearerToken = new JsonParser().parse("{"
		+ "\"value\":\"mF_9.B5f-4.1JqM_inactive\","
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
			.get("/resource")
			.header("Authorization", "Bearer mF_9.B5f-4.1JqM_inactive")
			.willReturn(unauthorised()),
		service("example.com")
			.post("/resource")
			.header("Authorization", "Bearer mF_9.B5f-4.1JqM_inactive")
			.willReturn(unauthorised()),
		service("example.com")
			.post("/resource")
			.header("Authorization", "Bearer mF_9.B5f-4.1JqM")
			.willReturn(success("OK", "text/plain"))));

	private CallProtectedResourceWithInactiveBearerToken cond;

	/**
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {

		hoverfly.resetJournal();

		cond = new CallProtectedResourceWithInactiveBearerToken("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		env.put("resource", new JsonObject());
	}

	/**
	 * Test method for {@link CallAccountsEndpointWithBearerToken#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_resourceServerAccepts() {

		env.put("access_token", bearerToken);
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
	 * Test method for {@link CallAccountsEndpointWithBearerToken#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_resourceServerAcceptsPost() {

		env.put("access_token", bearerToken);
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
	 * Test method for {@link CallAccountsEndpointWithBearerToken#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		env.put("access_token", inactiveBearerToken);
		env.getObject("resource").addProperty("resourceUrl", "http://example.com/resource");

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link CallAccountsEndpointWithBearerToken#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noErrorPost() {

		env.put("access_token", inactiveBearerToken);
		env.getObject("resource").addProperty("resourceUrl", "http://example.com/resource");
		env.getObject("resource").addProperty("resourceMethod", "POST");

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link CallAccountsEndpointWithBearerToken#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_badServer() {

		env.put("access_token", bearerToken);
		env.getObject("resource").addProperty("resourceUrl", "http://invalid.org/");

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link CallAccountsEndpointWithBearerToken#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingToken() {

		env.getObject("resource").addProperty("resourceUrl", "http://example.com/resource");

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link CallAccountsEndpointWithBearerToken#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingUrl() {

		env.put("access_token", bearerToken);

		cond.evaluate(env);

	}
}
