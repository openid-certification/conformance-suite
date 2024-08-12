package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ValidateClientAssertionClaims_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private long nowSeconds;

	private JsonObject client;

	private String clientId;

	private String audience;
	private String audienceMtls;

	private JsonObject claims;

	private JsonObject server;
	private JsonObject serverMtls;

	private ValidateClientAssertionClaims cond;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new ValidateClientAssertionClaims();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO, new String[0]);

		Date now = new Date();
		nowSeconds = now.getTime() / 1000;
		long issuedAt = nowSeconds - 10; // pretend this came from a distant server

		clientId = "test-client-id-346334adgdsfgdfg3425";

		audience = "https://localhost:8443/test/a/fintech-clienttest/token";
		audienceMtls = "https://localhost:8443/test-mtls/a/fintech-clienttest/token";

		client = JsonParser.parseString("{ \"client_id\": \"" + clientId + "\" }").getAsJsonObject();

		server = JsonParser.parseString("{"
			+ "\"issuer\":\"" + audience + "\","
			+ "\"token_endpoint\":\"" + audience + "\""
			+ "}").getAsJsonObject();

		serverMtls = JsonParser.parseString("{"
			+ "\"issuer\":\"" + audience + "\","
			+ "\"token_endpoint\":\"" + audience + "\","
			+ "\"mtls_endpoint_aliases\": {"
			+ "  \"token_endpoint\": \"" + audienceMtls + "\""
			+ "}"
			+ "}").getAsJsonObject();

		claims = JsonParser.parseString("{"
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

		cond.execute(env);

		verify(env, atLeastOnce()).getString("client", "client_id");
		verify(env, atLeastOnce()).getString("server", "issuer");
		verify(env, atLeastOnce()).getString("server", "token_endpoint");
		verify(env, atLeastOnce()).getElementFromObject("client_assertion", "claims.sub");
		verify(env, atLeastOnce()).getElementFromObject("client_assertion", "claims.iss");
		verify(env, atLeastOnce()).getElementFromObject("client_assertion", "claims.aud");
		verify(env, atLeastOnce()).getElementFromObject("client_assertion", "claims.jti");
		verify(env, atLeastOnce()).getLong("client_assertion", "claims.exp");
		verify(env, atLeastOnce()).getLong("client_assertion", "claims.iat");

	}

	@Test
	public void testEvaluate_noErrorMtls() {

		env.putObject("client", client);
		env.putObject("server", serverMtls);
		claims.addProperty("aud", audience);
		addClientAssertion(env, claims);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_noErrorMtlsAud() {

		env.putObject("client", client);
		env.putObject("server", serverMtls);
		claims.addProperty("aud", audienceMtls);
		addClientAssertion(env, claims);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_errorMtls() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("client", client);
			env.putObject("server", serverMtls);
			claims.addProperty("aud", "invalid");
			addClientAssertion(env, claims);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missingClientId() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("server", server);
			env.putObject("client", new JsonObject());
			addClientAssertion(env, claims);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_missingServerConfig() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("client", client);
			addClientAssertion(env, claims);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_missingIdToken() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("client", client);
			env.putObject("server", server);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_missingIssuer() {
		assertThrows(ConditionError.class, () -> {

			claims.remove("iss");

			env.putObject("client", client);
			env.putObject("server", server);
			addClientAssertion(env, claims);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_invalidIssuer() {
		assertThrows(ConditionError.class, () -> {

			claims.remove("iss");
			claims.addProperty("iss", "invalid");

			env.putObject("client", client);
			env.putObject("server", server);
			addClientAssertion(env, claims);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_missingAudience() {
		assertThrows(ConditionError.class, () -> {

			claims.remove("aud");

			env.putObject("client", client);
			env.putObject("server", server);
			addClientAssertion(env, claims);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_invalidAudience() {
		assertThrows(ConditionError.class, () -> {

			claims.remove("aud");
			claims.addProperty("aud", "invalid");

			env.putObject("client", client);
			env.putObject("server", server);
			addClientAssertion(env, claims);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_missingExp() {
		assertThrows(ConditionError.class, () -> {

			claims.remove("exp");

			env.putObject("client", client);
			env.putObject("server", server);
			addClientAssertion(env, claims);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_validNbf() {

		claims.remove("nbf");
		claims.addProperty("nbf", nowSeconds + (5 * 60)); // 5 minutes in the past is not ok

		env.putObject("client", client);
		env.putObject("server", server);
		addClientAssertion(env, claims);

		cond.execute(env);

	}

	@Test
	public void testEvaluate_invalidNbf() {
		assertThrows(ConditionError.class, () -> {

			claims.remove("nbf");
			claims.addProperty("nbf", nowSeconds + (6 * 60)); // 6 minutes in the past is not ok

			env.putObject("client", client);
			env.putObject("server", server);
			addClientAssertion(env, claims);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_invalidExp() {
		assertThrows(ConditionError.class, () -> {

			claims.remove("exp");
			claims.addProperty("exp", nowSeconds - (5 * 60)); // 5 minutes in the past is not ok

			env.putObject("client", client);
			env.putObject("server", server);
			addClientAssertion(env, claims);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_allowableExpSkew() {

		claims.remove("exp");
		claims.addProperty("exp", nowSeconds - (4 * 60)); // 4 minutes out should be fine still

		env.putObject("client", client);
		env.putObject("server", server);
		addClientAssertion(env, claims);

		cond.execute(env);

	}

	@Test
	public void testEvaluate_validExpFuture() {

		claims.remove("exp");
		claims.addProperty("exp", nowSeconds + (60 * 60 * 23)); // exp claim time range has not expired (23 hours)

		env.putObject("client", client);
		env.putObject("server", server);
		addClientAssertion(env, claims);

		cond.execute(env);

	}

	@Test
	public void testEvaluate_invalidExpFuture() {
		assertThrows(ConditionError.class, () -> {

			claims.remove("exp");
			claims.addProperty("exp", nowSeconds + (60 * 60 * 25)); // exp claim time range has expired (25 hours)

			env.putObject("client", client);
			env.putObject("server", server);
			addClientAssertion(env, claims);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_missingIat() {
		assertThrows(ConditionError.class, () -> {

			claims.remove("iat");

			env.putObject("client", client);
			env.putObject("server", server);
			addClientAssertion(env, claims);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_invalidIat() {
		assertThrows(ConditionError.class, () -> {

			claims.remove("iat");
			claims.addProperty("iat", nowSeconds + (60 * 60 * 25)); //iat claim time too far in future (25 hours)

			env.putObject("client", client);
			env.putObject("server", server);
			addClientAssertion(env, claims);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_allowableIatSkew() {

		claims.remove("iat");
		claims.addProperty("iat", nowSeconds + (60 * 60 * 23)); // iat claim time is within 24hour allowance (23 hours)

		env.putObject("client", client);
		env.putObject("server", server);
		addClientAssertion(env, claims);

		cond.execute(env);

	}

	@Test
	public void testEvaluate_invalidIatFuture() {
		assertThrows(ConditionError.class, () -> {

			claims.remove("iat");
			claims.addProperty("iat", nowSeconds + (60 * 60 * 25)); // exp claim time range has expired (25 hours)

			env.putObject("client", client);
			env.putObject("server", server);
			addClientAssertion(env, claims);

			cond.execute(env);

		});

	}
}
