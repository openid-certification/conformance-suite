package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
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

@ExtendWith(MockitoExtension.class)
public class OIDSSFWarnPollRequestContentTypeNotJson_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFWarnPollRequestContentTypeNotJson condition;

	@BeforeEach
	void setUp() {
		condition = new OIDSSFWarnPollRequestContentTypeNotJson();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
	}

	private void prepare(String contentType) {
		JsonObject headers = new JsonObject();
		if (contentType != null) {
			headers.addProperty("content-type", contentType);
		}
		JsonObject request = new JsonObject();
		request.add("headers", headers);
		env.putObject("incoming_request", request);
	}

	@Test
	void passesForApplicationJson() {
		prepare("application/json");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void passesForApplicationJsonWithCharset() {
		prepare("application/json; charset=utf-8");
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void warnsForAnotherContentType() {
		prepare("text/plain");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void warnsWithoutContentType() {
		prepare(null);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}
}
