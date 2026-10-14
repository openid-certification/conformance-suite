package net.openid.conformance.authzen.condition;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class WarnMethodRejectedWithNotFound_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private void run(AbstractCondition cond, String response) {
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env.putObject("authzen_api_endpoint_response", JsonParser.parseString(response).getAsJsonObject());
		cond.execute(env);
	}

	@Test
	public void notFound_warns() {
		assertThrows(ConditionError.class, () -> run(new WarnMethodRejectedWithNotFound(), """
			{ "status": 404, "headers": {} }"""));
	}

	@Test
	public void methodNotAllowed_isNotFlaggedAsNotFound() {
		run(new WarnMethodRejectedWithNotFound(), """
			{ "status": 405, "headers": { "allow": "POST" } }""");
	}
}
