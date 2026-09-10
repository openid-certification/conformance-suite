package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.openid.ssf.SsfConstants;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.openid.ssf.eventstore.OIDSSFInMemoryEventStore;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that deleting a stream records every event that can no longer be
 * delivered or acknowledged, so test modules stop waiting for their acks.
 * Still-queued events are reported as undeliverable (the receiver never got
 * them); events a poll receiver retrieved but neither acknowledged nor
 * reported via setErrs are reported separately as unresolved, so modules can
 * still grade the missing acknowledgement.
 */
@ExtendWith(MockitoExtension.class)
public class OIDSSFHandleStreamDeleteRequest_UnitTest {

	private static final String STREAM_ID = "stream_123";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFInMemoryEventStore eventStore;

	private final List<String> undeliverableJtis = new ArrayList<>();

	private final List<String> unresolvedJtis = new ArrayList<>();

	@BeforeEach
	public void setUp() {
		eventStore = new OIDSSFInMemoryEventStore();
		undeliverableJtis.clear();
		unresolvedJtis.clear();
	}

	private void prepareStream(String deliveryMethod) {
		JsonObject streamConfig = JsonParser.parseString("""
			{"stream_id":"%s","delivery":{"method":"%s"}}
			""".formatted(STREAM_ID, deliveryMethod)).getAsJsonObject();
		JsonObject streams = new JsonObject();
		streams.add(STREAM_ID, streamConfig);
		JsonObject ssf = new JsonObject();
		ssf.add("streams", streams);
		env.putObject("ssf", ssf);

		JsonObject queryParams = new JsonObject();
		queryParams.addProperty("stream_id", STREAM_ID);
		JsonObject incomingRequest = new JsonObject();
		incomingRequest.add("query_string_params", queryParams);
		env.putObject("incoming_request", incomingRequest);
	}

	private OIDSSFHandleStreamDeleteRequest createCondition() {
		OIDSSFHandleStreamDeleteRequest condition = new OIDSSFHandleStreamDeleteRequest(eventStore,
			(streamId, events) -> events.forEach(event -> undeliverableJtis.add(event.jti())),
			(streamId, events) -> events.forEach(event -> unresolvedJtis.add(event.jti())));
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return condition;
	}

	private void storeEvent(String jti) {
		eventStore.storeEvent(STREAM_ID, new OIDSSFSecurityEvent(jti, "token-" + jti, "type"));
	}

	private JsonObject result() {
		return env.getElementFromObject("ssf", "stream_op_result").getAsJsonObject();
	}

	@Test
	void answersAnUnknownStreamIdWith404WithoutGradingIt() {
		// SSF 1.0 8.1.1.5, Table 5: 404 is the transmitter's regular answer for an unknown
		// stream_id, e.g. a receiver deleting the stream of an earlier run before creating one
		prepareStream(SsfConstants.DELIVERY_METHOD_PUSH_RFC_8935_URI);
		env.getElementFromObject("incoming_request", "query_string_params").getAsJsonObject().addProperty("stream_id", "stream_from_an_earlier_run");

		assertDoesNotThrow(() -> createCondition().execute(env));

		assertEquals(404, OIDFJSON.getInt(result().get("status_code")));
		assertEquals("not_found", OIDFJSON.getString(result().getAsJsonObject("error").get("err")));
		assertTrue(env.getElementFromObject("ssf", "streams").getAsJsonObject().has(STREAM_ID), "the existing stream is untouched");
	}

	@Test
	void answersADeleteWithoutAnyStreamWith404WithoutGradingIt() {
		prepareStream(SsfConstants.DELIVERY_METHOD_PUSH_RFC_8935_URI);
		env.getElementFromObject("ssf", "streams").getAsJsonObject().remove(STREAM_ID);

		assertDoesNotThrow(() -> createCondition().execute(env));

		assertEquals(404, OIDFJSON.getInt(result().get("status_code")));
	}

	@Test
	void pushDeletionRecordsStillQueuedEvents() {
		prepareStream(SsfConstants.DELIVERY_METHOD_PUSH_RFC_8935_URI);
		storeEvent("jti-1");
		storeEvent("jti-2");
		// jti-1 was polled into the currently-executing push batch; the push task
		// itself accounts for in-flight batch events on deletion
		eventStore.pollEvents(STREAM_ID, 1);

		assertDoesNotThrow(() -> createCondition().execute(env));

		assertEquals(List.of("jti-2"), undeliverableJtis);
		assertTrue(unresolvedJtis.isEmpty());
	}

	@Test
	void pollDeletionSplitsQueuedFromRetrievedButUnackedEvents() {
		prepareStream(SsfConstants.DELIVERY_METHOD_POLL_RFC_8936_URI);
		storeEvent("jti-1");
		storeEvent("jti-2");
		storeEvent("jti-3");
		// receiver retrieved jti-1 and jti-2 via poll, acknowledged only jti-1
		eventStore.pollEvents(STREAM_ID, 2);
		eventStore.registerAckForStreamEvent(STREAM_ID, "jti-1");

		assertDoesNotThrow(() -> createCondition().execute(env));

		// jti-3 was never retrieved - undeliverable, not the receiver's fault;
		// jti-2 was retrieved but never acknowledged - unresolved, graded by the module
		assertEquals(List.of("jti-3"), undeliverableJtis);
		assertEquals(List.of("jti-2"), unresolvedJtis);
	}

	@Test
	void pollDeletionDoesNotRecordErrorReportedEvents() {
		prepareStream(SsfConstants.DELIVERY_METHOD_POLL_RFC_8936_URI);
		storeEvent("jti-1");
		storeEvent("jti-2");
		eventStore.pollEvents(STREAM_ID, 2);
		// the receiver rejected jti-1 via setErrs - that resolves it just like an ack
		eventStore.registerErrorForStreamEvent(STREAM_ID, "jti-1", new JsonObject());

		assertDoesNotThrow(() -> createCondition().execute(env));

		assertTrue(undeliverableJtis.isEmpty());
		assertEquals(List.of("jti-2"), unresolvedJtis);
	}

	@Test
	void deletionWithEverythingAcknowledgedRecordsNothing() {
		prepareStream(SsfConstants.DELIVERY_METHOD_POLL_RFC_8936_URI);
		storeEvent("jti-1");
		eventStore.pollEvents(STREAM_ID, 1);
		eventStore.registerAckForStreamEvent(STREAM_ID, "jti-1");

		assertDoesNotThrow(() -> createCondition().execute(env));

		assertTrue(undeliverableJtis.isEmpty());
		assertTrue(unresolvedJtis.isEmpty());
	}
}
