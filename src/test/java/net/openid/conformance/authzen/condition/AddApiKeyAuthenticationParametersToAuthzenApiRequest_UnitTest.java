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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddApiKeyAuthenticationParametersToAuthzenApiRequest_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private final Environment env = new Environment();

	private AddApiKeyAuthenticationParametersToAuthzenApiRequest cond;

	@BeforeEach
	public void setUp() {
		cond = new AddApiKeyAuthenticationParametersToAuthzenApiRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void apiKeyFromClient_isSentAsBearer() {
		env.putObject("client", JsonParser.parseString("{ \"api_key\": \"s3cret\" }").getAsJsonObject());

		cond.execute(env);

		assertEquals("Bearer s3cret", env.getString("authzen_api_endpoint_request_headers", "Authorization"));
	}

	@Test
	public void apiKeyMissing_failsNamingTheClientSection() {
		env.putObject("client", JsonParser.parseString("{}").getAsJsonObject());

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("'Client' section"));
	}

	@Test
	public void apiKeyNull_fails() {
		env.putObject("client", JsonParser.parseString("{ \"api_key\": null }").getAsJsonObject());

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertNull(env.getObject("authzen_api_endpoint_request_headers"));
	}

	@Test
	public void apiKeyEmpty_failsWithoutAddingAHeader() {
		env.putObject("client", JsonParser.parseString("{ \"api_key\": \"\" }").getAsJsonObject());

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("is empty"));
		assertNull(env.getObject("authzen_api_endpoint_request_headers"));
	}

	@Test
	public void apiKeyWhitespaceOnly_fails() {
		env.putObject("client", JsonParser.parseString("{ \"api_key\": \" \\t \" }").getAsJsonObject());

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("is empty"));
	}
}
