package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ChangeAuthzenSearchRequestPageLimit_UnitTest {

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

	@Test
	public void changePageLimit_setsTheNewLimitAndKeepsTheToken() {
		env.putObject("authzen_api_endpoint_request", json("""
			{ "subject": { "type": "user" }, "page": { "limit": 1, "token": "abc" } }"""));
		run(new ChangeAuthzenSearchRequestPageLimit(2));
		JsonObject page = env.getObject("authzen_api_endpoint_request").getAsJsonObject("page");
		assertEquals(2, OIDFJSON.getInt(page.get("limit")));
		assertEquals("abc", OIDFJSON.getString(page.get("token")));
	}

	@Test
	public void changePageLimit_toTheCurrentLimit_fails() {
		// The request would be sent unchanged, so a PDP returning the next page would be warned for nothing.
		env.putObject("authzen_api_endpoint_request", json("""
			{ "subject": { "type": "user" }, "page": { "limit": 2, "token": "abc" } }"""));
		assertThrows(ConditionError.class, () -> run(new ChangeAuthzenSearchRequestPageLimit(2)));
	}

	@Test
	public void changePageLimit_withoutPage_fails() {
		env.putObject("authzen_api_endpoint_request", json("""
			{ "subject": { "type": "user" } }"""));
		assertThrows(ConditionError.class, () -> run(new ChangeAuthzenSearchRequestPageLimit(2)));
	}
}
