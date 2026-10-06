package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
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
class ExtractAuthzenApiEndpointSearchResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ExtractAuthzenApiEndpointSearchResponse cond;

	@BeforeEach
	public void setUp() {
		cond = new ExtractAuthzenApiEndpointSearchResponse();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putBody(String json) {
		JsonObject response = new JsonObject();
		response.add("body_json", JsonParser.parseString(json));
		env.putObject("authzen_api_endpoint_response", response);
	}

	@Test
	public void objectBody_isExtracted() {
		putBody("""
			{ "search": "anything" }""");
		cond.execute(env);
		assertTrue(env.containsObject("authzen_search_endpoint_response"));
	}

	@Test
	public void arrayBody_throws() {
		putBody("""
			[ { "decision": true } ]""");
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("is not a JSON object"));
	}

	@Test
	public void missingBody_throws() {
		env.putObject("authzen_api_endpoint_response", new JsonObject());
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
