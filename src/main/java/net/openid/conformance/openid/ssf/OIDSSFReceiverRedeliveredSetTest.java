package net.openid.conformance.openid.ssf;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openid.ssf.conditions.OIDSSFFindingCondition;
import net.openid.conformance.openid.ssf.conditions.OIDSSFLogSuccessCondition;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFGenerateStreamSET;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandlePushDeliveryToReceiver;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamUtils;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@PublishTestModule(
	testName = "openid-ssf-receiver-redelivered-set",
	displayName = "OpenID Shared Signals Framework: Test Receiver accepts a redelivered Security Event Token",
	summary = """
		This test verifies that the receiver accepts and acknowledges a SET it has already acknowledged when the transmitter delivers it again.
		The test generates a dynamic transmitter and waits for a receiver to register a stream and verify it; once verified, it delivers one ordinary event and, once the receiver acknowledged it, delivers the very same SET (same 'jti', same token) a second time.
		RFC 8936 2.4: "a SET Transmitter MAY redeliver SETs it has previously delivered. The SET Recipient SHOULD accept repeat SETs and acknowledge the SETs regardless of whether the Recipient believes it has already acknowledged the SETs previously." For PUSH delivery RFC 8935 2 states: "The SET Recipient MUST respond as it would if the SET had not been previously received". The 'jti' identifies a SET (RFC 8417 2.2).
		A PUSH receiver that rejects the repeated SET fails the test (RFC 8935 2, MUST); a POLL receiver that reports it via 'setErrs' is warned (RFC 8936 2.4, SHOULD).
		Note: with POLL delivery a redelivered SET that is neither acknowledged again nor reported via 'setErrs' is noted after 60 seconds. The test still waits for the stream deletion before it finishes.
		The testsuite expects to observe the following interactions:
		 * create a stream
		 * verify the stream
		 * acknowledge the event (202 Accepted on PUSH per RFC 8935 2.2, or 'ack' on POLL per RFC 8936 2.4)
		 * acknowledge the same event again when it is delivered a second time
		 * delete the stream
		""",
	profile = "OIDSSF"
)
public class OIDSSFReceiverRedeliveredSetTest extends AbstractOIDSSFReceiverTestModule {

	protected static final int SILENT_DROP_RESOLUTION_TIMEOUT_SECONDS = 60;

	/** Delay between the receiver's first acknowledgement and the second delivery of the SET. */
	protected static final int REDELIVERY_DELAY_SECONDS = 2;

	volatile String createdStreamId;

	volatile String verificationStreamId;

	volatile String deletedStreamId;

	/** The ordinary event delivered twice, once generated. */
	volatile OIDSSFSecurityEvent event;

	/** Set once the receiver acknowledged the first delivery (and the redelivery was scheduled). */
	volatile boolean firstDeliveryAcknowledged;

	/** Set once the SET was handed to the receiver a second time. */
	volatile boolean redelivered;

	/** Set once the receiver's handling of the second delivery was graded (or found unassessable). */
	volatile boolean redeliveryGraded;

	@Override
	public void start() {
		super.start();
		scheduleTask(new CheckTestFinishedTask(this::isFinished), 10, TimeUnit.SECONDS);
	}

	@Override
	protected boolean isFinished() {
		return createdStreamId != null
			&& createdStreamId.equals(verificationStreamId)
			&& event != null
			&& redeliveryGraded
			&& createdStreamId.equals(deletedStreamId);
	}

	protected boolean isRedeliveredEvent(String jti) {
		OIDSSFSecurityEvent generated = event;
		return generated != null && generated.jti().equals(jti);
	}

	@Override
	protected void afterStreamCreation(String streamId, JsonObject createResult, JsonElement error) {
		if (createResult == null) {
			return;
		}
		createdStreamId = streamId;
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream creation for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.1");
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

		if (redelivered && !redeliveryGraded) {
			// POLL delivery: the redelivered SET shares its jti with the acknowledged first
			// delivery, so the stream deletion does not report it as unresolved
			redeliveryGraded = true;
			callAndContinueOnFailure(new OIDSSFFindingCondition(
					"Receiver deleted the stream without acknowledging the redelivered SET (jti=" + event.jti() + ") again or reporting it via 'setErrs'."),
				Condition.ConditionResult.INFO, "RFC8936-2.4");
		}
	}

	@Override
	protected void afterPushDeliverySuccess(String streamId, OIDSSFSecurityEvent deliveredEvent) {
		// PUSH delivery: the first successfully delivered verification event is the cue that
		// the receiver completed stream verification.
		if (SsfEvents.isVerificationEvent(deliveredEvent.type()) && verificationStreamId == null) {
			verificationStreamId = streamId;
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream Verification via PUSH delivery for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
			afterInitialStreamVerification(streamId);
			return;
		}
		if (isRedeliveredEvent(deliveredEvent.jti()) && !firstDeliveryAcknowledged) {
			firstDeliveryAcknowledged = true;
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Receiver acknowledged the first delivery of the SET (jti=" + deliveredEvent.jti() + ") via PUSH delivery"),
				Condition.ConditionResult.FAILURE, "RFC8935-2.2");
			scheduleTask(new RedeliverViaPushTask(streamId), REDELIVERY_DELAY_SECONDS, TimeUnit.SECONDS);
		}
	}

	@Override
	protected void onPushDeliveryNotAcknowledged(String streamId, OIDSSFSecurityEvent deliveredEvent) {
		super.onPushDeliveryNotAcknowledged(streamId, deliveredEvent);
		if (isRedeliveredEvent(deliveredEvent.jti()) && !firstDeliveryAcknowledged && !redeliveryGraded) {
			// the base 202 check grades the rejected first delivery; without an acknowledged
			// first delivery there is nothing to redeliver
			redeliveryGraded = true;
			callAndContinueOnFailure(new OIDSSFFindingCondition(
					"Receiver did not acknowledge the first delivery of the SET (jti=" + deliveredEvent.jti() + "), so its handling of a redelivered SET could not be assessed."),
				Condition.ConditionResult.INFO, "RFC8936-2.4");
		}
	}

	@Override
	protected void onStreamEventAcknowledged(String streamId, String jti, OIDSSFSecurityEvent ackedEvent) {
		// POLL delivery: the ack of the verification SET is the cue to start.
		if (SsfEvents.isVerificationEvent(ackedEvent.type()) && verificationStreamId == null) {
			verificationStreamId = streamId;
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream Verification via POLL delivery for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
			scheduleAfterStreamVerification(() -> afterInitialStreamVerification(streamId));
			return;
		}
		if (!isRedeliveredEvent(jti)) {
			return;
		}
		if (!firstDeliveryAcknowledged) {
			firstDeliveryAcknowledged = true;
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Receiver acknowledged the first delivery of the SET (jti=" + jti + ") via 'ack'"),
				Condition.ConditionResult.FAILURE, "RFC8936-2.4");
			scheduleTask(new RedeliverViaPollTask(streamId), REDELIVERY_DELAY_SECONDS, TimeUnit.SECONDS);
			return;
		}
		if (redelivered && !redeliveryGraded) {
			redeliveryGraded = true;
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Receiver acknowledged the redelivered SET (jti=" + jti + ") again via 'ack'"),
				Condition.ConditionResult.FAILURE, "RFC8936-2.4", "RFC8417-2.2");
			return;
		}
		eventLog.log(getName(), args("msg", "Receiver acknowledged the SET again before it was redelivered; ignoring the repeated acknowledgement", "jti", jti));
	}

	@Override
	protected void onStreamEventErrorReported(String streamId, String jti, JsonObject error) {
		super.onStreamEventErrorReported(streamId, jti, error);
		if (!isRedeliveredEvent(jti) || redeliveryGraded) {
			return;
		}
		redeliveryGraded = true;
		if (redelivered) {
			callAndContinueOnFailure(new OIDSSFFindingCondition(
					"Receiver reported an error via 'setErrs' for the redelivered SET (jti=" + jti + ") instead of acknowledging it again: " + error
						+ ". Receivers should accept repeat SETs and acknowledge them regardless of whether they believe they already acknowledged them."),
				Condition.ConditionResult.WARNING, "RFC8936-2.4", "RFC8417-2.2");
			return;
		}
		callAndContinueOnFailure(new OIDSSFFindingCondition(
				"Receiver reported an error via 'setErrs' for the first delivery of a valid SET (jti=" + jti + "): " + error
					+ ". Its handling of a redelivered SET could not be assessed."),
			Condition.ConditionResult.FAILURE, "RFC8936-2.4");
	}

	@Override
	protected void onEventsUndeliverable(String streamId, List<OIDSSFSecurityEvent> events) {
		super.onEventsUndeliverable(streamId, events);
		if (redeliveryGraded || events.stream().noneMatch(undeliverable -> isRedeliveredEvent(undeliverable.jti()))) {
			return;
		}
		redeliveryGraded = true;
		callAndContinueOnFailure(new OIDSSFFindingCondition(redelivered
				? "Receiver deleted the stream before it retrieved the redelivered SET, so its handling could not be assessed."
				: "Receiver deleted the stream before the SET was delivered the first time, so its handling of a redelivered SET could not be assessed."),
			Condition.ConditionResult.WARNING, "RFC8936-2.4");
	}

	@Override
	protected void onEventsUnresolvedAtDeletion(String streamId, List<OIDSSFSecurityEvent> events) {
		super.onEventsUnresolvedAtDeletion(streamId, events);
		if (redeliveryGraded || events.stream().noneMatch(unresolved -> isRedeliveredEvent(unresolved.jti()))) {
			return;
		}
		redeliveryGraded = true;
		callAndContinueOnFailure(new OIDSSFFindingCondition(
				"Receiver retrieved the SET (jti=" + event.jti() + ") but never acknowledged it before deleting the stream. "
					+ "Receivers must acknowledge accepted SETs via 'ack'; the handling of a redelivered SET could not be assessed."),
			Condition.ConditionResult.FAILURE, "RFC8936-2.4");
	}

	protected void afterInitialStreamVerification(String streamId) {
		JsonObject streamConfig = OIDSSFStreamUtils.getStreamConfig(env, streamId);
		if (streamConfig == null) {
			return;
		}
		JsonArray eventsDelivered = streamConfig.getAsJsonArray("events_delivered");
		String eventType = SsfEvents.preferredEventType(eventsDelivered == null ? null : OIDFJSON.convertJsonArrayToList(eventsDelivered));

		// The receiver must accept the redelivered SET whatever its subject, so one subject
		// suffices; a Complex Subject is avoided as the profile lets a receiver reject it.
		JsonObject subject = getPrimaryEventSubject();
		SsfEvent ssfEvent = generateSsfEventExample(eventType, Instant.now().getEpochSecond());

		var generateStreamSET = new OIDSSFGenerateStreamSET(eventStore, streamId, subject, ssfEvent,
			(sid, jti) -> event = eventStore.getRegisteredSecurityEvent(sid, jti));
		callAndStopOnFailure(generateStreamSET, ssfEvent.requirements().toArray(new String[0]));
		// PUSH delivery: the push task started by the verification request picks the event up;
		// POLL delivery: the receiver's next poll returns it.
	}

	protected class RedeliverViaPushTask implements Callable<String> {

		protected final String streamId;

		protected RedeliverViaPushTask(String streamId) {
			this.streamId = streamId;
		}

		@Override
		public String call() throws Exception {
			if (redeliveryGraded) {
				return "done";
			}
			if (OIDSSFStreamUtils.getStreamConfig(env, streamId) == null) {
				redeliveryGraded = true;
				callAndContinueOnFailure(new OIDSSFFindingCondition(
						"Receiver deleted the stream before the SET could be delivered a second time, so its handling of a redelivered SET could not be assessed."),
					Condition.ConditionResult.WARNING, "RFC8936-2.4");
				return "done";
			}

			eventLog.log(getName(), args("msg", "Delivering the already acknowledged SET a second time via PUSH delivery",
				"stream_id", streamId, "jti", event.jti(), "event_type", event.type()));
			redelivered = true;
			// The delivery bookkeeping of the base class already counts this SET as delivered
			// and acknowledged, so the second delivery grades its own response below.
			callAndContinueOnFailure(new OIDSSFHandlePushDeliveryToReceiver(streamId, event, (sid, ev) -> {
			}, (sid, ev) -> {
			}), Condition.ConditionResult.WARNING, "OIDSSF-6.1.1");

			redeliveryGraded = true;
			Integer status = env.getInteger("endpoint_response", "status");
			if (status != null && status >= 200 && status < 300) {
				callAndContinueOnFailure(new OIDSSFLogSuccessCondition(
						"Receiver acknowledged the redelivered SET (jti=" + event.jti() + ") again with HTTP status " + status),
					Condition.ConditionResult.FAILURE, "RFC8935-2", "RFC8936-2.4", "RFC8417-2.2");
				return "done";
			}
			// a pushed SET may be transmitted multiple times and the receiver must respond as if
			// it had not been received before - a MUST for push delivery, unlike the poll SHOULD
			callAndContinueOnFailure(new OIDSSFFindingCondition(
					"Receiver did not acknowledge the redelivered SET (jti=" + event.jti() + "): HTTP status " + status
						+ ", body: " + env.getString("endpoint_response", "body")
						+ ". A receiver must respond to a repeated SET as it would if the SET had not been received before, acknowledging it again."),
				Condition.ConditionResult.FAILURE, "RFC8935-2", "RFC8417-2.2");
			return "done";
		}
	}

	protected class RedeliverViaPollTask implements Callable<String> {

		protected final String streamId;

		protected RedeliverViaPollTask(String streamId) {
			this.streamId = streamId;
		}

		@Override
		public String call() throws Exception {
			if (redeliveryGraded) {
				return "done";
			}
			if (OIDSSFStreamUtils.getStreamConfig(env, streamId) == null) {
				redeliveryGraded = true;
				callAndContinueOnFailure(new OIDSSFFindingCondition(
						"Receiver deleted the stream before the SET could be delivered a second time, so its handling of a redelivered SET could not be assessed."),
					Condition.ConditionResult.WARNING, "RFC8936-2.4");
				return "done";
			}

			eventLog.log(getName(), args("msg", "Queueing the already acknowledged SET a second time; the receiver's next poll returns it again",
				"stream_id", streamId, "jti", event.jti(), "event_type", event.type()));
			eventStore.storeEvent(streamId, event);
			redelivered = true;
			// silently dropping the redelivered SET (neither ack nor setErrs) keeps the outcome
			// unobservable - resolve it after a while
			scheduleTask(new ResolveSilentlyIgnoredRedeliveryTask(streamId), SILENT_DROP_RESOLUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
			return "done";
		}
	}

	protected class ResolveSilentlyIgnoredRedeliveryTask implements Callable<String> {

		protected final String streamId;

		protected ResolveSilentlyIgnoredRedeliveryTask(String streamId) {
			this.streamId = streamId;
		}

		@Override
		public String call() throws Exception {
			if (redeliveryGraded) {
				return "done";
			}
			redeliveryGraded = true;

			boolean stillQueued = eventStore.getQueuedEvents(streamId).stream().anyMatch(queued -> isRedeliveredEvent(queued.jti()));
			if (stillQueued) {
				callAndContinueOnFailure(new OIDSSFFindingCondition(
						"Receiver never retrieved the redelivered SET (jti=" + event.jti() + ") within " + SILENT_DROP_RESOLUTION_TIMEOUT_SECONDS
							+ " seconds, so its handling could not be assessed."),
					Condition.ConditionResult.WARNING, "RFC8936-2.4");
				return "done";
			}
			callAndContinueOnFailure(new OIDSSFFindingCondition(
					"Receiver retrieved the redelivered SET (jti=" + event.jti() + ") but neither acknowledged it again nor reported it via 'setErrs' within "
						+ SILENT_DROP_RESOLUTION_TIMEOUT_SECONDS + " seconds. Receivers should accept repeat SETs and acknowledge them regardless of whether they believe they already acknowledged them."),
				Condition.ConditionResult.INFO, "RFC8936-2.4");
			return "done";
		}
	}

	@Override
	public void fireTestFinished() {
		eventLog.log(getName(), "Detected the receiver's handling of the redelivered SET.");
		super.fireTestFinished();
	}
}
