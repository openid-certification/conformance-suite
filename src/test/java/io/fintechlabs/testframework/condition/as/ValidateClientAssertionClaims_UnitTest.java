package io.fintechlabs.testframework.condition.as;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import java.util.Date;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ValidateClientAssertionClaims_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private long nowSeconds;

	private JsonObject client;

	private String clientId;

	private String audience;

	private JsonObject header;

	private JsonObject claims;

	private JsonObject server;

	private ValidateClientAssertionClaims cond;

	@Before
	public void setUp() throws Exception {

		cond = new ValidateClientAssertionClaims("UNIT-TEST", eventLog, ConditionResult.INFO, new String[0]);

		Date now = new Date();
		nowSeconds = now.getTime() / 1000;
		long issuedAt = nowSeconds - 10; // pretend this came from a distant server

		clientId = "test-client-id-346334adgdsfgdfg3425";

		audience = "https://localhost:8443/test-mtls/a/fintech-clienttest/token";

		client = new JsonParser().parse("{ \"client_id\": \"" + clientId + "\" }").getAsJsonObject();

		server = new JsonParser().parse("{"
			+ "\"issuer\":\"" + audience + "\","
			+ "\"token_endpoint\":\"" + audience + "\""
			+ "}").getAsJsonObject();

		header = new JsonParser().parse("{"
			+ "\"kid\":\"c1df0e7a-031c-4232-9314-8717d9bccdb4\","
			+ "\"typ\":\"JWT\","
			+ "\"alg\":\"PS256\""
			+ "}").getAsJsonObject();

		claims = new JsonParser().parse("{"
			+ "\"iss\":\"" + clientId + "\","
			+ "\"sub\":\"" + clientId + "\","
			+ "\"aud\":\"" + audience + "\","
			+ "\"jti\":\"GIRiuemsZA25YF25N-PXH3T6LJo0KDqG7zWyOZ5QsF4\""
			+ "}").getAsJsonObject();
		claims.addProperty("exp", issuedAt + 300);
		claims.addProperty("iat", issuedAt);
	}

	private void addClientAssertion(Environment env, JsonObject claims) {

		JsonObject clientAssertion = new JsonObject();
		clientAssertion.add("claims", claims);
		env.putObject("client_assertion", clientAssertion);
	}

	@Test
	public void testEvaluate_noError() {

		env.putObject("client", client);
		env.putObject("server", server);
		addClientAssertion(env, claims);

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("client", "client_id");
		verify(env, atLeastOnce()).getString("server", "issuer");
		verify(env, atLeastOnce()).getString("server", "token_endpoint");
		verify(env, atLeastOnce()).getElementFromObject("client_assertion", "claims.iss");
		verify(env, atLeastOnce()).getElementFromObject("client_assertion", "claims.iss");
		verify(env, atLeastOnce()).getElementFromObject("client_assertion", "claims.aud");
		verify(env, atLeastOnce()).getLong("client_assertion", "claims.exp");
		verify(env, atLeastOnce()).getLong("client_assertion", "claims.iat");

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingClientId() {

		env.putObject("server", server);
		addClientAssertion(env, claims);

		cond.evaluate(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingServerConfig() {

		env.putObject("client", client);
		addClientAssertion(env, claims);

		cond.evaluate(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingIdToken() {

		env.putObject("client", client);
		env.putObject("server", server);

		cond.evaluate(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingIssuer() {

		claims.remove("iss");

		env.putObject("client", client);
		env.putObject("server", server);
		addClientAssertion(env, claims);

		cond.evaluate(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidIssuer() {

		claims.remove("iss");
		claims.addProperty("iss", "invalid");

		env.putObject("client", client);
		env.putObject("server", server);
		addClientAssertion(env, claims);

		cond.evaluate(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingAudience() {

		claims.remove("aud");

		env.putObject("client", client);
		env.putObject("server", server);
		addClientAssertion(env, claims);

		cond.evaluate(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidAudience() {

		claims.remove("aud");
		claims.addProperty("aud", "invalid");

		env.putObject("client", client);
		env.putObject("server", server);
		addClientAssertion(env, claims);

		cond.evaluate(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingExp() {

		claims.remove("exp");

		env.putObject("client", client);
		env.putObject("server", server);
		addClientAssertion(env, claims);

		cond.evaluate(env);

	}

	@Test
	public void testEvaluate_validNbf() {

		claims.remove("nbf");
		claims.addProperty("nbf", nowSeconds + (5 * 60)); // 5 minutes in the past is not ok

		env.putObject("client", client);
		env.putObject("server", server);
		addClientAssertion(env, claims);

		cond.evaluate(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidNbf() {

		claims.remove("nbf");
		claims.addProperty("nbf", nowSeconds + (6 * 60)); // 6 minutes in the past is not ok

		env.putObject("client", client);
		env.putObject("server", server);
		addClientAssertion(env, claims);

		cond.evaluate(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidExp() {

		claims.remove("exp");
		claims.addProperty("exp", nowSeconds - (5 * 60)); // 5 minutes in the past is not ok

		env.putObject("client", client);
		env.putObject("server", server);
		addClientAssertion(env, claims);

		cond.evaluate(env);

	}

	@Test
	public void testEvaluate_allowableExpSkew() {

		claims.remove("exp");
		claims.addProperty("exp", nowSeconds - (4 * 60)); // 4 minutes out should be fine still

		env.putObject("client", client);
		env.putObject("server", server);
		addClientAssertion(env, claims);

		cond.evaluate(env);

	}

	@Test
	public void testEvaluate_validExpFuture() {

		claims.remove("exp");
		claims.addProperty("exp", nowSeconds + (60 * 60 * 23)); // exp claim time range has not expired (23 hours)

		env.putObject("client", client);
		env.putObject("server", server);
		addClientAssertion(env, claims);

		cond.evaluate(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidExpFuture() {

		claims.remove("exp");
		claims.addProperty("exp", nowSeconds + (60 * 60 * 25)); // exp claim time range has expired (25 hours)

		env.putObject("client", client);
		env.putObject("server", server);
		addClientAssertion(env, claims);

		cond.evaluate(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingIat() {

		claims.remove("iat");

		env.putObject("client", client);
		env.putObject("server", server);
		addClientAssertion(env, claims);

		cond.evaluate(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidIat() {

		claims.remove("iat");
		claims.addProperty("iat", nowSeconds + (60 * 60 * 25)); //iat claim time too far in future (25 hours)

		env.putObject("client", client);
		env.putObject("server", server);
		addClientAssertion(env, claims);

		cond.evaluate(env);

	}

	@Test
	public void testEvaluate_allowableIatSkew() {

		claims.remove("iat");
		claims.addProperty("iat", nowSeconds + (60 * 60 * 23)); // iat claim time is within 24hour allowance (23 hours)

		env.putObject("client", client);
		env.putObject("server", server);
		addClientAssertion(env, claims);

		cond.evaluate(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidIatFuture() {

		claims.remove("iat");
		claims.addProperty("iat", nowSeconds + (60 * 60 * 25)); // exp claim time range has expired (25 hours)

		env.putObject("client", client);
		env.putObject("server", server);
		addClientAssertion(env, claims);

		cond.evaluate(env);

	}
}
