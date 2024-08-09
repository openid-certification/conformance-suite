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
import static io.specto.hoverfly.junit.dsl.matchers.HoverflyMatchers.any;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(HoverflyExtension.class)
@ExtendWith(MockitoExtension.class)
public class DisallowAccessTokenInQuery_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	// Examples from RFC 6749

	private static JsonObject bearerToken = JsonParser.parseString("{"
		+ "\"value\":\"mF_9.B5f-4.1JqM\","
		+ "\"type\":\"Bearer\""
		+ "}").getAsJsonObject();

	private DisallowAccessTokenInQuery cond;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp(Hoverfly hoverfly) throws Exception {

		hoverfly.simulate(dsl(
			service("good.example.com")
				.get("/accounts")
				.queryParam("access_token", any())
				.willReturn(badRequest().body("Bad Request")),
			service("bad.example.com")
				.get("/accounts")
				.queryParam("access_token", "mF_9.B5f-4.1JqM")
				.willReturn(success("OK", "text/plain"))));
		hoverfly.resetJournal();

		cond = new DisallowAccessTokenInQuery();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		env.putObject("resource", new JsonObject());
	}

	@Test
	public void testEvaluate_noError(Hoverfly hoverfly) {

		env.putObject("access_token", bearerToken);
		env.putString("protected_resource_url", "http://good.example.com/accounts");

		cond.execute(env);

		hoverfly.verify(service("good.example.com")
			.get("/accounts")
			.queryParam("access_token", "mF_9.B5f-4.1JqM"));

		verify(env, atLeastOnce()).getString("access_token", "value");
		verify(env, atLeastOnce()).getString("protected_resource_url");
	}

	/**
	 * Test method for {@link DisallowAccessTokenInQuery#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_disallowedQueryAccepted() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("access_token", bearerToken);
			env.putString("protected_resource_url", "http://bad.example.com/accounts");

			cond.execute(env);

		});

	}

	/**
	 * Test method for {@link DisallowAccessTokenInQuery#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_badServer() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("access_token", bearerToken);
			env.putString("protected_resource_url", "http://invalid.org/accounts");

			cond.execute(env);

		});

	}

	/**
	 * Test method for {@link DisallowAccessTokenInQuery#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_missingToken() {
		assertThrows(ConditionError.class, () -> {

			env.putString("protected_resource_url", "http://good.example.com/accounts");

			cond.execute(env);

		});

	}

	/**
	 * Test method for {@link DisallowAccessTokenInQuery#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_missingUrl() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("access_token", bearerToken);

			cond.execute(env);

		});

	}

}
