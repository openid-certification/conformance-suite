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
class EnsureMethodNotAllowedResponseAllowsPost_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private void run(AbstractCondition cond, String response) {
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env.putObject("authzen_api_endpoint_response", JsonParser.parseString(response).getAsJsonObject());
		cond.execute(env);
	}

	@Test
	public void methodNotAllowed_withAllowPost_succeeds() {
		run(new EnsureMethodNotAllowedResponseAllowsPost(), """
			{ "status": 405, "headers": { "Allow": [ "OPTIONS, POST" ] } }""");
	}

	@Test
	public void methodNotAllowed_withoutAllow_fails() {
		assertThrows(ConditionError.class, () -> run(new EnsureMethodNotAllowedResponseAllowsPost(), """
			{ "status": 405, "headers": { "content-type": "application/json" } }"""));
	}

	@Test
	public void methodNotAllowed_withAllowWithoutPost_fails() {
		assertThrows(ConditionError.class, () -> run(new EnsureMethodNotAllowedResponseAllowsPost(), """
			{ "status": 405, "headers": { "allow": "GET, HEAD" } }"""));
	}

	@Test
	public void methodNotAllowed_withLowerCasePost_fails() {
		assertThrows(ConditionError.class, () -> run(new EnsureMethodNotAllowedResponseAllowsPost(), """
			{ "status": 405, "headers": { "allow": "get, post" } }"""));
	}

	@Test
	public void badRequest_needsNoAllowHeader() {
		run(new EnsureMethodNotAllowedResponseAllowsPost(), """
			{ "status": 400, "headers": {} }""");
	}
}
