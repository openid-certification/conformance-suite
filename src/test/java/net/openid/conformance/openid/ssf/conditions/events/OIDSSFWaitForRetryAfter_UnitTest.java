package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class OIDSSFWaitForRetryAfter_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFWaitForRetryAfter condition;

	@BeforeEach
	void setUp() {
		condition = new OIDSSFWaitForRetryAfter();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void prepare(String retryAfter, Integer minVerificationInterval) {
		JsonObject headers = new JsonObject();
		if (retryAfter != null) {
			headers.addProperty("retry-after", retryAfter);
		}
		JsonObject response = new JsonObject();
		response.addProperty("status", 429);
		response.add("headers", headers);
		env.putObject("resource_endpoint_response_full", response);

		JsonObject stream = new JsonObject();
		if (minVerificationInterval != null) {
			stream.addProperty("min_verification_interval", minVerificationInterval);
		}
		JsonObject ssf = JsonParser.parseString("{}").getAsJsonObject();
		ssf.add("stream", stream);
		env.putObject("ssf", ssf);
	}

	@Test
	void usesTheRetryAfterHeaderInDeltaSeconds() {
		prepare("45", 300);
		assertEquals(45, condition.getExpectedWaitSeconds(env));
	}

	@Test
	void fallsBackToTheAdvertisedIntervalWithoutRetryAfter() {
		prepare(null, 300);
		assertEquals(300, condition.getExpectedWaitSeconds(env));
	}

	@Test
	void fallsBackToTheAdvertisedIntervalForAnHttpDateRetryAfter() {
		prepare("Wed, 21 Oct 2026 07:28:00 GMT", 120);
		assertEquals(120, condition.getExpectedWaitSeconds(env));
	}

	@Test
	void usesTheDefaultWithoutAnyHint() {
		prepare(null, null);
		assertEquals(OIDSSFWaitForRetryAfter.DEFAULT_WAIT_SECONDS, condition.getExpectedWaitSeconds(env));
	}

	@Test
	void capsAnExcessiveWait() {
		prepare("86400", null);
		assertEquals(OIDSSFWaitForRetryAfter.MAX_WAIT_SECONDS, condition.getExpectedWaitSeconds(env));
	}
}
