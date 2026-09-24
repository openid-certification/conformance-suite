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
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetAuthzenClientConfiguration_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private final Environment env = new Environment();

	private GetAuthzenClientConfiguration cond;

	@BeforeEach
	public void setUp() {
		cond = new GetAuthzenClientConfiguration();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putConfig(String json) {
		env.putObject("config", JsonParser.parseString(json).getAsJsonObject());
	}

	@Test
	public void apiKeyOnlyClient_isLoadedWithoutClientId() {
		putConfig("{ \"client\": { \"api_key\": \"s3cret\" } }");

		cond.execute(env);

		assertEquals("s3cret", env.getString("client", "api_key"));
	}

	@Test
	public void clientSecretClient_isLoaded() {
		putConfig("{ \"client\": { \"client_id\": \"pep\", \"client_secret\": \"s3cret\" } }");

		cond.execute(env);

		assertEquals("pep", env.getString("client", "client_id"));
		assertEquals("s3cret", env.getString("client", "client_secret"));
	}

	@Test
	public void missingClientSection_fails() {
		putConfig("{ \"pdp\": { \"api_key\": \"s3cret\" } }");

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("'Client' section"));
	}

	@Test
	public void nonObjectClientSection_fails() {
		putConfig("{ \"client\": \"s3cret\" }");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
