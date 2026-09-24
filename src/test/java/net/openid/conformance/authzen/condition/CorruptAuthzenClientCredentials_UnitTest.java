package net.openid.conformance.authzen.condition;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CorruptAuthzenClientCredentials_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private final Environment env = new Environment();

	private CorruptAuthzenClientCredentials cond;

	@BeforeEach
	public void setUp() {
		cond = new CorruptAuthzenClientCredentials();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putClient(String json) {
		env.putObject("client", JsonParser.parseString(json).getAsJsonObject());
	}

	@Test
	public void apiKey_isCorrupted() {
		putClient("{ \"api_key\": \"s3cret\" }");

		cond.execute(env);

		assertEquals(CorruptAuthzenClientCredentials.INVALID_CREDENTIAL_VALUE, env.getString("client", "api_key"));
	}

	@Test
	public void clientSecret_isCorruptedAndClientIdKept() {
		putClient("{ \"client_id\": \"pep\", \"client_secret\": \"s3cret\" }");

		cond.execute(env);

		assertEquals(CorruptAuthzenClientCredentials.INVALID_CREDENTIAL_VALUE, env.getString("client", "client_secret"));
		assertEquals("pep", env.getString("client", "client_id"));
	}

	@Test
	public void noCredential_fails() {
		putClient("{ \"client_id\": \"pep\" }");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
