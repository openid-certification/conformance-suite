package net.openid.conformance.openid.ssf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openid.ssf.conditions.OIDSSFFindingCondition;
import net.openid.conformance.openid.ssf.conditions.OIDSSFLogSuccessCondition;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFValidatePushDeliveryErrorResponse;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFWarnPushDeliveryErrorCodeMismatch;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFWarnPushDeliveryErrorCodeNotRegistered;
import net.openid.conformance.openid.ssf.conditions.streams.AbstractOIDSSFGenerateStreamSET;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFGenerateWrongStateStreamVerificationSET;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamUtils;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@PublishTestModule(
	testName = "openid-ssf-receiver-verification-wrong-state",
	displayName = "OpenID Shared Signals Framework: Test Receiver rejects a verification event with an unexpected state",
	summary = """
		This test verifies that the receiver confirms the 'state' of a verification event.
		The test generates a dynamic transmitter and waits for a receiver to register a stream and request its verification. The verification event answering the first verification request carries a 'state' the receiver did not send (a different value when the request carried a state, a made-up value when it carried none); later verification requests are answered normally, so the receiver can complete the verification afterwards.
		SSF 1.0 8.1.4.1: "the Event Receiver SHALL confirm that the value for state is as expected. If the value of state does not match, an error response with the err field set to invalid_state SHOULD be returned".
		A receiver that accepts an event echoing a different value than the state it sent fails the test. When the receiver's request carried no state, accepting an event that carries one is reported as a warning only: the receiver has no expected value to confirm, and whether an unrequested state counts as "not as expected" is not settled in SSF 1.0.
		With PUSH delivery the error response is validated as well: it must be a 400 with an 'application/json' body carrying 'err' and 'description' (RFC 8935 2.3); an 'err' other than 'invalid_state' raises a warning.
		Note: with POLL delivery a verification event that was retrieved but neither acknowledged nor reported via 'setErrs' is noted after 60 seconds; deleting the stream without ever retrieving it fails the test, since the handling under test was not exercised. The test still waits for the stream deletion before it finishes.
		The testsuite expects to observe the following interactions:
		 * create a stream
		 * request a stream verification
		 * reject the verification event whose state does not match - PUSH delivery: answer the delivery with an error response whose 'err' is 'invalid_state' (RFC 8935 2.3); POLL delivery: do not list its 'jti' in 'ack', report it via 'setErrs' with 'err' set to 'invalid_state' instead (RFC 8936 2.4.4)
		 * optionally request the verification again and acknowledge the verification event
		 * delete the stream
		""",
	profile = "OIDSSF"
)
public class OIDSSFReceiverVerificationWrongStateTest extends AbstractOIDSSFReceiverTestModule {

	protected static final int SILENT_DROP_RESOLUTION_TIMEOUT_SECONDS = 60;

	volatile String createdStreamId;

	volatile String deletedStreamId;

	/** Set once the first verification request was answered with the wrong-state event. */
	volatile boolean wrongStateAnswered;

	/** The verification SET carrying the wrong state, once generated. */
	volatile OIDSSFSecurityEvent wrongStateEvent;

	/**
	 * Whether the receiver's verification request carried no state, so the wrong-state event
	 * carries a state the receiver never asked for rather than a different value than it sent.
	 * Acceptance is then a warning, not a failure, see {@link #gradeWrongStateEventAccepted}.
	 */
	volatile boolean receiverSentNoState;

	/** Set once the receiver's handling of the wrong-state event was graded (or found unassessable). */
	volatile boolean wrongStateGraded;

	@Override
	public void start() {
		super.start();
		scheduleTask(new CheckTestFinishedTask(this::isFinished), 10, TimeUnit.SECONDS);
	}

	@Override
	protected boolean isFinished() {
		return createdStreamId != null
			&& wrongStateGraded
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
			// deletion failed (e.g. 404 for an unknown or already-deleted stream) - do not
			// record it as the successful deletion or reset previously recorded state
			return;
		}
		deletedStreamId = streamId;
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream deletion for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.5");
	}

	/**
	 * The first verification request of the run is answered with a verification event whose
	 * state the receiver did not send; every later request (of this or a re-created stream)
	 * gets a regular verification event, so the receiver can complete the verification.
	 */
	@Override
	protected AbstractOIDSSFGenerateStreamSET createVerificationSetGenerator(String streamId) {
		if (wrongStateAnswered) {
			return super.createVerificationSetGenerator(streamId);
		}
		wrongStateAnswered = true;
		return new OIDSSFGenerateWrongStateStreamVerificationSET(eventStore, (event, receiverSentState) -> {
			wrongStateEvent = event;
			receiverSentNoState = !receiverSentState;
			if (!OIDSSFStreamUtils.isPushDelivery(OIDSSFStreamUtils.getStreamConfig(env, streamId))) {
				// POLL delivery: silently dropping the event (neither ack nor setErrs) keeps
				// the outcome unobservable - resolve it after a while.
				scheduleTask(new ResolveSilentlyIgnoredVerificationEventTask(streamId), SILENT_DROP_RESOLUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
			}
		});
	}

	protected boolean isWrongStateEvent(String jti) {
		OIDSSFSecurityEvent event = wrongStateEvent;
		return event != null && event.jti().equals(jti);
	}

	/**
	 * Rejecting the wrong-state event is the expected outcome; the base 202 check must not
	 * grade it as a failed delivery. The receiver's answer is graded in
	 * {@link #afterPushDeliverySuccess} and {@link #onPushDeliveryNotAcknowledged} instead.
	 */
	@Override
	protected Condition.ConditionResult getPushDeliveryRejectionSeverity(OIDSSFSecurityEvent event) {
		if (isWrongStateEvent(event.jti())) {
			return Condition.ConditionResult.INFO;
		}
		return super.getPushDeliveryRejectionSeverity(event);
	}

	@Override
	protected void afterPushDeliverySuccess(String streamId, OIDSSFSecurityEvent event) {
		if (isWrongStateEvent(event.jti())) {
			gradeWrongStateEventAccepted(event.jti(), "accepted", "reject a mismatch with an error response", "RFC8935-2.3");
			return;
		}
		if (SsfEvents.isVerificationEvent(event.type())) {
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream Verification via PUSH delivery for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
		}
	}

	@Override
	protected void onPushDeliveryNotAcknowledged(String streamId, OIDSSFSecurityEvent event) {
		super.onPushDeliveryNotAcknowledged(streamId, event);
		if (!isWrongStateEvent(event.jti()) || wrongStateGraded) {
			return;
		}
		Integer status = env.getInteger("endpoint_response", "status");
		if (status != null && status >= 400) {
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition(
					"Receiver rejected the verification event whose state does not match (jti=" + event.jti() + ") with HTTP status " + status),
				Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1", "RFC8935-2.3");
		}
		// the rejection must be a 400 with a JSON body carrying 'err' and 'description'; the
		// code 'invalid_state' is only recommended for a state mismatch, hence the warning
		callAndContinueOnFailure(OIDSSFValidatePushDeliveryErrorResponse.class, Condition.ConditionResult.FAILURE, "RFC8935-2.3");
		callAndContinueOnFailure(new OIDSSFWarnPushDeliveryErrorCodeMismatch(OIDSSFWarnPushDeliveryErrorCodeNotRegistered.ERROR_CODE_INVALID_STATE),
			Condition.ConditionResult.WARNING, "OIDSSF-8.1.4.1", "RFC8935-2.4");
		wrongStateGraded = true;
	}

	@Override
	protected void onStreamEventAcknowledged(String streamId, String jti, OIDSSFSecurityEvent event) {
		if (isWrongStateEvent(jti)) {
			gradeWrongStateEventAccepted(jti, "acknowledged", "not acknowledge a mismatch", "RFC8936-2.4");
			return;
		}
		if (SsfEvents.isVerificationEvent(event.type())) {
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream Verification via POLL delivery for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
		}
	}

	/**
	 * Grades the receiver's acceptance (a 202 on push, an {@code ack} on poll) of the
	 * wrong-state event. A different value than the state the receiver sent is a plain
	 * mismatch and a FAILURE. A state the receiver never asked for is graded WARNING: the
	 * receiver has no expected value to confirm, and SSF 1.0 8.1.4.1 does not say whether an
	 * unrequested state is "not as expected".
	 */
	protected void gradeWrongStateEventAccepted(String jti, String howAccepted, String expectedReaction, String deliveryRequirement) {
		if (wrongStateGraded) {
			return;
		}
		if (receiverSentNoState) {
			callAndContinueOnFailure(new OIDSSFFindingCondition(
					"Receiver " + howAccepted + " a verification event carrying a state although its verification request carried none (jti=" + jti + "). "
						+ "Graded as a warning: the receiver had no expected state to confirm, and whether a state it never requested counts as a mismatch is not settled; "
						+ "a receiver that confirms the state is as expected would " + expectedReaction + "."),
				Condition.ConditionResult.WARNING, "OIDSSF-8.1.4.1", deliveryRequirement);
		} else {
			callAndContinueOnFailure(new OIDSSFFindingCondition(
					"Receiver " + howAccepted + " a verification event whose state does not match the state it sent (jti=" + jti + "). "
						+ "Receivers must confirm that the state of a verification event is the one they sent and " + expectedReaction + "."),
				Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1", deliveryRequirement);
		}
		wrongStateGraded = true;
	}

	@Override
	protected void onStreamEventErrorReported(String streamId, String jti, JsonObject error) {
		super.onStreamEventErrorReported(streamId, jti, error);
		if (!isWrongStateEvent(jti) || wrongStateGraded) {
			return;
		}
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition(
				"Receiver reported an error for the verification event whose state does not match (jti=" + jti + "): " + error),
			Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1", "RFC8936-2.4");
		String errorCode = OIDFJSON.tryGetString(error.get("err"));
		if (OIDSSFWarnPushDeliveryErrorCodeNotRegistered.ERROR_CODE_INVALID_STATE.equals(errorCode)) {
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("The reported 'err' is 'invalid_state'"), Condition.ConditionResult.WARNING, "OIDSSF-8.1.4.1");
		} else {
			callAndContinueOnFailure(new OIDSSFFindingCondition(
					"The 'err' reported for the verification event whose state does not match is '" + errorCode + "' rather than 'invalid_state'."),
				Condition.ConditionResult.WARNING, "OIDSSF-8.1.4.1", "RFC8936-2.4");
		}
		wrongStateGraded = true;
	}

	@Override
	protected void onEventsUndeliverable(String streamId, List<OIDSSFSecurityEvent> events) {
		super.onEventsUndeliverable(streamId, events);
		if (wrongStateGraded || events.stream().noneMatch(event -> isWrongStateEvent(event.jti()))) {
			return;
		}
		callAndContinueOnFailure(new OIDSSFFindingCondition(
				"The receiver deleted the stream without ever retrieving the verification event whose state does not match, so its handling could not be assessed. "
					+ "Keep the stream open and keep polling until the verification event was retrieved and rejected, then delete it."),
			Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
		wrongStateGraded = true;
	}

	@Override
	protected void onEventsUnresolvedAtDeletion(String streamId, List<OIDSSFSecurityEvent> events) {
		super.onEventsUnresolvedAtDeletion(streamId, events);
		if (wrongStateGraded || events.stream().noneMatch(event -> isWrongStateEvent(event.jti()))) {
			return;
		}
		callAndContinueOnFailure(new OIDSSFFindingCondition(
				"Receiver retrieved the verification event whose state does not match but neither acknowledged it nor reported it via 'setErrs' before deleting the stream. "
					+ "Not acknowledging is correct; reporting it via 'setErrs' with 'err' set to 'invalid_state' would make the rejection observable."),
			Condition.ConditionResult.INFO, "OIDSSF-8.1.4.1", "RFC8936-2.4");
		wrongStateGraded = true;
	}

	protected class ResolveSilentlyIgnoredVerificationEventTask implements Callable<String> {

		protected final String streamId;

		protected ResolveSilentlyIgnoredVerificationEventTask(String streamId) {
			this.streamId = streamId;
		}

		@Override
		public String call() throws Exception {
			OIDSSFSecurityEvent event = wrongStateEvent;
			if (wrongStateGraded || event == null) {
				return "done";
			}
			wrongStateGraded = true;

			// getUndeliveredEventJtis() covers the case where the receiver deleted the stream
			// with the event still queued (the deletion purged the event store)
			Set<String> stillQueuedJtis = new HashSet<>(getUndeliveredEventJtis());
			eventStore.getQueuedEvents(streamId).forEach(queued -> stillQueuedJtis.add(queued.jti()));

			if (stillQueuedJtis.contains(event.jti())) {
				// graded when the stream is deleted, see onEventsUndeliverable
				wrongStateGraded = false;
				eventLog.log(getName(), args("msg", "Receiver has not retrieved the verification event whose state does not match within "
						+ SILENT_DROP_RESOLUTION_TIMEOUT_SECONDS + " seconds; its handling is assessed once retrieved, and a stream deletion before that fails the test",
					"jti", event.jti()));
				return "done";
			}
			callAndContinueOnFailure(new OIDSSFFindingCondition(
					"Receiver retrieved the verification event whose state does not match (jti=" + event.jti() + ") but neither acknowledged it "
						+ "nor reported it via 'setErrs' within " + SILENT_DROP_RESOLUTION_TIMEOUT_SECONDS + " seconds. Not acknowledging is correct; "
						+ "reporting it via 'setErrs' with 'err' set to 'invalid_state' would make the rejection observable."),
				Condition.ConditionResult.INFO, "OIDSSF-8.1.4.1", "RFC8936-2.4");
			return "done";
		}
	}

	@Override
	public void fireTestFinished() {
		eventLog.log(getName(), "Detected the receiver's handling of the verification event whose state does not match.");
		super.fireTestFinished();
	}
}
