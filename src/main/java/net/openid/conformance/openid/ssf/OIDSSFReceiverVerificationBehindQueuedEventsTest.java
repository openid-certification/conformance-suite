package net.openid.conformance.openid.ssf;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openid.ssf.conditions.OIDSSFFindingCondition;
import net.openid.conformance.openid.ssf.conditions.OIDSSFLogSuccessCondition;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFGenerateStreamSET;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@PublishTestModule(
	testName = "openid-ssf-receiver-verification-behind-queued-events",
	displayName = "OpenID Shared Signals Framework: Test Receiver accepts a verification event delivered behind queued events",
	summary = """
		This test verifies that the receiver does not depend on the verification event being delivered synchronously or ahead of other events.
		The test generates a dynamic transmitter and waits for a receiver to register a stream. As soon as the stream is created, three ordinary events are queued for it; the verification event the receiver requests afterwards queues behind them, so the receiver gets the three events first and the verification event last (PUSH delivery: the pushes start with the verification request; POLL delivery: the polls return the queued events first).
		SSF 1.0 8.1.4.2: "Event Receivers MUST NOT depend on the Verification Event being transmitted synchronously or in any particular order relative to the current queue of events."
		The testsuite expects to observe the following interactions:
		 * create a stream
		 * request a stream verification
		 * acknowledge the three queued events and the verification event (202 Accepted on PUSH per RFC 8935 2.2, or 'ack' on POLL per RFC 8936 2.4)
		 * delete the stream
		""",
	profile = "OIDSSF"
)
public class OIDSSFReceiverVerificationBehindQueuedEventsTest extends AbstractOIDSSFReceiverTestModule {

	protected static final int QUEUED_EVENT_COUNT = 3;

	volatile String createdStreamId;

	volatile String verificationStreamId;

	volatile String deletedStreamId;

	volatile boolean queuedEventsGenerated;

	final Set<String> eventsEnqueued = ConcurrentHashMap.newKeySet();

	final Set<String> eventsAcked = ConcurrentHashMap.newKeySet();

	/** Event types in the order the receiver acknowledged them, for the log at the end. */
	final List<String> acknowledgementOrder = new CopyOnWriteArrayList<>();

	@Override
	public void start() {
		super.start();
		scheduleTask(new CheckTestFinishedTask(this::isFinished), 10, TimeUnit.SECONDS);
	}

	@Override
	protected boolean isFinished() {
		return createdStreamId != null
			&& queuedEventsGenerated
			&& createdStreamId.equals(verificationStreamId)
			&& didReceiveExpectedAcksForQueuedEvents()
			&& createdStreamId.equals(deletedStreamId);
	}

	protected boolean didReceiveExpectedAcksForQueuedEvents() {
		// Events for which no acknowledgement can arrive any more must not stall the test
		// until it times out; whether every queued event was acknowledged is judged in
		// fireTestFinished.
		Set<String> expectedAcks = new LinkedHashSet<>(eventsEnqueued);
		expectedAcks.removeAll(getResolvedWithoutAckJtis());
		return eventsAcked.containsAll(expectedAcks);
	}

	@Override
	protected void afterStreamCreation(String streamId, JsonObject createResult, JsonElement error) {
		if (createResult == null) {
			return;
		}
		createdStreamId = streamId;
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream creation for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.1");

		if (error == null && streamId != null) {
			queueEventsAheadOfVerification(streamId, createResult);
		}
	}

	/**
	 * Queues ordinary events right after stream creation, i.e. before the receiver can have
	 * requested verification. With PUSH delivery nothing is pushed until the receiver's
	 * verification request starts the push task, so the queued events arrive first and the
	 * verification event last; with POLL delivery the receiver's polls return them first.
	 */
	protected void queueEventsAheadOfVerification(String streamId, JsonObject createResult) {
		JsonObject streamConfig = createResult.getAsJsonObject("result");
		JsonArray eventsDelivered = streamConfig == null ? null : streamConfig.getAsJsonArray("events_delivered");
		String eventType = SsfEvents.preferredEventType(eventsDelivered == null ? null : OIDFJSON.convertJsonArrayToList(eventsDelivered));

		// The receiver must accept the queued events whatever their subject, so one subject
		// suffices; a Complex Subject is avoided as the profile lets a receiver reject it.
		JsonObject subject = getPrimaryEventSubject();
		long timestamp = Instant.now().getEpochSecond();

		for (int i = 0; i < QUEUED_EVENT_COUNT; i++) {
			SsfEvent event = generateSsfEventExample(eventType, timestamp);
			var generateStreamSET = new OIDSSFGenerateStreamSET(eventStore, streamId, subject, event, this::onStreamEventEnqueued);
			callAndContinueOnFailure(generateStreamSET, Condition.ConditionResult.WARNING, event.requirements().toArray(new String[0]));
		}
		queuedEventsGenerated = true;

		eventLog.log(getName(), args(
			"msg", "Queued " + QUEUED_EVENT_COUNT + " events before the receiver requested verification; the verification event will be delivered after them",
			"stream_id", streamId,
			"event_type", eventType,
			"queued_jtis", List.copyOf(eventsEnqueued)));
	}

	@Override
	protected void onStreamEventEnqueued(String streamId, String jti) {
		eventsEnqueued.add(jti);
	}

	@Override
	protected void afterPushDeliverySuccess(String streamId, OIDSSFSecurityEvent event) {
		acknowledgementOrder.add(event.type());
		if (SsfEvents.isVerificationEvent(event.type())) {
			verificationStreamId = streamId;
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition(
					"Detected Stream Verification via PUSH delivery for stream_id=" + streamId + " (delivered after " + eventsAcked.size() + " of the queued events)"),
				Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.2");
			return;
		}
		eventsAcked.add(event.jti());
	}

	@Override
	protected void onStreamEventAcknowledged(String streamId, String jti, OIDSSFSecurityEvent event) {
		acknowledgementOrder.add(event.type());
		if (SsfEvents.isVerificationEvent(event.type())) {
			verificationStreamId = streamId;
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition(
					"Detected Stream Verification via POLL delivery for stream_id=" + streamId + " (acknowledged after " + eventsAcked.size() + " of the queued events)"),
				Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.2");
			return;
		}
		eventsAcked.add(jti);
	}

	@Override
	protected void onStreamEventErrorReported(String streamId, String jti, JsonObject error) {
		super.onStreamEventErrorReported(streamId, jti, error);
		if (eventsEnqueued.contains(jti)) {
			callAndContinueOnFailure(new OIDSSFFindingCondition(
					"Receiver reported an error via 'setErrs' for a valid event that was queued ahead of the verification event (jti=" + jti + "): " + error
						+ ". Receivers must accept and acknowledge valid events regardless of their position relative to the verification event."),
				Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.2", "RFC8936-2.4");
		}
	}

	@Override
	protected void afterStreamDeletion(String streamId, JsonObject deleteResult, JsonElement error) {
		if (error != null || streamId == null) {
			// deletion failed (e.g. 404 for an unknown or already-deleted stream) - do not
			// record it as the successful deletion or reset previously recorded state
			return;
		}
		deletedStreamId = streamId;
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream deletion for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.5");
	}

	@Override
	public void fireTestFinished() {
		eventLog.log(getName(), args(
			"msg", "Order in which the receiver acknowledged the delivered events",
			"acknowledgement_order", List.copyOf(acknowledgementOrder)));

		// Undeliverable events were never handed to the receiver, error reports via setErrs
		// and rejected push deliveries are graded where they happen. What remains are events
		// the receiver retrieved but never acknowledged before deleting the stream.
		Set<String> unacknowledged = new LinkedHashSet<>(eventsEnqueued);
		unacknowledged.removeAll(eventsAcked);
		unacknowledged.removeAll(getUndeliveredEventJtis());
		unacknowledged.removeAll(getErrorReportedEventJtis());
		unacknowledged.removeAll(getRejectedPushEventJtis());
		if (unacknowledged.isEmpty()) {
			eventLog.log(getName(), "Detected acknowledgements for the events queued ahead of the verification event.");
		} else {
			callAndContinueOnFailure(new OIDSSFFindingCondition(
					"The receiver never acknowledged " + unacknowledged.size() + " of the " + QUEUED_EVENT_COUNT + " events queued ahead of the verification event "
						+ "before deleting the stream (jtis: " + unacknowledged + "). Receivers must acknowledge accepted SETs via 'ack' or a 202 response."),
				Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.2", "RFC8936-2.4");
		}
		super.fireTestFinished();
	}
}
