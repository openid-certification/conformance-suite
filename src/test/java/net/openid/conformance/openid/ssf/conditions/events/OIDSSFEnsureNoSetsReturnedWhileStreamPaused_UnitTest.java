package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class OIDSSFEnsureNoSetsReturnedWhileStreamPaused_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFEnsureNoSetsReturnedWhileStreamPaused condition;

	@BeforeEach
	public void setUp() {
		condition = new OIDSSFEnsureNoSetsReturnedWhileStreamPaused();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void preparePollingResponse(String bodyJson) {
		JsonObject response = new JsonObject();
		response.addProperty("status", 200);
		if (bodyJson != null) {
			response.add("body_json", JsonParser.parseString(bodyJson));
		}
		env.putObject("ssf_polling_response", response);
	}

	@Test
	public void shouldPassWhenSetsIsEmpty() {
		preparePollingResponse("{\"sets\":{}}");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	public void shouldPassWhenSetsIsMissing() {
		preparePollingResponse("{}");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	public void shouldFailWhenSetsContainsAnEvent() {
		preparePollingResponse("{\"sets\":{\"jti-1\":\"eyJhbGciOiJub25lIn0.e30.\"}}");
		ConditionError error = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(error.getMessage().contains("paused stream"));
	}

	@Test
	public void shouldFailWhenBodyIsNotJson() {
		preparePollingResponse(null);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}
}
