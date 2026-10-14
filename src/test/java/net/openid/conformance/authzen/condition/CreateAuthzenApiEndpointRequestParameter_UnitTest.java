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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the required/optional filtering in CreateAuthzenApiEndpointRequestParameter through the
 * entity builders: required members must be present, optional ones are copied, and anything else
 * is dropped from the request the suite sends.
 */
@ExtendWith(MockitoExtension.class)
class CreateAuthzenApiEndpointRequestParameter_UnitTest {

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

	private JsonObject build(AbstractCondition cond) {
		env.putObject("authzen_api_endpoint_request", new JsonObject());
		run(cond);
		return env.getObject("authzen_api_endpoint_request");
	}

	@Test
	public void subject_keepsRequiredAndOptionalMembers_dropsOthers() {
		JsonObject request = build(new CreateAuthzenApiEndpointRequestSubject(json(
			"{ \"type\": \"user\", \"id\": \"alice\", \"properties\": { \"role\": \"admin\" }, \"extra\": 1 }")));
		JsonObject subject = request.getAsJsonObject("subject");
		assertEquals("alice", OIDFJSON.getString(subject.get("id")));
		assertTrue(subject.has("properties"));
		assertFalse(subject.has("extra"));
	}

	@Test
	public void subject_missingId_fails() {
		env.putObject("authzen_api_endpoint_request", new JsonObject());
		fails(new CreateAuthzenApiEndpointRequestSubject(json("{ \"type\": \"user\" }")));
	}

	@Test
	public void searchSubject_dropsTheId() {
		JsonObject request = build(new CreateAuthzenApiEndpointRequestSearchSubject(json(
			"{ \"type\": \"user\", \"id\": \"alice\" }")));
		JsonObject subject = request.getAsJsonObject("subject");
		assertEquals("user", OIDFJSON.getString(subject.get("type")));
		assertFalse(subject.has("id"));
	}

	@Test
	public void action_requiresName() {
		env.putObject("authzen_api_endpoint_request", new JsonObject());
		fails(new CreateAuthzenApiEndpointRequestAction(json("{ \"properties\": { \"soft\": true } }")));
	}

	@Test
	public void resource_keepsProperties() {
		JsonObject request = build(new CreateAuthzenApiEndpointRequestResource(json(
			"{ \"type\": \"record\", \"id\": \"record-2\", \"properties\": { \"status\": \"archived\" } }")));
		assertEquals("archived", OIDFJSON.getString(request.getAsJsonObject("resource").getAsJsonObject("properties").get("status")));
	}
}
