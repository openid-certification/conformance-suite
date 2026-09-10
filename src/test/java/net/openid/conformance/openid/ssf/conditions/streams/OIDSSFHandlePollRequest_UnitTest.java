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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
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
		pollRequest("""
			{"returnImmediately": %s, "maxEvents": 16}
			""".formatted(returnImmediately));
	}

	private void pollRequest(String bodyJson) {
		pollRequest(STREAM, bodyJson);
	}

	private void pollRequest(String streamId, String bodyJson) {
		JsonObject request = JsonParser.parseString("""
			{"query_string_params": {"stream_id": "%s"}, "body_json": %s}
			""".formatted(streamId, bodyJson)).getAsJsonObject();
		env.putObject("incoming_request", request);
	}

	private void setStreamStatus(String status) {
		env.getElementFromObject("ssf", "streams." + STREAM + "._status").getAsJsonObject().addProperty("status", status);
	}

	@Test
	void pausedStreamStillProcessesAcknowledgementsButDeliversNothing() {
		// SSF 1.0 8.1.2.1: paused only means the transmitter "MUST NOT transmit events over the
		// stream"; an acknowledgement concerns SETs the receiver already holds (RFC 8936 2.4)
		// and must be honoured in any state.
		setStreamStatus("paused");
		pollRequest("""
			{"ack": ["jti-1"], "returnImmediately": true, "maxEvents": 16}
			""");

		condition.execute(env);

		assertTrue(eventStore.isStreamEventAcked(STREAM, "jti-1"));
		JsonObject result = condition.getResult();
		assertEquals(200, OIDFJSON.getInt(result.get("status_code")));
		assertTrue(result.getAsJsonObject("result").getAsJsonObject("sets").isEmpty());
	}

	@Test
	void disabledStreamStillProcessesSetErrsButDeliversNothing() {
		setStreamStatus("disabled");
		pollRequest("""
			{"setErrs": {"jti-1": {"err": "invalid_request", "description": "x"}}, "returnImmediately": true}
			""");

		condition.execute(env);

		assertTrue(eventStore.isErrorForStreamEvent(STREAM, "jti-1") != null);
		JsonObject result = condition.getResult();
		assertTrue(result.getAsJsonObject("result").getAsJsonObject("sets").isEmpty());
	}

	@Test
	void longPollReleasesTheTestLockWhileWaitingAndReacquiresIt() {
		pollRequest(false);

		condition.execute(env);

		InOrder inOrder = inOrder(lockManager);
		inOrder.verify(lockManager).releaseLock();
		inOrder.verify(lockManager).reacquireLock();
		JsonObject result = condition.getResult();
		assertEquals(200, OIDFJSON.getInt(result.get("status_code")));
		assertTrue(result.getAsJsonObject("result").getAsJsonObject("sets").has("jti-1"));
	}

	@Test
	void longPollKeepsItsOwnResultWhenAnotherPollRequestRunsMeanwhile() throws Exception {
		// RFC 8936 2.4.2: a receiver may acknowledge "on separate threads" from the one that
		// polls, so a second poll request can arrive while a long poll has released the lock.
		String otherStream = "stream-2";
		env.getElementFromObject("ssf", "streams").getAsJsonObject().add(otherStream, JsonParser.parseString("""
			{"stream_id": "%s", "_status": {"stream_id": "%s", "status": "enabled"}}
			""".formatted(otherStream, otherStream)).getAsJsonObject());

		CountDownLatch lockReleased = new CountDownLatch(1);
		doAnswer(invocation -> {
			lockReleased.countDown();
			return null;
		}).when(lockManager).releaseLock();

		OIDSSFHandlePollRequest longPoll = new OIDSSFHandlePollRequest(eventStore, (streamId, jti, event) -> { }, (streamId, jti, error) -> { });
		longPoll.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		longPoll.setLockManager(lockManager);
		pollRequest(otherStream, "{\"returnImmediately\": false, \"maxEvents\": 16}");
		Thread longPollThread = new Thread(() -> longPoll.execute(env));
		longPollThread.start();
		assertTrue(lockReleased.await(5, TimeUnit.SECONDS), "the long poll did not release the lock");

		// the other request is answered while the long poll is still waiting for an event
		pollRequest(true);
		condition.execute(env);
		assertTrue(condition.getResult().getAsJsonObject("result").getAsJsonObject("sets").has("jti-1"));

		eventStore.storeEvent(otherStream, new OIDSSFSecurityEvent("jti-2", "set-2", "urn:example:event"));
		longPollThread.join(TimeUnit.SECONDS.toMillis(15));
		assertFalse(longPollThread.isAlive(), "the long poll did not return after an event was stored");

		JsonObject longPollSets = longPoll.getResult().getAsJsonObject("result").getAsJsonObject("sets");
		assertTrue(longPollSets.has("jti-2"));
		assertFalse(longPollSets.has("jti-1"));
	}

	@Test
	void shortPollKeepsTheTestLock() {
		pollRequest(true);

		condition.execute(env);

		verify(lockManager, never()).releaseLock();
		verify(lockManager, never()).reacquireLock();
		JsonObject result = condition.getResult();
		assertTrue(result.getAsJsonObject("result").getAsJsonObject("sets").has("jti-1"));
	}

	private int variationCount(String variation) {
		JsonObject variations = env.getElementFromObject("ssf", OIDSSFHandlePollRequest.POLL_REQUEST_VARIATIONS_KEY).getAsJsonObject();
		return variations.has(variation) ? OIDFJSON.getInt(variations.get(variation)) : 0;
	}

	@Test
	void recordsAPollWithoutAcknowledgementAsPollOnly() {
		// RFC 8936 2.4.1: an empty ack array acknowledges nothing, so it is still poll-only
		pollRequest("""
			{"ack": [], "returnImmediately": true}
			""");

		condition.execute(env);

		assertEquals(1, variationCount(OIDSSFHandlePollRequest.VARIATION_POLL_ONLY));
		assertEquals(0, variationCount(OIDSSFHandlePollRequest.VARIATION_ACKNOWLEDGE_ONLY));
		assertEquals(0, variationCount(OIDSSFHandlePollRequest.VARIATION_POLL_WITH_ACKNOWLEDGEMENT));
		assertEquals(1, variationCount(OIDSSFHandlePollRequest.VARIATION_SHORT_POLL));
		assertEquals(0, variationCount(OIDSSFHandlePollRequest.VARIATION_LONG_POLL));
	}

	@Test
	void recordsAnAcknowledgementWithMaxEventsZeroAsAcknowledgeOnly() {
		pollRequest("""
			{"ack": ["jti-1"], "maxEvents": 0, "returnImmediately": true}
			""");

		condition.execute(env);

		assertEquals(1, variationCount(OIDSSFHandlePollRequest.VARIATION_ACKNOWLEDGE_ONLY));
		assertEquals(0, variationCount(OIDSSFHandlePollRequest.VARIATION_POLL_ONLY));
		assertEquals(0, variationCount(OIDSSFHandlePollRequest.VARIATION_POLL_WITH_ACKNOWLEDGEMENT));
	}

	@Test
	void recordsAnAcknowledgementThatAlsoPollsAsPollWithAcknowledgement() {
		pollRequest("""
			{"ack": ["jti-1"], "maxEvents": 16, "returnImmediately": true}
			""");

		condition.execute(env);

		assertEquals(1, variationCount(OIDSSFHandlePollRequest.VARIATION_POLL_WITH_ACKNOWLEDGEMENT));
		assertEquals(0, variationCount(OIDSSFHandlePollRequest.VARIATION_ACKNOWLEDGE_ONLY));
		assertEquals(0, variationCount(OIDSSFHandlePollRequest.VARIATION_POLL_ONLY));
	}

	@Test
	void recordsASetErrsReportAsAnAcknowledgement() {
		pollRequest("""
			{"setErrs": {"jti-1": {"err": "invalid_request", "description": "x"}}, "maxEvents": 0, "returnImmediately": true}
			""");

		condition.execute(env);

		assertEquals(1, variationCount(OIDSSFHandlePollRequest.VARIATION_ACKNOWLEDGE_ONLY));
	}

	@Test
	void recordsALongPollWhenReturnImmediatelyIsAbsent() {
		pollRequest("{}");

		condition.execute(env);

		assertEquals(1, variationCount(OIDSSFHandlePollRequest.VARIATION_POLL_ONLY));
		assertEquals(1, variationCount(OIDSSFHandlePollRequest.VARIATION_LONG_POLL));
		assertEquals(0, variationCount(OIDSSFHandlePollRequest.VARIATION_SHORT_POLL));
	}

	@Test
	void accumulatesCountsAcrossRequests() {
		pollRequest(true);
		condition.execute(env);
		pollRequest("""
			{"ack": ["jti-1"], "maxEvents": 0, "returnImmediately": true}
			""");
		condition.execute(env);

		assertEquals(1, variationCount(OIDSSFHandlePollRequest.VARIATION_POLL_ONLY));
		assertEquals(1, variationCount(OIDSSFHandlePollRequest.VARIATION_ACKNOWLEDGE_ONLY));
		assertEquals(2, variationCount(OIDSSFHandlePollRequest.VARIATION_SHORT_POLL));
	}
}
