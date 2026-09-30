package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.openid.ssf.SsfConstants.StreamStatus;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class OIDSSFEnsureStreamStatusIs_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFEnsureStreamStatusIs createCondition(StreamStatus expected) {
		OIDSSFEnsureStreamStatusIs condition = new OIDSSFEnsureStreamStatusIs(expected);
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		return condition;
	}

	private void prepareResponse(String bodyJson) {
		JsonObject response = new JsonObject();
		response.addProperty("status", 200);
		if (bodyJson != null) {
			response.add("body_json", JsonParser.parseString(bodyJson));
		}
		env.putObject("resource_endpoint_response_full", response);
	}

	@Test
	public void shouldPassWhenStatusMatches() {
		prepareResponse("{\"stream_id\":\"stream_1\",\"status\":\"paused\",\"reason\":\"maintenance\"}");
		assertDoesNotThrow(() -> createCondition(StreamStatus.paused).execute(env));
	}

	@Test
	public void shouldFailWhenStatusDiffers() {
		prepareResponse("{\"stream_id\":\"stream_1\",\"status\":\"enabled\"}");
		ConditionError error = assertThrows(ConditionError.class, () -> createCondition(StreamStatus.paused).execute(env));
		assertTrue(error.getMessage().contains("not 'paused'"));
	}

	@Test
	public void shouldFailWhenStatusIsMissing() {
		prepareResponse("{\"stream_id\":\"stream_1\"}");
		assertThrows(ConditionError.class, () -> createCondition(StreamStatus.enabled).execute(env));
	}

	@Test
	public void shouldFailWhenStatusIsNotAString() {
		prepareResponse("{\"stream_id\":\"stream_1\",\"status\":1}");
		assertThrows(ConditionError.class, () -> createCondition(StreamStatus.enabled).execute(env));
	}

	@Test
	public void shouldFailWhenBodyIsNotAJsonObject() {
		prepareResponse("[\"enabled\"]");
		assertThrows(ConditionError.class, () -> createCondition(StreamStatus.enabled).execute(env));
	}

	@Test
	public void shouldFailWhenBodyIsMissing() {
		prepareResponse(null);
		assertThrows(ConditionError.class, () -> createCondition(StreamStatus.enabled).execute(env));
	}
}
