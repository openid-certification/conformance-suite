package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
class ChangeAuthzenSearchRequestEntity_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private static JsonObject json(String s) {
		return JsonParser.parseString(s).getAsJsonObject();
	}

	private void run(String entity, String replacement) {
		ChangeAuthzenSearchRequestEntity cond = new ChangeAuthzenSearchRequestEntity(entity, json(replacement));
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		cond.execute(env);
	}

	@Test
	public void replacesTheEntityAndKeepsTheRest() {
		env.putObject("authzen_api_endpoint_request", json("""
			{ "subject": { "type": "user" }, "action": { "name": "read" }, "resource": { "type": "record", "id": "record-1" },
			  "page": { "limit": 1, "token": "abc" } }"""));
		run("action", "{ \"name\": \"write\" }");
		JsonObject request = env.getObject("authzen_api_endpoint_request");
		assertEquals("write", OIDFJSON.getString(request.getAsJsonObject("action").get("name")));
		assertEquals("abc", OIDFJSON.getString(request.getAsJsonObject("page").get("token")));
		assertEquals("record-1", OIDFJSON.getString(request.getAsJsonObject("resource").get("id")));
	}

	@Test
	public void missingEntity_fails() {
		env.putObject("authzen_api_endpoint_request", json("""
			{ "subject": { "type": "user", "id": "alice" }, "resource": { "type": "record", "id": "record-1" } }"""));
		assertThrows(ConditionError.class, () -> run("action", "{ \"name\": \"write\" }"));
	}

	@Test
	public void unchangedReplacement_fails() {
		// A replacement equal to the entity would send an unchanged request and test nothing.
		env.putObject("authzen_api_endpoint_request", json("""
			{ "subject": { "type": "user", "id": "alice" }, "resource": { "type": "record", "id": "record-1" } }"""));
		assertThrows(ConditionError.class, () -> run("resource", "{ \"id\": \"record-1\", \"type\": \"record\" }"));
	}
}
