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

class WarnAuthzenRequestRejected_UnitTest {

	private final Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private WarnAuthzenRequestRejected cond;

	@BeforeEach
	public void setUp() {
		cond = new WarnAuthzenRequestRejected("The PDP is expected to ignore the member");
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
	}

	private void respond(int status) {
		env.putObject("authzen_api_endpoint_response", JsonParser.parseString("{ \"status\": " + status + ", \"headers\": {} }").getAsJsonObject());
	}

	@Test
	public void accepted_succeeds() {
		respond(200);
		cond.execute(env);
	}

	@ParameterizedTest
	@ValueSource(ints = {400, 415, 422})
	public void rejected_isFlaggedWithTheExpectation(int status) {
		respond(status);
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("rejected the request with HTTP " + status), e.getMessage());
		assertTrue(e.getMessage().contains("The PDP is expected to ignore the member"), e.getMessage());
	}
}
