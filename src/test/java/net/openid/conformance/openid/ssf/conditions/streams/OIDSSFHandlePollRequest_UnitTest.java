package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.openid.ssf.eventstore.OIDSSFInMemoryEventStore;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.TestLockManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * The poll handler runs on a request thread that holds the test lock. A long poll
 * (RFC 8936 2.2: {@code returnImmediately} defaults to false) may wait seconds for an
 * event, so the lock must be released around the wait - otherwise every other request the
 * receiver makes in the meantime (an acknowledgement, a jwks fetch, a delete) queues behind it.
 */
@ExtendWith(MockitoExtension.class)
public class OIDSSFHandlePollRequest_UnitTest {

	private static final String STREAM = "stream-1";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private final OIDSSFInMemoryEventStore eventStore = new OIDSSFInMemoryEventStore();

	private TestLockManager lockManager;

	private OIDSSFHandlePollRequest condition;

	@BeforeEach
	void setUp() {
		lockManager = mock(TestLockManager.class);
		condition = new OIDSSFHandlePollRequest(eventStore, (streamId, jti, event) -> { }, (streamId, jti, error) -> { });
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		condition.setLockManager(lockManager);

		JsonObject ssf = JsonParser.parseString("""
			{"streams": {"%s": {"stream_id": "%s", "_status": {"stream_id": "%s", "status": "enabled"}}}}
			""".formatted(STREAM, STREAM, STREAM)).getAsJsonObject();
		env.putObject("ssf", ssf);
		eventStore.storeEvent(STREAM, new OIDSSFSecurityEvent("jti-1", "set-1", "urn:example:event"));
	}

	private void pollRequest(boolean returnImmediately) {
		JsonObject request = JsonParser.parseString("""
			{"query_string_params": {"stream_id": "%s"}, "body_json": {"returnImmediately": %s, "maxEvents": 16}}
			""".formatted(STREAM, returnImmediately)).getAsJsonObject();
		env.putObject("incoming_request", request);
	}

	@Test
	void longPollReleasesTheTestLockWhileWaitingAndReacquiresIt() {
		pollRequest(false);

		condition.execute(env);

		InOrder inOrder = inOrder(lockManager);
		inOrder.verify(lockManager).releaseLock();
		inOrder.verify(lockManager).reacquireLock();
		JsonObject result = env.getElementFromObject("ssf", "poll_result").getAsJsonObject();
		assertEquals(200, OIDFJSON.getInt(result.get("status_code")));
		assertTrue(result.getAsJsonObject("result").getAsJsonObject("sets").has("jti-1"));
	}

	@Test
	void shortPollKeepsTheTestLock() {
		pollRequest(true);

		condition.execute(env);

		verify(lockManager, never()).releaseLock();
		verify(lockManager, never()).reacquireLock();
		JsonObject result = env.getElementFromObject("ssf", "poll_result").getAsJsonObject();
		assertTrue(result.getAsJsonObject("result").getAsJsonObject("sets").has("jti-1"));
	}
}
