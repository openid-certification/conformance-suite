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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarnUnsupportedMethodAccepted_UnitTest {

	private final Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private WarnUnsupportedMethodAccepted cond;

	@BeforeEach
	public void setUp() {
		cond = new WarnUnsupportedMethodAccepted();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env.putString("authzen_api_endpoint_request_method", "GET");
	}

	private void respond(int status) {
		env.putObject("authzen_api_endpoint_response", JsonParser.parseString("{ \"status\": " + status + ", \"headers\": {} }").getAsJsonObject());
	}

	@ParameterizedTest
	@ValueSource(ints = {200, 201, 204, 299})
	public void successStatus_isFlagged(int status) {
		respond(status);
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("accepted a GET request"), e.getMessage());
	}

	@ParameterizedTest
	@ValueSource(ints = {400, 404, 405, 415, 500, 302})
	public void otherStatus_isNotFlagged(int status) {
		// Whether a non-success status is acceptable is EnsureHttpStatusCodeMatchesExpected's call.
		respond(status);
		cond.execute(env);
	}

	@Test
	public void missingStatus_isNotFlagged() {
		env.putObject("authzen_api_endpoint_response", JsonParser.parseString("{ \"headers\": {} }").getAsJsonObject());
		cond.execute(env);
	}
}
