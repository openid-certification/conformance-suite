package net.openid.conformance.openid.ssf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openid.ssf.conditions.OIDSSFFindingCondition;
import net.openid.conformance.openid.ssf.conditions.OIDSSFLogSuccessCondition;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFValidatePushDeliveryErrorResponse;
import net.openid.conformance.openid.ssf.conditions.streams.AbstractOIDSSFGenerateStreamSET;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFGenerateWrongSubjectStreamVerificationSET;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamUtils;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@PublishTestModule(
	testName = "openid-ssf-receiver-verification-wrong-subject",
	displayName = "OpenID Shared Signals Framework: Test Receiver rejects a verification event that names another stream",
	summary = """
		This test verifies that the receiver validates the subject of a verification event.
		The test generates a dynamic transmitter and waits for a receiver to register a stream and request its verification. The verification event answering the first verification request echoes the receiver's 'state' correctly but carries an opaque 'sub_id' whose id is not the stream's; later verification requests are answered normally, so the receiver can complete the verification afterwards.
		SSF 1.0 8.1.4.1: "Upon receiving a Verification Event, the Event Receiver SHALL parse the SET and validate its claims", and the verification event's sub_id "MUST always be set to have a simple value of type opaque. The id of the value MUST be the stream_id of the stream being verified".
		A receiver that accepts such an event is reported as a warning: the specification names no receiver reaction for a subject that does not match, unlike for a state mismatch. With PUSH delivery a rejection is validated as an RFC 8935 2.3 error response (a 400 with an 'application/json' body carrying 'err' and 'description'); no particular 'err' code is expected.
		Note: with POLL delivery a verification event that was retrieved but neither acknowledged nor reported via 'setErrs' is noted after 60 seconds; deleting the stream without ever retrieving it fails the test, since the handling under test was not exercised. The test still waits for the stream deletion before it finishes.
		The testsuite expects to observe the following interactions:
		 * create a stream
		 * request a stream verification
		 * reject the verification event whose sub_id names another stream - PUSH delivery: answer the delivery with an error response (RFC 8935 2.3); POLL delivery: do not list its 'jti' in 'ack', optionally report it via 'setErrs' (RFC 8936 2.4.4)
		 * optionally request the verification again and acknowledge the verification event
		 * delete the stream
		""",
	profile = "OIDSSF"
)
public class OIDSSFReceiverVerificationWrongSubjectTest extends AbstractOIDSSFReceiverTestModule {

	protected static final int SILENT_DROP_RESOLUTION_TIMEOUT_SECONDS = 60;

	volatile String createdStreamId;

	volatile String deletedStreamId;

	/** Set once the first verification request was answered with the wrong-subject event. */
	volatile boolean wrongSubjectAnswered;

	/** The verification SET naming another stream, once generated. */
	volatile OIDSSFSecurityEvent wrongSubjectEvent;

	/** Set once the receiver's handling of the wrong-subject event was graded (or found unassessable). */
	volatile boolean wrongSubjectGraded;

	@Override
	public void start() {
		super.start();
		scheduleTask(new CheckTestFinishedTask(this::isFinished), 10, TimeUnit.SECONDS);
	}

	@Override
	protected boolean isFinished() {
		return createdStreamId != null
			&& wrongSubjectGraded
			&& createdStreamId.equals(deletedStreamId);
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
			return;
		}
		deletedStreamId = streamId;
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream deletion for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.5");
	}

	/**
	 * The first verification request of the run is answered with a verification event naming
	 * another stream; every later request gets a regular verification event, so the receiver
	 * can complete the verification.
	 */
	@Override
	protected AbstractOIDSSFGenerateStreamSET createVerificationSetGenerator(String streamId) {
		if (wrongSubjectAnswered) {
			return super.createVerificationSetGenerator(streamId);
		}
		wrongSubjectAnswered = true;
		return new OIDSSFGenerateWrongSubjectStreamVerificationSET(eventStore, event -> {
			wrongSubjectEvent = event;
			if (!OIDSSFStreamUtils.isPushDelivery(OIDSSFStreamUtils.getStreamConfig(env, streamId))) {
				// POLL delivery: silently dropping the event (neither ack nor setErrs) keeps
				// the outcome unobservable - resolve it after a while.
				scheduleTask(new ResolveSilentlyIgnoredVerificationEventTask(streamId), SILENT_DROP_RESOLUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
			}
		});
	}

	protected boolean isWrongSubjectEvent(String jti) {
		OIDSSFSecurityEvent event = wrongSubjectEvent;
		return event != null && event.jti().equals(jti);
	}

	/**
	 * Rejecting the wrong-subject event is the expected outcome; the base 202 check must not
	 * grade it as a failed delivery. The receiver's answer is graded in
	 * {@link #afterPushDeliverySuccess} and {@link #onPushDeliveryNotAcknowledged} instead.
	 */
	@Override
	protected Condition.ConditionResult getPushDeliveryRejectionSeverity(OIDSSFSecurityEvent event) {
		if (isWrongSubjectEvent(event.jti())) {
			return Condition.ConditionResult.INFO;
		}
		return super.getPushDeliveryRejectionSeverity(event);
	}

	@Override
	protected void afterPushDeliverySuccess(String streamId, OIDSSFSecurityEvent event) {
		if (isWrongSubjectEvent(event.jti())) {
			gradeWrongSubjectEventAccepted(event.jti(), "accepted", "RFC8935-2.3");
			return;
		}
		if (SsfEvents.isVerificationEvent(event.type())) {
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream Verification via PUSH delivery for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
		}
	}

	@Override
	protected void onPushDeliveryNotAcknowledged(String streamId, OIDSSFSecurityEvent event) {
		super.onPushDeliveryNotAcknowledged(streamId, event);
		if (!isWrongSubjectEvent(event.jti()) || wrongSubjectGraded) {
			return;
		}
		Integer status = env.getInteger("endpoint_response", "status");
		if (status != null && status >= 400) {
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition(
					"Receiver rejected the verification event naming another stream (jti=" + event.jti() + ") with HTTP status " + status),
				Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1", "RFC8935-2.3");
		}
		// the rejection must be a 400 with a JSON body carrying 'err' and 'description'; no
		// registered error code is defined for a subject mismatch, so the code is only logged
		callAndContinueOnFailure(OIDSSFValidatePushDeliveryErrorResponse.class, Condition.ConditionResult.FAILURE, "RFC8935-2.3");
		eventLog.log(getName(), args("msg", "Error code reported for the verification event naming another stream",
			"err", env.getElementFromObject("endpoint_response", "body_json.err")));
		wrongSubjectGraded = true;
	}

	@Override
	protected void onStreamEventAcknowledged(String streamId, String jti, OIDSSFSecurityEvent event) {
		if (isWrongSubjectEvent(jti)) {
			gradeWrongSubjectEventAccepted(jti, "acknowledged", "RFC8936-2.4");
			return;
		}
		if (SsfEvents.isVerificationEvent(event.type())) {
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream Verification via POLL delivery for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
		}
	}

	/**
	 * Acceptance (a 202 on push, an {@code ack} on poll) is a WARNING: SSF 1.0 8.1.4.1 obliges
	 * the receiver to validate the claims and fixes the subject to the stream being verified,
	 * but names no reaction to a subject that does not match.
	 */
	protected void gradeWrongSubjectEventAccepted(String jti, String howAccepted, String deliveryRequirement) {
		if (wrongSubjectGraded) {
			return;
		}
		callAndContinueOnFailure(new OIDSSFFindingCondition(
				"Receiver " + howAccepted + " a verification event whose sub_id names another stream than the one being verified (jti=" + jti + "). "
					+ "Receivers must validate the claims of a verification event, whose sub_id must be the opaque stream_id of the stream being verified; "
					+ "graded as a warning because the specification names no receiver reaction to a subject that does not match."),
			Condition.ConditionResult.WARNING, "OIDSSF-8.1.4.1", deliveryRequirement);
		wrongSubjectGraded = true;
	}

	@Override
	protected void onStreamEventErrorReported(String streamId, String jti, JsonObject error) {
		super.onStreamEventErrorReported(streamId, jti, error);
		if (!isWrongSubjectEvent(jti) || wrongSubjectGraded) {
			return;
		}
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition(
				"Receiver reported an error for the verification event naming another stream (jti=" + jti + "): " + error),
			Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1", "RFC8936-2.4");
		eventLog.log(getName(), args("msg", "Error code reported for the verification event naming another stream",
			"err", OIDFJSON.tryGetString(error.get("err"))));
		wrongSubjectGraded = true;
	}

	@Override
	protected void onEventsUndeliverable(String streamId, List<OIDSSFSecurityEvent> events) {
		super.onEventsUndeliverable(streamId, events);
		if (wrongSubjectGraded || events.stream().noneMatch(event -> isWrongSubjectEvent(event.jti()))) {
			return;
		}
		callAndContinueOnFailure(new OIDSSFFindingCondition(
				"The receiver deleted the stream without ever retrieving the verification event naming another stream, so its handling could not be assessed. "
					+ "Keep the stream open and keep polling until the verification event was retrieved, then delete it."),
			Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
		wrongSubjectGraded = true;
	}

	@Override
	protected void onEventsUnresolvedAtDeletion(String streamId, List<OIDSSFSecurityEvent> events) {
		super.onEventsUnresolvedAtDeletion(streamId, events);
		if (wrongSubjectGraded || events.stream().noneMatch(event -> isWrongSubjectEvent(event.jti()))) {
			return;
		}
		callAndContinueOnFailure(new OIDSSFFindingCondition(
				"Receiver retrieved the verification event naming another stream but neither acknowledged it nor reported it via 'setErrs' before deleting the stream. "
					+ "Not acknowledging is correct; reporting it via 'setErrs' would make the rejection observable."),
			Condition.ConditionResult.INFO, "OIDSSF-8.1.4.1", "RFC8936-2.4");
		wrongSubjectGraded = true;
	}

	protected class ResolveSilentlyIgnoredVerificationEventTask implements Callable<String> {

		protected final String streamId;

		protected ResolveSilentlyIgnoredVerificationEventTask(String streamId) {
			this.streamId = streamId;
		}

		@Override
		public String call() throws Exception {
			OIDSSFSecurityEvent event = wrongSubjectEvent;
			if (wrongSubjectGraded || event == null) {
				return "done";
			}
			wrongSubjectGraded = true;

			Set<String> stillQueuedJtis = new HashSet<>(getUndeliveredEventJtis());
			eventStore.getQueuedEvents(streamId).forEach(queued -> stillQueuedJtis.add(queued.jti()));

			if (stillQueuedJtis.contains(event.jti())) {
				// graded when the stream is deleted, see onEventsUndeliverable
				wrongSubjectGraded = false;
				eventLog.log(getName(), args("msg", "Receiver has not retrieved the verification event naming another stream within "
						+ SILENT_DROP_RESOLUTION_TIMEOUT_SECONDS + " seconds; its handling is assessed once retrieved, and a stream deletion before that fails the test",
					"jti", event.jti()));
				return "done";
			}
			callAndContinueOnFailure(new OIDSSFFindingCondition(
					"Receiver retrieved the verification event naming another stream (jti=" + event.jti() + ") but neither acknowledged it "
						+ "nor reported it via 'setErrs' within " + SILENT_DROP_RESOLUTION_TIMEOUT_SECONDS + " seconds. Not acknowledging is correct; "
						+ "reporting it via 'setErrs' would make the rejection observable."),
				Condition.ConditionResult.INFO, "OIDSSF-8.1.4.1", "RFC8936-2.4");
			return "done";
		}
	}

	@Override
	public void fireTestFinished() {
		eventLog.log(getName(), "Detected the receiver's handling of the verification event naming another stream.");
		super.fireTestFinished();
	}
}
