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
public class OIDSSFValidatePushDeliveryErrorResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFValidatePushDeliveryErrorResponse condition;

	@BeforeEach
	public void setUp() {
		condition = new OIDSSFValidatePushDeliveryErrorResponse();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
	}

	private void prepareResponse(int status, String contentType, String body) {
		prepareResponse(status, contentType, "en", body);
	}

	private void prepareResponse(int status, String contentType, String contentLanguage, String body) {
		JsonObject response = new JsonObject();
		response.addProperty("status", status);
		JsonObject headers = new JsonObject();
		if (contentType != null) {
			headers.addProperty("content-type", contentType);
		}
		if (contentLanguage != null) {
			headers.addProperty("content-language", contentLanguage);
		}
		response.add("headers", headers);
		if (body != null) {
			response.addProperty("body", body);
			try {
				response.add("body_json", JsonParser.parseString(body));
			} catch (RuntimeException e) {
				// not JSON: body_json stays absent, as AbstractCallEndpoint leaves it
			}
		}
		env.putObject("endpoint_response", response);
	}

	@Test
	public void passesForWellFormedErrorResponse() {
		prepareResponse(400, "application/json; charset=utf-8", "{\"err\":\"invalid_key\",\"description\":\"Key ID 12345 has been revoked.\"}");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	public void failsWhenNoResponseWasReceived() {
		prepareResponse(0, null, null);
		ConditionError error = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(error.getMessage().contains("No HTTP response"));
	}

	@Test
	public void failsForStatusOtherThan400() {
		prepareResponse(422, "application/json", "{\"err\":\"invalid_key\",\"description\":\"nope\"}");
		ConditionError error = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(error.getMessage().contains("400"));
	}

	@Test
	public void failsForNonJsonContentType() {
		prepareResponse(400, "text/plain", "{\"err\":\"invalid_key\",\"description\":\"nope\"}");
		ConditionError error = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(error.getMessage().contains("application/json"));
	}

	@Test
	public void failsWhenContentTypeIsMissing() {
		prepareResponse(400, null, "{\"err\":\"invalid_key\",\"description\":\"nope\"}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void failsWhenContentLanguageIsMissing() {
		prepareResponse(400, "application/json", null, "{\"err\":\"invalid_key\",\"description\":\"nope\"}");
		ConditionError e = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(e.getMessage().contains("Content-Language"));
	}

	@Test
	public void failsWhenContentLanguageIsBlank() {
		prepareResponse(400, "application/json", " ", "{\"err\":\"invalid_key\",\"description\":\"nope\"}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void failsWhenBodyIsNotJson() {
		prepareResponse(400, "application/json", "invalid key");
		ConditionError error = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(error.getMessage().contains("JSON object"));
	}

	@Test
	public void failsWhenBodyIsAJsonArray() {
		prepareResponse(400, "application/json", "[\"invalid_key\"]");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void failsWhenBodyIsEmpty() {
		prepareResponse(400, "application/json", null);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void failsWhenErrIsMissing() {
		prepareResponse(400, "application/json", "{\"description\":\"nope\"}");
		ConditionError error = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(error.getMessage().contains("'err'"));
	}

	@Test
	public void failsWhenErrIsNotAString() {
		prepareResponse(400, "application/json", "{\"err\":42,\"description\":\"nope\"}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void failsWhenDescriptionIsMissing() {
		prepareResponse(400, "application/json", "{\"err\":\"invalid_key\"}");
		ConditionError error = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(error.getMessage().contains("'description'"));
	}

	@Test
	public void failsWhenDescriptionIsNotAString() {
		prepareResponse(400, "application/json", "{\"err\":\"invalid_key\",\"description\":{\"en\":\"nope\"}}");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}
}
