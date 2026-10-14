package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
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
class EnsureAuthzenApiEndpointPathContainsV1_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private static JsonObject json(String s) {
		return JsonParser.parseString(s).getAsJsonObject();
	}

	private void run(AbstractCondition cond) {
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		cond.execute(env);
	}

	private void fails(AbstractCondition cond) {
		assertThrows(ConditionError.class, () -> run(cond));
	}

	private void check(String endpoint) {
		env.putString("authzen_api_endpoint", endpoint);
		run(new EnsureAuthzenApiEndpointPathContainsV1());
	}

	@Test
	public void defaultPath_succeeds() {
		check("https://pdp.example.com/access/v1/evaluation");
	}

	@Test
	public void v1AtTheEnd_succeeds() {
		check("https://pdp.example.com/authz/v1");
	}

	@Test
	public void noVersionSegment_fails() {
		assertThrows(ConditionError.class, () -> check("https://pdp.example.com/access/evaluation"));
	}

	@Test
	public void v1OnlyInsideASegment_fails() {
		assertThrows(ConditionError.class, () -> check("https://pdp.example.com/access/v10/evaluation"));
	}

	@Test
	public void v1InTheHostOnly_fails() {
		assertThrows(ConditionError.class, () -> check("https://v1.pdp.example.com/access/evaluation"));
	}
}
