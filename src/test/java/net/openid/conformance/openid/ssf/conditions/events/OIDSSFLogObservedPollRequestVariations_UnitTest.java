package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandlePollRequest;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * The summary is informational: RFC 8936 2.4 lets the receiver choose its poll request
 * variation, so the condition never fails, whatever was (or was not) recorded.
 */
@ExtendWith(MockitoExtension.class)
public class OIDSSFLogObservedPollRequestVariations_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFLogObservedPollRequestVariations condition;

	@BeforeEach
	void setUp() {
		condition = new OIDSSFLogObservedPollRequestVariations();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env.putObject("ssf", new JsonObject());
	}

	private void recorded(String variation, int count) {
		JsonObject variations = env.getElementFromObject("ssf", OIDSSFHandlePollRequest.POLL_REQUEST_VARIATIONS_KEY) == null
			? new JsonObject()
			: env.getElementFromObject("ssf", OIDSSFHandlePollRequest.POLL_REQUEST_VARIATIONS_KEY).getAsJsonObject();
		variations.addProperty(variation, count);
		env.putObject("ssf", OIDSSFHandlePollRequest.POLL_REQUEST_VARIATIONS_KEY, variations);
	}

	@Test
	void doesNotFailWhenNothingWasRecorded() {
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void summarisesASingleVariation() {
		recorded(OIDSSFHandlePollRequest.VARIATION_POLL_ONLY, 3);
		recorded(OIDSSFHandlePollRequest.VARIATION_SHORT_POLL, 3);
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void summarisesEveryVariation() {
		recorded(OIDSSFHandlePollRequest.VARIATION_POLL_ONLY, 1);
		recorded(OIDSSFHandlePollRequest.VARIATION_ACKNOWLEDGE_ONLY, 1);
		recorded(OIDSSFHandlePollRequest.VARIATION_POLL_WITH_ACKNOWLEDGEMENT, 2);
		recorded(OIDSSFHandlePollRequest.VARIATION_LONG_POLL, 2);
		recorded(OIDSSFHandlePollRequest.VARIATION_SHORT_POLL, 2);
		assertDoesNotThrow(() -> condition.execute(env));
	}
}
