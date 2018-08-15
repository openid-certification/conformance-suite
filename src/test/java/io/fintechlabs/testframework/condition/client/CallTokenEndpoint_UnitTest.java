package io.fintechlabs.testframework.condition.client;

import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.badRequest;
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
public class CallTokenEndpoint_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private static JsonObject requestParameters = new JsonParser().parse("{"
		+ "\"grant_type\":\"client_credentials\""
		+ "}").getAsJsonObject();

	private static JsonObject requestHeaders = new JsonParser().parse("{"
		+ "\"Authorization\":\"Basic czZCaGRSa3F0MzpnWDFmQmF0M2JW\""
		+ "}").getAsJsonObject();

	private static JsonObject goodResponse = new JsonParser().parse("{"
		+ "\"access_token\":\"2YotnFZFEjr1zCsicMWpAA\","
		+ "\"token_type\":\"example\","
		+ "\"expires_in\":3600,"
		+ "\"example_parameter\":\"example_value\""
		+ "}").getAsJsonObject();

	@ClassRule
	public static HoverflyRule hoverfly = HoverflyRule.inSimulationMode(dsl(
		service("good.example.com")
			.post("/token")
			.anyBody()
			.willReturn(success(goodResponse.toString(), "application/json")),
		service("error.example.com")
			.post("/token")
			.anyBody()
			.willReturn(badRequest()),
		service("bad.example.com")
			.post("/token")
			.anyBody()
			.willReturn(success("This is not JSON!", "text/plain")),
		service("empty.example.com")
			.post("/token")
			.anyBody()
			.willReturn(success("", "application/json"))));

	private CallTokenEndpoint cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		hoverfly.resetJournal();

		cond = new CallTokenEndpoint("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CallTokenEndpoint#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		JsonObject server = new JsonParser().parse("{"
			+ "\"token_endpoint\":\"https://good.example.com/token\""
			+ "}").getAsJsonObject();
		env.put("server", server);

		env.put("token_endpoint_request_form_parameters", requestParameters);
		env.put("token_endpoint_request_headers", requestHeaders);

		cond.evaluate(env);

		hoverfly.verify(service("good.example.com")
			.post("/token")
			.header("Authorization", "Basic czZCaGRSa3F0MzpnWDFmQmF0M2JW")
			.body("grant_type=client_credentials"));

		verify(env, atLeastOnce()).getString("server", "token_endpoint");

		assertThat(env.getObject("token_endpoint_response")).isInstanceOf(JsonObject.class);
		assertThat(env.getObject("token_endpoint_response").entrySet()).containsAll(goodResponse.entrySet());
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CallTokenEndpoint#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_noHeaders() {

		/* A normal server would refuse this request, but we want to make sure the condition doesn't fail */

		JsonObject server = new JsonParser().parse("{"
			+ "\"token_endpoint\":\"https://good.example.com/token\""
			+ "}").getAsJsonObject();
		env.put("server", server);

		env.put("token_endpoint_request_form_parameters", requestParameters);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CallTokenEndpoint#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_nonexistingServer() {

		JsonObject server = new JsonParser().parse("{"
			+ "\"token_endpoint\":\"https://nonexisting.example.com/token\""
			+ "}").getAsJsonObject();
		env.put("server", server);

		env.put("token_endpoint_request_form_parameters", requestParameters);
		env.put("token_endpoint_request_headers", requestHeaders);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CallTokenEndpoint#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_errorResponse() {

		JsonObject server = new JsonParser().parse("{"
			+ "\"token_endpoint\":\"https://error.example.com/token\""
			+ "}").getAsJsonObject();
		env.put("server", server);

		env.put("token_endpoint_request_form_parameters", requestParameters);
		env.put("token_endpoint_request_headers", requestHeaders);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CallTokenEndpoint#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_badResponse() {

		JsonObject server = new JsonParser().parse("{"
			+ "\"token_endpoint\":\"https://bad.example.com/token\""
			+ "}").getAsJsonObject();
		env.put("server", server);

		env.put("token_endpoint_request_form_parameters", requestParameters);
		env.put("token_endpoint_request_headers", requestHeaders);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CallTokenEndpoint#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_emptyResponse() {

		JsonObject server = new JsonParser().parse("{"
			+ "\"token_endpoint\":\"https://empty.example.com/token\""
			+ "}").getAsJsonObject();
		env.put("server", server);

		env.put("token_endpoint_request_form_parameters", requestParameters);
		env.put("token_endpoint_request_headers", requestHeaders);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CallTokenEndpoint#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_requestMissing() {

		JsonObject server = new JsonParser().parse("{"
			+ "\"token_endpoint\":\"https://good.example.com/token\""
			+ "}").getAsJsonObject();
		env.put("server", server);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CallTokenEndpoint#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_configMissing() {

		env.put("token_endpoint_request_form_parameters", requestParameters);
		env.put("token_endpoint_request_headers", requestHeaders);

		cond.evaluate(env);

	}
}
