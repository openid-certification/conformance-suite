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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarnPDPAcceptsUnauthenticatedRequests_UnitTest {

	private final Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private WarnPDPAcceptsUnauthenticatedRequests cond;

	@BeforeEach
	public void setUp() {
		cond = new WarnPDPAcceptsUnauthenticatedRequests();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void respond(int status) {
		env.putObject("authzen_api_endpoint_response", JsonParser.parseString("{ \"status\": " + status + ", \"headers\": {} }").getAsJsonObject());
	}

	@Test
	public void answered_isFlagged() {
		respond(200);
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("answered a request that carried no credentials"), e.getMessage());
	}

	@ParameterizedTest
	@ValueSource(ints = {401, 403})
	public void rejected_passes(int status) {
		respond(status);
		assertDoesNotThrow(() -> cond.execute(env));
	}
}
