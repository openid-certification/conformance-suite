package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * SSF 1.0 8.1.3.1: an add-subject request names the stream and the subject and may say whether
 * the subject is verified; a malformed request is answered 400, not with an aborted test.
 */
@ExtendWith(MockitoExtension.class)
public class OIDSSFHandleStreamSubjectAdd_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFHandleStreamSubjectAdd condition;

	@BeforeEach
	void setUp() {
		condition = new OIDSSFHandleStreamSubjectAdd();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env.putObject("ssf", JsonParser.parseString("""
			{"streams": {"s1": {"stream_id": "s1"}}}
			""").getAsJsonObject());
	}

	private void putBody(String json) {
		JsonObject request = new JsonObject();
		request.add("body_json", JsonParser.parseString(json));
		env.putObject("incoming_request", request);
	}

	private JsonObject result() {
		return env.getElementFromObject("ssf", "stream_op_result").getAsJsonObject();
	}

	@Test
	void addsAVerifiedSubject() {
		putBody("""
			{"stream_id": "s1", "subject": {"format": "email", "email": "user@example.com"}, "verified": true}
			""");
		assertDoesNotThrow(() -> condition.execute(env));
		assertEquals(200, OIDFJSON.getInt(result().get("status_code")));
	}

	@Test
	void addsASubjectWithoutVerified() {
		putBody("""
			{"stream_id": "s1", "subject": {"format": "email", "email": "user@example.com"}}
			""");
		assertDoesNotThrow(() -> condition.execute(env));
		assertEquals(200, OIDFJSON.getInt(result().get("status_code")));
	}

	@Test
	void answersANonBooleanVerifiedWith400() {
		putBody("""
			{"stream_id": "s1", "subject": {"format": "email", "email": "user@example.com"}, "verified": "yes"}
			""");
		assertThrows(ConditionError.class, () -> condition.execute(env));
		assertEquals(400, OIDFJSON.getInt(result().get("status_code")));
	}

	@Test
	void answersABodyThatIsNotAnObjectWith400() {
		putBody("""
			["not", "an", "object"]
			""");
		assertThrows(ConditionError.class, () -> condition.execute(env));
		assertEquals(400, OIDFJSON.getInt(result().get("status_code")));
	}
}
