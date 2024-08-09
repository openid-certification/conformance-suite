package net.openid.conformance.condition.common;

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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class EnsureIncomingTls12WithSecureCipherOrTls13_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureIncomingTls12WithSecureCipherOrTls13 cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new EnsureIncomingTls12WithSecureCipherOrTls13();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noErrorTls12() {
		JsonObject hasTls12 = JsonParser.parseString("{\"headers\": "
			+ "{\"x-ssl-protocol\": \"TLSv1.2\", \"x-ssl-cipher\": \"ECDHE-RSA-AES128-GCM-SHA256\"}"
			+ "}").getAsJsonObject();

		env.putObject("client_request", hasTls12);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("client_request", "headers.x-ssl-protocol");
	}

	@Test
	public void testEvaluate_noErrorTls13() {
		JsonObject hasTls12 = JsonParser.parseString("{\"headers\": "
			+ "{\"x-ssl-protocol\": \"TLSv1.3\", \"x-ssl-cipher\": \"flibble\"}"
			+ "}").getAsJsonObject();

		env.putObject("client_request", hasTls12);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("client_request", "headers.x-ssl-protocol");
	}

	@Test
	public void testEvaluate_wrong_tls11() {
		assertThrows(ConditionError.class, () -> {
			JsonObject wrongTls = JsonParser.parseString("{\"headers\": "
				+ "{\"x-ssl-protocol\": \"TLSv1.1\", \"x-ssl-cipher\": \"ECDHE-RSA-AES128-GCM-SHA256\"}"
				+ "}").getAsJsonObject();

			env.putObject("client_request", wrongTls);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missingProtocol() {
		assertThrows(ConditionError.class, () -> {
			JsonObject missingTls = JsonParser.parseString("{\"headers\": "
				+ "{\"x-ssl-cipher\": \"ECDHE-RSA-AES128-GCM-SHA256\"}"
				+ "}").getAsJsonObject();

			env.putObject("client_request", missingTls);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missingCipher() {
		assertThrows(ConditionError.class, () -> {
			JsonObject onlyTls = JsonParser.parseString("{\"headers\": "
				+ "{\"x-ssl-protocol\": \"TLSv1.2\"}"
				+ "}").getAsJsonObject();

			env.putObject("client_request", onlyTls);

			cond.execute(env);

			verify(env, atLeastOnce()).getString("client_request", "headers.x-ssl-protocol");

		});

	}
}
