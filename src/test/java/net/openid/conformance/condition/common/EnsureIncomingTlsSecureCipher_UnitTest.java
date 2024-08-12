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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class EnsureIncomingTlsSecureCipher_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureIncomingTlsSecureCipher cond;

	private List<JsonObject> hasTls;
	private JsonObject wrongTls;
	private JsonObject missingTls;
	private JsonObject onlyTls;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		cond = new EnsureIncomingTlsSecureCipher();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		hasTls = new ArrayList<>();

		hasTls.add(JsonParser.parseString("{\"headers\": "
			+ "{\"x-ssl-protocol\": \"TLSv1.2\", \"x-ssl-cipher\": \"DHE-RSA-AES128-GCM-SHA256\"}"
			+ "}").getAsJsonObject());
		hasTls.add(JsonParser.parseString("{\"headers\": "
			+ "{\"x-ssl-protocol\": \"TLSv1.2\", \"x-ssl-cipher\": \"ECDHE-RSA-AES128-GCM-SHA256\"}"
			+ "}").getAsJsonObject());
		hasTls.add(JsonParser.parseString("{\"headers\": "
			+ "{\"x-ssl-protocol\": \"TLSv1.2\", \"x-ssl-cipher\": \"DHE-RSA-AES256-GCM-SHA384\"}"
			+ "}").getAsJsonObject());
		hasTls.add(JsonParser.parseString("{\"headers\": "
			+ "{\"x-ssl-protocol\": \"TLSv1.2\", \"x-ssl-cipher\": \"ECDHE-RSA-AES256-GCM-SHA384\"}"
			+ "}").getAsJsonObject());
		wrongTls = JsonParser.parseString("{\"headers\": "
			+ "{\"x-ssl-protocol\": \"TLSv1.2\", \"x-ssl-cipher\": \"DUCK-TAPE-AND-A-PRAYER\"}"
			+ "}").getAsJsonObject();
		onlyTls = JsonParser.parseString("{\"headers\": "
			+ "{\"x-ssl-cipher\": \"ECDHE-RSA-AES128-GCM-SHA256\"}"
			+ "}").getAsJsonObject();
		missingTls = JsonParser.parseString("{\"headers\": "
			+ "{\"x-ssl-protocol\": \"TLSv1.2\"}"
			+ "}").getAsJsonObject();

	}

	/**
	 * Test method for {@link EnsureClientCertificateCNMatchesClientId#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		for (JsonObject tls : hasTls) {
			env.putObject("client_request", tls);

			cond.execute(env);

			verify(env, atLeastOnce()).getString("client_request", "headers.x-ssl-cipher");
		}

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

		verify(env, atLeastOnce()).getString("client_request", "headers.x-ssl-cipher");

	}
}
