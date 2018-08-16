package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
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
import static io.specto.hoverfly.junit.dsl.ResponseCreators.badRequest;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;

/**
 * @author srmoore
 */
@RunWith(MockitoJUnitRunner.class)
public class CallRevocationEndpoint_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private static JsonObject revocationEndpointRequestFormParameters = new JsonParser()
		.parse("{\"token\":\"2YotnFZFEjr1zCsicMWpAA\"}")
		.getAsJsonObject();

	private static JsonObject requestHeaders = new JsonParser().parse("{"
		+ "\"Authorization\":\"Basic czZCaGRSa3F0MzpnWDFmQmF0M2JW\""
		+ "}").getAsJsonObject();

	@ClassRule
	public static HoverflyRule hoverfly = HoverflyRule.inSimulationMode(dsl(
		service("good.example.com")
			.post("/revoke")
			.anyBody()
			.willReturn(success()),
		service("error.example.com")
			.post("/revoke")
			.anyBody()
			.willReturn(badRequest())));

	private CallRevocationEndpoint cond;

	@Before
	public void setUp(){

		hoverfly.resetJournal();
		cond = new CallRevocationEndpoint("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noError() {
		JsonObject server = new JsonParser().parse("{"
			+ "\"revocation_endpoint\":\"https://good.example.com/revoke\""
			+ "}").getAsJsonObject();
		env.putObject("server",server);
		env.putObject("revocation_endpoint_request_form_parameters", revocationEndpointRequestFormParameters);
		env.putObject("revocation_endpoint_request_headers", requestHeaders);

		cond.evaluate(env);
		hoverfly.verify(service("good.example.com")
			.post("/revoke")
			.header("Authorization", "Basic czZCaGRSa3F0MzpnWDFmQmF0M2JW")
			.body("token=2YotnFZFEjr1zCsicMWpAA"));
	}

	@Test
	public void testEvaluate_noError_noHeaders() {
		JsonObject server = new JsonParser().parse("{"
			+ "\"revocation_endpoint\":\"https://good.example.com/revoke\""
			+ "}").getAsJsonObject();
		env.putObject("server",server);
		env.putObject("revocation_endpoint_request_form_parameters", revocationEndpointRequestFormParameters);

		cond.evaluate(env);
		hoverfly.verify(service("good.example.com")
			.post("/revoke")
			.body("token=2YotnFZFEjr1zCsicMWpAA"));
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_serverResponseError(){
		JsonObject server = new JsonParser().parse("{"
			+ "\"revocation_endpoint\":\"https://error.example.com/revoke\""
			+ "}").getAsJsonObject();
		env.putObject("server",server);
		env.putObject("revocation_endpoint_request_form_parameters", revocationEndpointRequestFormParameters);

		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_noRevocationEndpoint(){
		JsonObject server = new JsonParser().parse("{"
			+ "\"not_specifiying_revocation_endpoint\":\"https://error.example.com/revoke\""
			+ "}").getAsJsonObject();
		env.putObject("server",server);
		env.putObject("revocation_endpoint_request_form_parameters", revocationEndpointRequestFormParameters);

		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_noFormParameters(){
		JsonObject server = new JsonParser().parse("{"
			+ "\"revocation_endpoint\":\"https://error.example.com/revoke\""
			+ "}").getAsJsonObject();
		env.putObject("server",server);

		cond.evaluate(env);
	}
}
