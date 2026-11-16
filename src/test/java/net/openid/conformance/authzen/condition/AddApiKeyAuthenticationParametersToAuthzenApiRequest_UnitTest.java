package net.openid.conformance.authzen.condition;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("'PDP API Key' field is missing"), e.getMessage());
		assertNull(env.getObject("authzen_api_endpoint_request_headers"));
	}

	@Test
	public void apiKeyNotAString_failsNamingTheField() {
		env.putObject("client", JsonParser.parseString("{ \"api_key\": 12345 }").getAsJsonObject());

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("'PDP API Key' field in the 'Client' section of the test configuration is not a string"), e.getMessage());
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

	@Test
	public void headerNameEmpty_sendsBearer() {
		env.putObject("client", JsonParser.parseString("{ \"api_key\": \"s3cret\", \"api_key_header\": \"\" }").getAsJsonObject());

		cond.execute(env);

		assertEquals("Bearer s3cret", env.getString("authzen_api_endpoint_request_headers", "Authorization"));
	}

	@Test
	public void headerNameNull_sendsBearer() {
		env.putObject("client", JsonParser.parseString("{ \"api_key\": \"s3cret\", \"api_key_header\": null }").getAsJsonObject());

		cond.execute(env);

		assertEquals("Bearer s3cret", env.getString("authzen_api_endpoint_request_headers", "Authorization"));
	}

	@Test
	public void headerNameAuthorizationInAnyCase_sendsBearer() {
		env.putObject("client", JsonParser.parseString("{ \"api_key\": \"s3cret\", \"api_key_header\": \"authorization\" }").getAsJsonObject());

		cond.execute(env);

		assertEquals("Bearer s3cret", env.getString("authzen_api_endpoint_request_headers", "Authorization"));
	}

	@Test
	public void otherHeaderName_carriesTheKeyAsItsValue() {
		env.putObject("client", JsonParser.parseString("{ \"api_key\": \"s3cret\", \"api_key_header\": \" X-API-Key \" }").getAsJsonObject());

		cond.execute(env);

		assertEquals("s3cret", env.getString("authzen_api_endpoint_request_headers", "X-API-Key"));
		assertNull(env.getString("authzen_api_endpoint_request_headers", "Authorization"));
	}

	@Test
	public void headerNameNotAToken_failsNamingTheField() {
		env.putObject("client", JsonParser.parseString("{ \"api_key\": \"s3cret\", \"api_key_header\": \"X API Key:\" }").getAsJsonObject());

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("'PDP API Key Header' field in the 'Client' section of the test configuration is not an HTTP header name"), e.getMessage());
		assertNull(env.getObject("authzen_api_endpoint_request_headers"));
	}

	@Test
	public void headerNameNotAString_failsNamingTheField() {
		env.putObject("client", JsonParser.parseString("{ \"api_key\": \"s3cret\", \"api_key_header\": 1 }").getAsJsonObject());

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("'PDP API Key Header' field in the 'Client' section of the test configuration is not a string"), e.getMessage());
	}

	@ParameterizedTest
	@ValueSource(strings = {"Content-Type", "accept", "X-Request-ID", "HOST", "Content-Length", "Transfer-Encoding", "Accept-Charset"})
	public void headerNameTheTestSendsItself_failsNamingTheField(String header) {
		env.putObject("client", JsonParser.parseString("{ \"api_key\": \"s3cret\", \"api_key_header\": \"" + header + "\" }").getAsJsonObject());

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("names a header the test sends itself or that the HTTP client sets (" + header + ")"), e.getMessage());
		assertNull(env.getObject("authzen_api_endpoint_request_headers"));
	}
}
