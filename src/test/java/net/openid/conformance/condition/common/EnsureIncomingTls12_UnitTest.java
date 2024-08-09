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
public class EnsureIncomingTls12_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureIncomingTls12 cond;

	private JsonObject hasTls;
	private JsonObject wrongTls;
	private JsonObject missingTls;
	private JsonObject onlyTls;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		cond = new EnsureIncomingTls12();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		hasTls = JsonParser.parseString("{\"headers\": "
			+ "{\"x-ssl-protocol\": \"TLSv1.2\", \"x-ssl-cipher\": \"ECDHE-RSA-AES128-GCM-SHA256\"}"
			+ "}").getAsJsonObject();
		wrongTls = JsonParser.parseString("{\"headers\": "
			+ "{\"x-ssl-protocol\": \"TLSv1.1\", \"x-ssl-cipher\": \"ECDHE-RSA-AES128-GCM-SHA256\"}"
			+ "}").getAsJsonObject();
		missingTls = JsonParser.parseString("{\"headers\": "
			+ "{\"x-ssl-cipher\": \"ECDHE-RSA-AES128-GCM-SHA256\"}"
			+ "}").getAsJsonObject();
		onlyTls = JsonParser.parseString("{\"headers\": "
			+ "{\"x-ssl-protocol\": \"TLSv1.2\"}"
			+ "}").getAsJsonObject();

	}

	/**
	 * Test method for {@link EnsureClientCertificateCNMatchesClientId#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		env.putObject("client_request", hasTls);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("client_request", "headers.x-ssl-protocol");

	}
	@Test
	public void testEvaluate_wrong() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("client_request", wrongTls);

			cond.execute(env);

		});

	}
	@Test
	public void testEvaluate_missing() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("client_request", missingTls);

			cond.execute(env);

		});

	}
	@Test
	public void testEvaluate_only() {

		env.putObject("client_request", onlyTls);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("client_request", "headers.x-ssl-protocol");

	}
}
