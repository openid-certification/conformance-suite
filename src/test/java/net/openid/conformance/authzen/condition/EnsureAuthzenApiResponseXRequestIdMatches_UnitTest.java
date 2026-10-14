package net.openid.conformance.authzen.condition;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class EnsureAuthzenApiResponseXRequestIdMatches_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureAuthzenApiResponseXRequestIdMatches cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureAuthzenApiResponseXRequestIdMatches();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env.putString("authzen_api_endpoint_request_x_request_id", "bfe9eb29-ab87-4ca3-be83-a1d5d8305716");
	}

	private void putResponse(String json) {
		env.putObject("authzen_api_endpoint_response", JsonParser.parseString(json).getAsJsonObject());
	}

	@Test
	public void echoed_succeeds() {
		putResponse("""
			{ "status": 200, "headers": { "x-request-id": [ "bfe9eb29-ab87-4ca3-be83-a1d5d8305716" ] } }""");
		cond.execute(env);
	}

	@Test
	public void echoed_headerNameIsCaseInsensitive() {
		putResponse("""
			{ "status": 400, "headers": { "X-Request-Id": "bfe9eb29-ab87-4ca3-be83-a1d5d8305716" } }""");
		cond.execute(env);
	}

	@Test
	public void missing_fails() {
		putResponse("""
			{ "status": 200, "headers": { "content-type": "application/json" } }""");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("did not echo"), e.getMessage());
	}

	@Test
	public void differentValue_fails() {
		putResponse("""
			{ "status": 200, "headers": { "x-request-id": "some-other-id" } }""");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("same X-Request-ID"), e.getMessage());
	}
}
