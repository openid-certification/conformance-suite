package io.fintechlabs.testframework.condition.client;

import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import io.specto.hoverfly.junit.rule.HoverflyRule;

@RunWith(MockitoJUnitRunner.class)
public class GetDynamicServerConfiguration_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	@ClassRule
	public static HoverflyRule hoverfly = HoverflyRule.inSimulationMode(dsl(
		service("good.example.com")
			.get("/.well-known/openid-configuration")
			.willReturn(success("{\"jwks_uri\":\"https://good.example.com/jwks.json\"}", "application/json")),
		service("bad.example.com")
			.get("/.well-known/openid-configuration")
			.willReturn(success("This is not JSON!", "text/plain")),
		service("empty.example.com")
			.get("/.well-known/openid-configuration")
			.willReturn(success("", "application/json"))));

	private GetDynamicServerConfiguration cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		hoverfly.resetJournal();

		cond = new GetDynamicServerConfiguration();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.GetDynamicServerConfiguration#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		JsonObject config = new JsonParser().parse("{"
			+ "\"server\":{"
			+ "\"discoveryUrl\":\"https://good.example.com/.well-known/openid-configuration\""
			+ "}}").getAsJsonObject();
		env.putObject("config", config);

		cond.evaluate(env);

		hoverfly.verify(service("good.example.com").get("/.well-known/openid-configuration"));

		verify(env, atLeastOnce()).getString("config", "server.discoveryUrl");

		assertThat(env.getObject("server")).isNotNull();
		assertThat(env.getString("server", "jwks_uri")).isEqualTo("https://good.example.com/jwks.json");
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.GetDynamicServerConfiguration#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_fallbackToIssuer() {

		JsonObject config = new JsonParser().parse("{"
			+ "\"server\":{"
			+ "\"discoveryIssuer\":\"https://good.example.com\""
			+ "}}").getAsJsonObject();
		env.putObject("config", config);

		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.GetDynamicServerConfiguration#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_discoveryUrlTakesPriority() {

		JsonObject config = new JsonParser().parse("{"
			+ "\"server\":{"
			+ "\"discoveryUrl\":\"https://good.example.com/not-here\","
			+ "\"issuer\":\"https://good.example.com\""
			+ "}}").getAsJsonObject();
		env.putObject("config", config);

		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.GetDynamicServerConfiguration#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_noServer() {

		JsonObject config = new JsonParser().parse("{"
			+ "\"server\":{"
			+ "\"discoveryIssuer\":\"https://nonexisting.example.com\""
			+ "}}").getAsJsonObject();
		env.putObject("config", config);

		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.GetDynamicServerConfiguration#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_badResponse() {

		JsonObject config = new JsonParser().parse("{"
			+ "\"server\":{"
			+ "\"discoveryIssuer\":\"https://bad.example.com\""
			+ "}}").getAsJsonObject();
		env.putObject("config", config);

		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.GetDynamicServerConfiguration#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_emptyResponse() {

		JsonObject config = new JsonParser().parse("{"
			+ "\"server\":{"
			+ "\"discoveryIssuer\":\"https://empty.example.com\""
			+ "}}").getAsJsonObject();
		env.putObject("config", config);

		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.GetDynamicServerConfiguration#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingUrls() {

		JsonObject config = new JsonParser().parse("{\"server\":{}}").getAsJsonObject();
		env.putObject("config", config);

		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.GetDynamicServerConfiguration#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingConfig() {

		cond.evaluate(env);
	}
}
