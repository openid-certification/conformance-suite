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

class WarnChangedPageRequestAccepted_UnitTest {

	private final Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private WarnChangedPageRequestAccepted cond;

	@BeforeEach
	public void setUp() {
		cond = new WarnChangedPageRequestAccepted("page.limit");
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void respond(int status) {
		env.putObject("authzen_api_endpoint_response", JsonParser.parseString("{ \"status\": " + status + ", \"headers\": {} }").getAsJsonObject());
	}

	@Test
	public void pageReturned_isFlagged() {
		respond(200);
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("returned a page for a request whose page.limit differs"), e.getMessage());
	}

	@Test
	public void pageReturnedForChangedEntity_namesTheEntity() {
		respond(200);
		cond = new WarnChangedPageRequestAccepted("action");
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("returned a page for a request whose action differs"), e.getMessage());
	}

	@ParameterizedTest
	@ValueSource(ints = {400, 409, 422, 500, 302})
	public void otherStatus_isNotFlagged(int status) {
		// Whether a status other than 200 is acceptable is EnsureHttpStatusCodeMatchesExpected's call.
		respond(status);
		cond.execute(env);
	}

	@Test
	public void missingStatus_isNotFlagged() {
		env.putObject("authzen_api_endpoint_response", JsonParser.parseString("{ \"headers\": {} }").getAsJsonObject());
		cond.execute(env);
	}
}
