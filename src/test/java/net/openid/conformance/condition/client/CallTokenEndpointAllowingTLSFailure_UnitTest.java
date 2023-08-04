package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import io.specto.hoverfly.junit.dsl.HoverflyDsl;
import io.specto.hoverfly.junit.rule.HoverflyRule;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.badRequest;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class CallTokenEndpointAllowingTLSFailure_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private static JsonObject requestParameters = JsonParser.parseString("{"
		+ "\"grant_type\":\"client_credentials\""
		+ "}").getAsJsonObject();

	private static JsonObject requestHeaders = JsonParser.parseString("{"
		+ "\"Authorization\":\"Basic czZCaGRSa3F0MzpnWDFmQmF0M2JW\""
		+ "}").getAsJsonObject();

	private static JsonObject goodResponse = JsonParser.parseString("{"
		+ "\"access_token\":\"2YotnFZFEjr1zCsicMWpAA\","
		+ "\"token_type\":\"example\","
		+ "\"expires_in\":3600,"
		+ "\"example_parameter\":\"example_value\""
		+ "}").getAsJsonObject();

	private static String thisIsNotJsonText = "This is not JSON!";

	@ClassRule
	public static HoverflyRule hoverfly;

	static {
		hoverfly = HoverflyRule.inSimulationMode(dsl(
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
				.willReturn(success(thisIsNotJsonText, "text/plain")),
			service("notauth.example.com")
				.post("/token")
				.anyBody()
				.willReturn(HoverflyDsl.response().status(401).body(thisIsNotJsonText).header("Content-Type", "text/plain")),
			service("empty.example.com")
				.post("/token")
				.anyBody()
				.willReturn(success("", "application/json"))));
	}

	private CallTokenEndpointAllowingTLSFailure cond;

	@Before
	public void setUp() throws Exception {

		hoverfly.resetJournal();

		cond = new CallTokenEndpointAllowingTLSFailure();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noError() {

		JsonObject server = JsonParser.parseString("{"
			+ "\"token_endpoint\":\"https://good.example.com/token\""
			+ "}").getAsJsonObject();
		env.putObject("server", server);

		env.putObject("token_endpoint_request_form_parameters", requestParameters);
		env.putObject("token_endpoint_request_headers", requestHeaders);

		cond.execute(env);

		hoverfly.verify(service("good.example.com")
			.post("/token")
			.header("Authorization", "Basic czZCaGRSa3F0MzpnWDFmQmF0M2JW")
			.body("grant_type=client_credentials"));

		verify(env, atLeastOnce()).getString("server", "token_endpoint");

		assertThat(env.getObject("token_endpoint_response")).isInstanceOf(JsonObject.class);
		assertThat(env.getObject("token_endpoint_response").entrySet()).containsAll(goodResponse.entrySet());
	}

	@Test
	public void testEvaluate_noHeaders() {

		/* A normal server would refuse this request, but we want to make sure the condition doesn't fail */

		JsonObject server = JsonParser.parseString("{"
			+ "\"token_endpoint\":\"https://good.example.com/token\""
			+ "}").getAsJsonObject();
		env.putObject("server", server);

		env.putObject("token_endpoint_request_form_parameters", requestParameters);

		cond.execute(env);

	}

	@Test
	public void testEvaluate_nonexistingServer() {

		// this actually results in hoverfly returning a 502 bad gateway error
		JsonObject server = JsonParser.parseString("{"
			+ "\"token_endpoint\":\"https://nonexisting.example.com/token\""
			+ "}").getAsJsonObject();
		env.putObject("server", server);

		env.putObject("token_endpoint_request_form_parameters", requestParameters);
		env.putObject("token_endpoint_request_headers", requestHeaders);

		cond.execute(env);

		assertThat(env.getElementFromObject("token_endpoint_response_full", "status")).isEqualTo(new JsonPrimitive(502));

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_errorResponse() {

		JsonObject server = JsonParser.parseString("{"
			+ "\"token_endpoint\":\"https://error.example.com/token\""
			+ "}").getAsJsonObject();
		env.putObject("server", server);

		env.putObject("token_endpoint_request_form_parameters", requestParameters);
		env.putObject("token_endpoint_request_headers", requestHeaders);

		cond.execute(env);

	}

	/** Note that, although the server is returning a non-json response, it is expected that the condition
	 * returns success and leaves the caller to determine what to do with a non-json response.
	 */
	@Test
	public void testEvaluate_badResponse() {

		JsonObject server = JsonParser.parseString("{"
			+ "\"token_endpoint\":\"https://bad.example.com/token\""
			+ "}").getAsJsonObject();
		env.putObject("server", server);

		env.putObject("token_endpoint_request_form_parameters", requestParameters);
		env.putObject("token_endpoint_request_headers", requestHeaders);

		cond.execute(env);

		assertThat(env.getObject("token_endpoint_response_full")).isInstanceOf(JsonObject.class);
		assertThat(env.getElementFromObject("token_endpoint_response_full", "status")).isEqualTo(new JsonPrimitive(200));
		assertThat(env.getElementFromObject("token_endpoint_response_full", "body")).isEqualTo(new JsonPrimitive(thisIsNotJsonText));
		assertThat(env.getElementFromObject("token_endpoint_response_full", "body_json")).isNull();
	}

	@Test
	public void testEvaluate_notAuthorized() {

		env.putString("server", "token_endpoint", "https://notauth.example.com/token");

		env.putObject("token_endpoint_request_form_parameters", requestParameters);
		env.putObject("token_endpoint_request_headers", requestHeaders);

		cond.execute(env);

		assertThat(env.getObject("token_endpoint_response_full")).isInstanceOf(JsonObject.class);
		assertThat(env.getElementFromObject("token_endpoint_response_full", "status")).isEqualTo(new JsonPrimitive(401));
		assertThat(env.getElementFromObject("token_endpoint_response_full", "body")).isEqualTo(new JsonPrimitive(thisIsNotJsonText));
		assertThat(env.getElementFromObject("token_endpoint_response_full", "body_json")).isNull();
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_emptyResponse() {

		JsonObject server = JsonParser.parseString("{"
			+ "\"token_endpoint\":\"https://empty.example.com/token\""
			+ "}").getAsJsonObject();
		env.putObject("server", server);

		env.putObject("token_endpoint_request_form_parameters", requestParameters);
		env.putObject("token_endpoint_request_headers", requestHeaders);

		cond.execute(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_requestMissing() {

		JsonObject server = JsonParser.parseString("{"
			+ "\"token_endpoint\":\"https://good.example.com/token\""
			+ "}").getAsJsonObject();
		env.putObject("server", server);

		cond.execute(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_configMissing() {

		env.putObject("token_endpoint_request_form_parameters", requestParameters);
		env.putObject("token_endpoint_request_headers", requestHeaders);

		cond.execute(env);

	}
}
