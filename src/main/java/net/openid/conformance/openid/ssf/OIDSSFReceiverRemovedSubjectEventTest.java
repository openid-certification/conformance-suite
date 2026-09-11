package net.openid.conformance.openid.ssf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openid.ssf.conditions.OIDSSFFindingCondition;
import net.openid.conformance.openid.ssf.conditions.OIDSSFLogSuccessCondition;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFGenerateStreamSET;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamUtils;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamUtils.StreamSubjectOperation;
import net.openid.conformance.openid.ssf.variant.SsfProfile;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@PublishTestModule(
	testName = "openid-ssf-receiver-removed-subject-event",
	displayName = "OpenID Shared Signals Framework: Test Receiver Tolerates Events for a Removed Subject",
	summary = """
		This test verifies that the receiver tolerates an event for a subject it removed from the stream.
		The test generates a dynamic transmitter and waits for a receiver to register and verify a stream. The receiver then has to remove a subject from the stream via the remove-subject endpoint (the subject does not need to have been added before). Once it has, the transmitter generates one event of a requested event type for exactly that removed subject and delivers it: a transmitter may keep sending events for a removed subject for some time (SSF 1.0 9.3).
		Receivers must tolerate events for subjects that were removed from the stream and must not report them as errors to the transmitter, so the receiver must accept the event (HTTP 202 on PUSH delivery, 'ack' on POLL delivery); an error response or a 'setErrs' report fails the test. An event the receiver does not retrieve within 60 seconds cannot be assessed.
		The testsuite expects to observe the following interactions:
		 * create a stream
		 * verify the stream
		 * remove a subject from the stream
		 * acknowledge the event delivered for the removed subject
		 * delete the stream
		""",
	profile = "OIDSSF"
)
@VariantNotApplicable(parameter = SsfProfile.class, values = "caep_interop")
public class OIDSSFReceiverRemovedSubjectEventTest extends AbstractOIDSSFReceiverTestModule {

	private static final int REMOVED_SUBJECT_EVENT_ASSESSMENT_TIMEOUT_SECONDS = 60;

	volatile String createdStreamId;

	volatile String verificationStreamId;

	volatile String deletedStreamId;

	/** The first subject the receiver removed from the stream. */
	volatile JsonObject removedSubject;

	/** {@code jti} of the event generated for the removed subject, once generated. */
	volatile String removedSubjectEventJti;

	final AtomicBoolean removedSubjectEventGenerated = new AtomicBoolean();

	/** Set once the receiver acknowledged the event or once it can no longer be assessed. */
	volatile boolean removedSubjectEventAssessed;

	@Override
	public void start() {
		super.start();
		scheduleTask(new CheckTestFinishedTask(this::isFinished), 4, TimeUnit.SECONDS);
	}

	@Override
	protected boolean isFinished() {
		if (createdStreamId == null || !createdStreamId.equals(deletedStreamId)) {
			return false;
		}
		if (removedSubject == null || !removedSubjectEventGenerated.get()) {
			// nothing to wait for; the missing subject removal is graded in fireTestFinished
			return true;
		}
		return isRemovedSubjectEventResolved();
	}

	/**
	 * Whether the event for the removed subject needs no further waiting: acknowledged, reported
	 * as an error, rejected, purged or left unresolved by the delete, or past the assessment
	 * timeout.
	 */
	private boolean isRemovedSubjectEventResolved() {
		String jti = removedSubjectEventJti;
		return jti == null || removedSubjectEventAssessed || getResolvedWithoutAckJtis().contains(jti);
	}

	@Override
	public void fireTestFinished() {
		gradeRemovedSubjectEvent();
		super.fireTestFinished();
	}

	private void gradeRemovedSubjectEvent() {
		if (removedSubject == null) {
			callAndContinueOnFailure(new OIDSSFFindingCondition(
					"The receiver deleted the stream without removing a subject from it, so no event for a removed subject could be delivered. "
						+ "The receiver has to remove a subject via the remove-subject endpoint after verifying the stream."),
				Condition.ConditionResult.FAILURE, "OIDSSF-9.3");
			return;
		}
		if (!removedSubjectEventGenerated.get()) {
			callAndContinueOnFailure(new OIDSSFFindingCondition(
					"The receiver removed a subject but deleted the stream before the transmitter could deliver an event for it. "
						+ "The receiver has to verify the stream first and keep it open until the event for the removed subject was delivered."),
				Condition.ConditionResult.FAILURE, "OIDSSF-9.3");
			return;
		}
		String jti = removedSubjectEventJti;
		if (jti == null) {
			// the reason the event was not generated is already in the log
			return;
		}
		if (getUndeliveredEventJtis().contains(jti)) {
			eventLog.log(getName(), args(
				"msg", "The receiver deleted the stream before the event for the removed subject was delivered; whether it tolerates such events cannot be assessed",
				"jti", jti, "subject", removedSubject));
		} else if (getUnresolvedEventJtis().contains(jti)) {
			callAndContinueOnFailure(new OIDSSFFindingCondition(
					"The receiver retrieved the event for the removed subject (jti " + jti + ") but neither acknowledged it nor reported it before deleting the stream. "
						+ "Accepted SETs must be acknowledged via 'ack' on POLL delivery."),
				Condition.ConditionResult.FAILURE, "RFC8936-2.4");
		}
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
	protected void afterPushDeliverySuccess(String streamId, OIDSSFSecurityEvent event) {
		if (SsfEvents.isVerificationEvent(event.type()) && verificationStreamId == null) {
			verificationStreamId = streamId;
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream Verification via PUSH delivery for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
			generateRemovedSubjectEventWhenReady();
			return;
		}
		if (event.jti().equals(removedSubjectEventJti)) {
			onRemovedSubjectEventAccepted(streamId, event.jti(), "202 response to the PUSH delivery");
		}
	}

	@Override
	protected void onStreamEventAcknowledged(String streamId, String jti, OIDSSFSecurityEvent event) {
		if (SsfEvents.isVerificationEvent(event.type()) && verificationStreamId == null) {
			verificationStreamId = streamId;
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream Verification via POLL delivery for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
			scheduleAfterStreamVerification(this::generateRemovedSubjectEventWhenReady);
			return;
		}
		if (jti.equals(removedSubjectEventJti)) {
			onRemovedSubjectEventAccepted(streamId, jti, "'ack' in a POLL request");
		}
	}

	private void onRemovedSubjectEventAccepted(String streamId, String jti, String how) {
		removedSubjectEventAssessed = true;
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Receiver accepted the event for the removed subject via " + how + " for stream_id=" + streamId + " (jti=" + jti + ")"),
			Condition.ConditionResult.FAILURE, "OIDSSF-9.3");
	}

	@Override
	protected void onStreamEventErrorReported(String streamId, String jti, JsonObject error) {
		super.onStreamEventErrorReported(streamId, jti, error);
		if (jti.equals(removedSubjectEventJti)) {
			removedSubjectEventAssessed = true;
			callAndContinueOnFailure(new OIDSSFFindingCondition(
					"The receiver reported the event for the subject it removed from the stream as an error via 'setErrs' (jti " + jti + ", error " + error + "). "
						+ "Receivers must tolerate events for subjects that were removed from the stream and must not report them as errors to the transmitter."),
				Condition.ConditionResult.FAILURE, "OIDSSF-9.3");
		}
	}

	/** A rejected push of the removed-subject event is recorded at INFO here and graded once in {@link #onPushDeliveryNotAcknowledged}. */
	@Override
	protected Condition.ConditionResult getPushDeliveryRejectionSeverity(OIDSSFSecurityEvent event) {
		if (event.jti().equals(removedSubjectEventJti)) {
			return Condition.ConditionResult.INFO;
		}
		return super.getPushDeliveryRejectionSeverity(event);
	}

	@Override
	protected void onPushDeliveryNotAcknowledged(String streamId, OIDSSFSecurityEvent event) {
		super.onPushDeliveryNotAcknowledged(streamId, event);
		if (event.jti().equals(removedSubjectEventJti)) {
			removedSubjectEventAssessed = true;
			callAndContinueOnFailure(new OIDSSFFindingCondition(
					"The receiver did not accept the push delivery of the event for the subject it removed from the stream (jti " + event.jti() + "). "
						+ "Receivers must tolerate events for subjects that were removed from the stream and must not report them as errors to the transmitter."),
				Condition.ConditionResult.FAILURE, "OIDSSF-9.3");
		}
	}

	@Override
	protected void afterStreamSubjectChange(StreamSubjectOperation operation, String streamId, JsonObject result, JsonElement error) {
		if (operation != StreamSubjectOperation.remove || error != null || streamId == null || removedSubject != null) {
			return;
		}
		JsonElement subject = result.get("subject");
		if (subject == null || !subject.isJsonObject()) {
			return;
		}
		JsonObject removed = subject.getAsJsonObject().deepCopy();
		removed.remove("_verified");
		removedSubject = removed;
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected removal of a subject from stream_id=" + streamId + ": " + removed),
			Condition.ConditionResult.FAILURE, "OIDSSF-8.1.3.3");
		// generated in the background so the remove-subject request is answered first
		scheduleTask(() -> {
			generateRemovedSubjectEventWhenReady();
			return "done";
		}, 1, TimeUnit.SECONDS);
	}

	/**
	 * Generates the event for the removed subject once the stream is verified and a subject was
	 * removed, whichever happens last, and exactly once.
	 */
	private void generateRemovedSubjectEventWhenReady() {
		String streamId = createdStreamId;
		JsonObject subject = removedSubject;
		if (streamId == null || verificationStreamId == null || subject == null) {
			return;
		}
		if (!removedSubjectEventGenerated.compareAndSet(false, true)) {
			return;
		}
		JsonObject streamConfig = OIDSSFStreamUtils.getStreamConfig(env, streamId);
		if (streamConfig == null) {
			eventLog.log(getName(), args(
				"msg", "The receiver deleted the stream before the event for the removed subject could be generated; whether it tolerates such events cannot be assessed",
				"stream_id", streamId, "subject", subject));
			return;
		}

		String eventType = selectSubjectEventType(streamConfig);
		if (eventType == null) {
			removedSubjectEventAssessed = true;
			callAndContinueOnFailure(new OIDSSFFindingCondition(
					"The stream delivers no event type that identifies a subject (only the SSF framework events), so no event for the removed subject can be generated. "
						+ "Request at least one CAEP or RISC event type when creating the stream."),
				Condition.ConditionResult.FAILURE, "OIDSSF-9.3");
			return;
		}

		SsfEvent event = generateSsfEventExample(eventType, Instant.now().getEpochSecond());
		Set<String> requirements = new LinkedHashSet<>(event.requirements());
		requirements.add("OIDSSF-9.3");
		eventLog.log(getName(), args(
			"msg", "Generating an event for the subject the receiver removed from the stream; the receiver must tolerate it",
			"stream_id", streamId, "event_type", eventType, "subject", subject));
		callAndContinueOnFailure(new OIDSSFGenerateStreamSET(eventStore, streamId, subject, event, (sid, jti) -> removedSubjectEventJti = jti),
			Condition.ConditionResult.FAILURE, requirements.toArray(new String[0]));

		if (OIDSSFStreamUtils.isPushDelivery(streamConfig)) {
			schedulePushDelivery(streamId);
		}

		scheduleTask(() -> {
			if (!isRemovedSubjectEventResolved()) {
				eventLog.log(getName(), args(
					"msg", "The receiver has neither acknowledged nor reported the event for the removed subject within " + REMOVED_SUBJECT_EVENT_ASSESSMENT_TIMEOUT_SECONDS
						+ " seconds; whether it tolerates such events cannot be assessed unless it does so before deleting the stream",
					"jti", removedSubjectEventJti));
				removedSubjectEventAssessed = true;
			}
			return "done";
		}, REMOVED_SUBJECT_EVENT_ASSESSMENT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
	}

	/** The first delivered event type that is about a subject rather than about the stream itself. */
	private static String selectSubjectEventType(JsonObject streamConfig) {
		JsonElement eventsDelivered = streamConfig.get("events_delivered");
		if (eventsDelivered == null || !eventsDelivered.isJsonArray()) {
			return null;
		}
		List<String> eventTypes = OIDFJSON.convertJsonArrayToList(eventsDelivered.getAsJsonArray());
		return eventTypes.stream()
			.filter(eventType -> !SsfEvents.SSF_EVENT_TYPES.contains(eventType))
			.findFirst()
			.orElse(null);
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
}
