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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * @author srmoore
 */
@RunWith(MockitoJUnitRunner.class)
public class CallDynamicRegistrationEndpoint_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private static JsonObject requestParameters = new JsonParser().parse("{"
		+ "\"client_name\":\"UNIT-TEST client\","
		+ "\"grant_types\":[\"authorization_code\"],"
		+ "\"redirect_uris\":[\"https://redirecturi.com/\"]"
		+ "}").getAsJsonObject();

	private static JsonObject goodResponse = new JsonParser().parse("{" +
		"\"client_id\":\"UNIT-TEST-CLIENT-ID\"," +
		"\"client_secret\":\"UNIT-TEST-CLIENT-SECRET\"," +
		"\"client_secret_expires_at\":0," +
		"\"client_id_issued_at\":1525119671," +
		"\"registration_access_token\":\"reg.access.token\"," +
		"\"registration_client_uri\":\"https://good.example.com/register/UNIT-TEST-CLIENT-ID\"," +
		"\"redirect_uris\":[\"https://redirecturi.com/\"]," +
		"\"client_name\":\"UNIT-TEST client\"," +
		"\"token_endpoint_auth_method\":\"client_secret_basic\"," +
		"\"scope\":\"openid email profile\"," +
		"\"grant_types\":[\"authorization_code\"]," +
		"\"response_types\":[\"code\"]} ").getAsJsonObject();

	private static JsonObject goodResponseNoRegistrationAPI = new JsonParser().parse("{" +
		"\"client_id\":\"UNIT-TEST-CLIENT-ID\"," +
		"\"client_secret\":\"UNIT-TEST-CLIENT-SECRET\"," +
		"\"client_secret_expires_at\":0," +
		"\"client_id_issued_at\":1525119671," +
		"\"redirect_uris\":[\"https://redirecturi.com/\"]," +
		"\"client_name\":\"UNIT-TEST client\"," +
		"\"token_endpoint_auth_method\":\"client_secret_basic\"," +
		"\"scope\":\"openid email profile\"," +
		"\"grant_types\":[\"authorization_code\"]," +
		"\"response_types\":[\"code\"]} ").getAsJsonObject();

	@ClassRule
	public static HoverflyRule hoverfly = HoverflyRule.inSimulationMode(dsl(
		service("good.example.com")
			.post("/registration")
			.anyBody()
			.willReturn(success(goodResponse.toString(), "application/json")),
		service("noregapi.example.com")
			.post("/registration")
			.anyBody()
			.willReturn(success(goodResponseNoRegistrationAPI.toString(), "application/json")),
		service("error.example.com")
			.post("/registration")
			.anyBody()
			.willReturn(badRequest()),
		service("bad.example.com")
			.post("/registration")
			.anyBody()
			.willReturn(success("This is not JSON!", "text/plain")),
		service("empty.example.com")
			.post("/registration")
			.anyBody()
			.willReturn(success())));

	private CallDynamicRegistrationEndpoint cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		hoverfly.resetJournal();

		cond = new CallDynamicRegistrationEndpoint("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CallDynamicRegistrationEndpoint#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		JsonObject server = new JsonParser().parse("{"
			+ "\"registration_endpoint\":\"https://good.example.com/registration\""
			+ "}").getAsJsonObject();
		env.put("server", server);

		env.put("dynamic_registration_request", requestParameters);


		cond.evaluate(env);

		hoverfly.verify(service("good.example.com")
			.post("/registration")
			.body(requestParameters.toString()));

		verify(env, atLeastOnce()).getString("server", "registration_endpoint");

		assertThat(env.get("client")).isInstanceOf(JsonObject.class);
		assertThat(env.get("client").entrySet()).containsAll(goodResponse.entrySet());
		assertThat(env.getString("registration_client_uri")).isEqualToIgnoringCase("https://good.example.com/register/UNIT-TEST-CLIENT-ID");
		assertThat(env.getString("registration_access_token")).isEqualToIgnoringCase("reg.access.token");
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CallDynamicRegistrationEndpoint#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_noError_noRegistrationClientUri() {

		JsonObject server = new JsonParser().parse("{"
			+ "\"registration_endpoint\":\"https://noregapi.example.com/registration\""
			+ "}").getAsJsonObject();
		env.put("server", server);

		env.put("dynamic_registration_request", requestParameters);


		cond.evaluate(env);

		hoverfly.verify(service("noregapi.example.com")
			.post("/registration")
			.body(requestParameters.toString()));

		verify(env, atLeastOnce()).getString("server", "registration_endpoint");

		assertThat(env.get("client")).isInstanceOf(JsonObject.class);
		assertThat(env.get("client").entrySet()).containsAll(goodResponseNoRegistrationAPI.entrySet());
		assertThat(env.getString("registration_client_uri")).isNullOrEmpty();
		assertThat(env.getString("registration_access_token")).isNullOrEmpty();
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CallDynamicRegistrationEndpoint#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_noServerRegistraitonEndpoint(){
		JsonObject server = new JsonParser().parse("{\"not_registration_endpoint\":\"foo\"}").getAsJsonObject();
		env.put("server",server);
		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CallDynamicRegistrationEndpoint#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testBadRequestResponseFromServer(){

		JsonObject server = new JsonParser().parse("{"
			+ "\"registration_endpoint\":\"https://error.example.com/registration\""
			+ "}").getAsJsonObject();
		env.put("server", server);

		env.put("dynamic_registration_request", requestParameters);

		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CallDynamicRegistrationEndpoint#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testInvalidJsonReturnedFromServer(){
		JsonObject server = new JsonParser().parse("{"
			+ "\"registration_endpoint\":\"https://bad.example.com/registration\""
			+ "}").getAsJsonObject();
		env.put("server", server);

		env.put("dynamic_registration_request", requestParameters);

		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CallDynamicRegistrationEndpoint#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEmptyBodyReturnedFromServer(){
		JsonObject server = new JsonParser().parse("{"
			+ "\"registration_endpoint\":\"https://empty.example.com/registration\""
			+ "}").getAsJsonObject();
		env.put("server", server);

		env.put("dynamic_registration_request", requestParameters);

		cond.evaluate(env);
	}
}
