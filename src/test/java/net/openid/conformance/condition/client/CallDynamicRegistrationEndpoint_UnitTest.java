package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.specto.hoverfly.junit.core.Hoverfly;
import io.specto.hoverfly.junit5.HoverflyExtension;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.badRequest;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;
import io.specto.hoverfly.junit.verification.HoverflyVerificationError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@ExtendWith(HoverflyExtension.class)
public class CallDynamicRegistrationEndpoint_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private static JsonObject requestParameters = JsonParser.parseString("{"
		+ "\"client_name\":\"UNIT-TEST client São Paulo\","
		+ "\"grant_types\":[\"authorization_code\"],"
		+ "\"redirect_uris\":[\"https://redirecturi.com/\"]"
		+ "}").getAsJsonObject();

	private static JsonObject goodResponse = JsonParser.parseString("{" +
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

	private static JsonObject goodResponseNoRegistrationAPI = JsonParser.parseString("{" +
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

	private CallDynamicRegistrationEndpoint cond;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp(Hoverfly hoverfly) throws Exception {

		hoverfly.simulate(dsl(
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
		hoverfly.resetJournal();

		cond = new CallDynamicRegistrationEndpoint();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test method for {@link CallDynamicRegistrationEndpoint#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noError(Hoverfly hoverfly) {
		String requestAccessToken = "mF_9.B5f-4.1JqM";

		JsonObject server = JsonParser.parseString("{"
			+ "\"registration_endpoint\":\"https://good.example.com/registration\""
			+ "}").getAsJsonObject();
		env.putObject("server", server);

		env.putObject("dynamic_registration_request", requestParameters);

		// Optional access token to be included in the authorization header.
		env.putString("initial_access_token", requestAccessToken);


		cond.execute(env);

		hoverfly.verify(service("good.example.com")
			.post("/registration")
			.body(requestParameters.toString())
			.header("Authorization", "Bearer " + requestAccessToken));

		verify(env, atLeastOnce()).getString("server", "registration_endpoint");

		assertThat(env.getObject("dynamic_registration_endpoint_response")).isInstanceOf(JsonObject.class);
		assertThat(((JsonObject)env.getElementFromObject("dynamic_registration_endpoint_response", "body_json")).entrySet()).containsAll(goodResponse.entrySet());
	}

	/**
	 * Test method for {@link CallDynamicRegistrationEndpoint#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noError_noRegistrationClientUri(Hoverfly hoverfly) {

		JsonObject server = JsonParser.parseString("{"
			+ "\"registration_endpoint\":\"https://noregapi.example.com/registration\""
			+ "}").getAsJsonObject();
		env.putObject("server", server);

		env.putObject("dynamic_registration_request", requestParameters);


		cond.execute(env);

		hoverfly.verify(service("noregapi.example.com")
			.post("/registration")
			.body(requestParameters.toString()));

		verify(env, atLeastOnce()).getString("server", "registration_endpoint");

		assertThat(env.getObject("dynamic_registration_endpoint_response")).isInstanceOf(JsonObject.class);
		assertThat(((JsonObject)env.getElementFromObject("dynamic_registration_endpoint_response", "body_json")).entrySet()).containsAll(goodResponseNoRegistrationAPI.entrySet());
	}

	/**
	 * Test method for {@link CallDynamicRegistrationEndpoint#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noError_noInitialAccessToken(Hoverfly hoverfly) {
		JsonObject server = JsonParser.parseString("{"
			+ "\"registration_endpoint\":\"https://good.example.com/registration\""
			+ "}").getAsJsonObject();
		env.putObject("server", server);

		env.putObject("dynamic_registration_request", requestParameters);

		cond.execute(env);

		/*
		 * No initial access token was supplied.
		 *
		 * Verify the request did not contain an authorization header field.
		 */
		hoverfly.verify(service("good.example.com")
			.post("/registration")
			.anyBody(),

			(request, data) -> {
				data.getJournal().getEntries().forEach(entry -> {
					if (entry.getRequest().getHeaders().containsKey("Authorization")) {
						throw new HoverflyVerificationError("Unexpected Authorization Request Header Field.");
					}
				});
			});
	}

	/**
	 * Test method for {@link CallDynamicRegistrationEndpoint#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noServerRegistraitonEndpoint(){
		assertThrows(ConditionError.class, () -> {
			JsonObject server = JsonParser.parseString("{\"not_registration_endpoint\":\"foo\"}").getAsJsonObject();
			env.putObject("server", server);
			cond.execute(env);
		});
	}

	/**
	 * Test method for {@link CallDynamicRegistrationEndpoint#evaluate(Environment)}.
	 */
	@Test
	public void testBadRequestResponseFromServer(){
		assertThrows(ConditionError.class, () -> {

			JsonObject server = JsonParser.parseString("{"
				+ "\"registration_endpoint\":\"https://error.example.com/registration\""
				+ "}").getAsJsonObject();
			env.putObject("server", server);

			env.putObject("dynamic_registration_request", requestParameters);

			cond.execute(env);
		});
	}

	/**
	 * Test method for {@link CallDynamicRegistrationEndpoint#evaluate(Environment)}.
	 */
	@Test
	public void testInvalidJsonReturnedFromServer(){
		assertThrows(ConditionError.class, () -> {
			JsonObject server = JsonParser.parseString("{"
				+ "\"registration_endpoint\":\"https://bad.example.com/registration\""
				+ "}").getAsJsonObject();
			env.putObject("server", server);

			env.putObject("dynamic_registration_request", requestParameters);

			cond.execute(env);
		});
	}

	/**
	 * Test method for {@link CallDynamicRegistrationEndpoint#evaluate(Environment)}.
	 */
	@Test
	public void testEmptyBodyReturnedFromServer(){
		assertThrows(ConditionError.class, () -> {
			JsonObject server = JsonParser.parseString("{"
				+ "\"registration_endpoint\":\"https://empty.example.com/registration\""
				+ "}").getAsJsonObject();
			env.putObject("server", server);

			env.putObject("dynamic_registration_request", requestParameters);

			cond.execute(env);
		});
	}
}
