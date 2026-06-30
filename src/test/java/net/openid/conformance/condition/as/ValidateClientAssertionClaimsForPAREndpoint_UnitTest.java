package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidateClientAssertionClaimsForPAREndpoint_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private long nowSeconds;

	private JsonObject client;
	private String clientId;

	private String tokenEndpoint;
	private String issuer;
	private String parEndpoint;

	private JsonObject claims;
	private JsonObject server;

	private ValidateClientAssertionClaimsForPAREndpoint cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateClientAssertionClaimsForPAREndpoint();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO, new String[0]);

		Date now = new Date();
		nowSeconds = now.getTime() / 1000;
		long issuedAt = nowSeconds - 10;

		clientId = "test-client-id-346334adgdsfgdfg3425";
		tokenEndpoint = "https://as.example.com/token";
		issuer = "https://as.example.com/";
		parEndpoint = "https://as.example.com/par";

		client = JsonParser.parseString("{ \"client_id\": \"" + clientId + "\" }").getAsJsonObject();

		server = JsonParser.parseString("{"
			+ "\"issuer\":\"" + issuer + "\","
			+ "\"token_endpoint\":\"" + tokenEndpoint + "\","
			+ "\"pushed_authorization_request_endpoint\":\"" + parEndpoint + "\""
			+ "}").getAsJsonObject();

		claims = JsonParser.parseString("{"
			+ "\"iss\":\"" + clientId + "\","
			+ "\"sub\":\"" + clientId + "\","
			+ "\"aud\":\"" + issuer + "\","
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
	public void testEvaluate_issuerAud_noMtlsAlias() {
		// aud = issuer URL, no MTLS PAR alias — must not NPE
		env.putObject("client", client);
		env.putObject("server", server);
		addClientAssertion(env, claims);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_tokenEndpointAud_noMtlsAlias() {
		// aud = token endpoint URL, no MTLS PAR alias — must not NPE
		claims.addProperty("aud", tokenEndpoint);
		env.putObject("client", client);
		env.putObject("server", server);
		addClientAssertion(env, claims);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_parEndpointAud_noMtlsAlias() {
		// aud = PAR endpoint URL, no MTLS PAR alias — must not NPE
		claims.addProperty("aud", parEndpoint);
		env.putObject("client", client);
		env.putObject("server", server);
		addClientAssertion(env, claims);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_wrongAud_noMtlsAlias() {
		// aud = unrecognised value, no MTLS PAR alias — must throw ConditionError (not NPE)
		assertThrows(ConditionError.class, () -> {
			claims.addProperty("aud", "https://attacker.example.com/");
			env.putObject("client", client);
			env.putObject("server", server);
			addClientAssertion(env, claims);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_arrayAud_noMtlsAlias() {
		// array aud containing issuer, no MTLS PAR alias — must not NPE
		var aud = new JsonArray();
		aud.add(issuer);
		aud.add("https://other.example.com/");
		claims.add("aud", aud);
		env.putObject("client", client);
		env.putObject("server", server);
		addClientAssertion(env, claims);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_wrongArrayAud_noMtlsAlias() {
		// array aud with no valid value, no MTLS PAR alias — must throw ConditionError (not NPE)
		assertThrows(ConditionError.class, () -> {
			var aud = new JsonArray();
			aud.add("https://attacker.example.com/");
			claims.add("aud", aud);
			env.putObject("client", client);
			env.putObject("server", server);
			addClientAssertion(env, claims);

			cond.execute(env);
		});
	}
}
