package net.openid.conformance.openid.ssf;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.condition.client.WaitFor5Seconds;
import net.openid.conformance.openid.ssf.SsfConstants.StreamStatus;
import net.openid.conformance.openid.ssf.conditions.OIDSSFFindingCondition;
import net.openid.conformance.openid.ssf.conditions.OIDSSFLogSuccessCondition;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFCallPollEndpoint;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFCheckVerificationEventState;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureNoSetPushedWhileStreamPaused;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureNoSetsReturnedWhileStreamPaused;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureUnsolicitedVerificationEventHasNoState;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFExtractVerificationEventFromPushRequest;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFLogAcceptedUnsolicitedVerificationEvent;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFEnsureStreamStatusIs;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFReadStreamStatusCall;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFUpdateStreamStatusCall;
import net.openid.conformance.openid.ssf.delivery.SSfPushRequest;
import net.openid.conformance.openid.ssf.variant.SsfProfile;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

@PublishTestModule(
	testName = "openid-ssf-transmitter-paused-stream-holds-events",
	displayName = "OpenID Shared Signals Framework: A paused stream transmits no events and holds them until it is enabled again",
	summary = """
		This test verifies the transmitter's handling of the 'paused' stream status. SSF 1.0 8.1.2.1,
		status paused: "The Transmitter MUST NOT transmit events over the stream. The Transmitter SHOULD
		hold any events it would have transmitted while paused, and SHOULD transmit them when the
		stream's status becomes enabled". The specification does not exempt verification events from
		this rule, so the verification event requested while the stream is paused is used as the
		held event.
		The testsuite expects to observe the following interactions:
		 * create a stream and read its status, which must be 'enabled'
		 * update the stream status to 'paused'
		 * trigger a verification event
		 * observe the paused stream for about 20 seconds: no SET may be pushed to the receiver or
		   returned by the poll endpoint (a failure)
		 * update the stream status to 'enabled'
		 * receive the held verification event within about 60 seconds and validate it (a warning
		   if it does not arrive, since holding and transmitting held events are SHOULDs)
		 * delete the stream

		Updating the stream status is not required by the CAEP Interop Profile (2.3.5 only requires
		reading the status); a transmitter whose metadata has no status_endpoint skips this test
		under the default profile and fails it under the CAEP Interop Profile.
		""",
	profile = "OIDSSF"
)
public class OIDSSFTransmitterPausedStreamHoldsEventsTest extends AbstractOIDSSFTransmitterStreamVerificationTest {

	/**
	 * How long the paused stream is observed for deliveries after the verification event was
	 * triggered. Long enough for a transmitter that ignores the pause to deliver the event.
	 */
	protected static final Duration PAUSED_OBSERVATION_WINDOW = Duration.ofSeconds(20);

	protected static final int PAUSED_OBSERVATION_STEP_SECONDS = 5;

	/**
	 * How long to wait for the held verification event after the stream was enabled again;
	 * matches the window the poll based verification tests grant a transmitter.
	 */
	protected static final Duration HELD_EVENT_WAIT_WINDOW = Duration.ofSeconds(60);

	protected static final int HELD_EVENT_WAIT_STEP_SECONDS = 10;

	/**
	 * The moment the transmitter acknowledged the pause; pushes received earlier were sent while
	 * the stream was still enabled.
	 */
	protected volatile Instant pausedAt;

	/** The status the stream is put into before the verification event is requested. */
	protected StreamStatus stoppedStatus() {
		return StreamStatus.paused;
	}

	/** Requests a verification event without touching the stream status. */
	protected void triggerPlainVerificationEvent() {
		super.triggerVerificationEvent();
	}

	@Override
	protected void triggerVerificationEvent() {

		String statusEndpoint = env.getString("ssf", "transmitter_metadata.status_endpoint");
		if (statusEndpoint == null) {
			if (isSsfProfileEnabled(SsfProfile.CAEP_INTEROP)) {
				throw new TestFailureException(getId(), "Transmitter metadata does not include a status_endpoint, "
					+ "which is required by the CAEP Interop Profile (CAEPIOP-2.3.5).");
			}
			fireTestSkipped("Transmitter metadata does not include a status_endpoint, so the stream cannot be paused. "
				+ "The SSF specification defines status_endpoint as optional (OIDSSF-7.1.1).");
		}

		eventLog.runBlock("Read Stream Status", () -> {
			callAndStopOnFailure(OIDSSFReadStreamStatusCall.class, "OIDSSF-8.1.2.1", "CAEPIOP-2.3.5");
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndStopOnFailure(EnsureHttpStatusCodeIs200.class, "OIDSSF-8.1.2.1");
			call(exec().unmapKey("endpoint_response"));
			// a stream that does not start out enabled cannot be paused meaningfully
			callAndStopOnFailure(new OIDSSFEnsureStreamStatusIs(StreamStatus.enabled), "OIDSSF-8.1.2.1");
		});

		StreamStatus stoppedStatus = stoppedStatus();
		eventLog.runBlock("Update Stream Status to '" + stoppedStatus + "'", () -> {
			callAndStopOnFailure(new OIDSSFUpdateStreamStatusCall(stoppedStatus), "OIDSSF-8.1.2.2");
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndStopOnFailure(EnsureHttpStatusCodeIs200.class, "OIDSSF-8.1.2.2");
			call(exec().unmapKey("endpoint_response"));
			callAndContinueOnFailure(new OIDSSFEnsureStreamStatusIs(stoppedStatus), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.2.2");

			// pushes that reached the receiver before this instant were sent while the stream was still enabled
			pausedAt = Instant.now();
			env.putString("ssf", "stream_paused_at", pausedAt.toString());
			env.putString("ssf", "stream_stopped_status", stoppedStatus.name());
		});

		triggerPlainVerificationEvent();
	}

	@Override
	protected void performVerification() {

		observeStoppedStream();

		enableStream();

		boolean heldEventReceived = switch (deliveryMode) {
			case PUSH -> awaitHeldVerificationEventViaPush();
			case POLL -> pollForSolicitedVerificationEvent("POLL_ONLY after enabling the stream", OIDSSFCallPollEndpoint.PollMode.POLL_ONLY);
		};

		eventLog.runBlock("Held events after the stream was enabled again", () -> {
			if (heldEventReceived) {
				callAndContinueOnFailure(new OIDSSFLogSuccessCondition("The transmitter held the verification event while the stream was paused "
					+ "and transmitted it once the stream was enabled again"), Condition.ConditionResult.INFO, "OIDSSF-8.1.2.1");
			} else {
				callAndContinueOnFailure(new OIDSSFFindingCondition("The verification event requested while the stream was paused was not "
					+ "delivered within " + HELD_EVENT_WAIT_WINDOW.toSeconds() + " seconds after the stream was enabled again. SSF 1.0 8.1.2.1 says "
					+ "the transmitter SHOULD hold events while the stream is paused and SHOULD transmit them when the stream becomes enabled."),
					Condition.ConditionResult.WARNING, "OIDSSF-8.1.2.1");
			}
		});
	}

	/** Observes the stopped stream for deliveries over the scheduled delivery method. */
	protected void observeStoppedStream() {
		eventLog.log(getName(), "Observing the " + stoppedStatus() + " stream for " + PAUSED_OBSERVATION_WINDOW.toSeconds()
			+ " seconds: the transmitter must not deliver any SET while the stream is " + stoppedStatus() + " (SSF 1.0 8.1.2.1)");

		switch (deliveryMode) {
			case PUSH:
				observePausedStreamViaPush();
				break;
			case POLL:
				observePausedStreamViaPoll();
				break;
			default:
				break;
		}
	}

	protected void enableStream() {
		eventLog.runBlock("Update Stream Status to 'enabled'", () -> {
			callAndStopOnFailure(new OIDSSFUpdateStreamStatusCall(StreamStatus.enabled), "OIDSSF-8.1.2.2");
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndStopOnFailure(EnsureHttpStatusCodeIs200.class, "OIDSSF-8.1.2.2");
			call(exec().unmapKey("endpoint_response"));
			callAndContinueOnFailure(new OIDSSFEnsureStreamStatusIs(StreamStatus.enabled), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.2.2");
		});
	}

	/**
	 * Waits for push requests for the observation window. Waiting happens outside a runBlock:
	 * runBlock is synchronized on the event log and would block the HTTP handler thread that
	 * records incoming pushes.
	 */
	protected void observePausedStreamViaPush() {
		Instant deadline = Instant.now().plus(PAUSED_OBSERVATION_WINDOW);
		int pushesWhilePaused = 0;
		while (Instant.now().isBefore(deadline)) {
			SSfPushRequest pushRequest = lookupNextPushRequest(PAUSED_OBSERVATION_STEP_SECONDS);
			if (pushRequest == null) {
				continue;
			}
			if (!pushRequest.receivedAt().isBefore(pausedAt)) {
				pushesWhilePaused++;
			}
			// the condition accepts pushes that reached the receiver before the pause was acknowledged
			eventLog.runBlock("Push request received while the stream is " + stoppedStatus(), () ->
				callAndContinueOnFailure(OIDSSFEnsureNoSetPushedWhileStreamPaused.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.2.1"));
		}
		if (pushesWhilePaused == 0) {
			eventLog.runBlock("Stopped stream observed via PUSH", () ->
				callAndContinueOnFailure(new OIDSSFLogSuccessCondition("No SET was pushed while the stream was " + stoppedStatus()),
					Condition.ConditionResult.INFO, "OIDSSF-8.1.2.1"));
		}
	}

	protected void observePausedStreamViaPoll() {
		int attempts = (int) (PAUSED_OBSERVATION_WINDOW.toSeconds() / PAUSED_OBSERVATION_STEP_SECONDS);
		for (int attempt = 1; attempt <= attempts; attempt++) {
			int currentAttempt = attempt;
			eventLog.runBlock("Poll the " + stoppedStatus() + " stream (attempt " + currentAttempt + "/" + attempts + ")", () -> {
				env.putString("ssf", "poll.mode", OIDSSFCallPollEndpoint.PollMode.POLL_ONLY.name());
				callAndStopOnFailure(OIDSSFCallPollEndpoint.class, "OIDSSF-8.1.2.1", "RFC8936-2.4");
				call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
				callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.FAILURE, "RFC8936-2.5");
				call(exec().unmapKey("endpoint_response"));
				env.mapKey("ssf_polling_response", "resource_endpoint_response_full");
				callAndContinueOnFailure(OIDSSFEnsureNoSetsReturnedWhileStreamPaused.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.2.1");

				if (currentAttempt < attempts) {
					callAndContinueOnFailure(WaitFor5Seconds.class, Condition.ConditionResult.INFO);
				}
			});
		}
	}

	/**
	 * Waits for the held verification event via push after the stream was enabled again.
	 * Non-verification SETs and transmitter-initiated verification events (SSF 1.0 8.1.4) are
	 * validated as far as applicable and skipped.
	 *
	 * @return {@code true} once the verification event echoing the state of this test's
	 *         verification request was received and validated
	 */
	protected boolean awaitHeldVerificationEventViaPush() {
		Instant deadline = Instant.now().plus(HELD_EVENT_WAIT_WINDOW);
		while (Instant.now().isBefore(deadline)) {
			// wait outside runBlock, see observePausedStreamViaPush
			SSfPushRequest pushRequest = lookupNextPushRequest(HELD_EVENT_WAIT_STEP_SECONDS);
			if (pushRequest == null) {
				eventLog.log(getName(), "Waiting for the held verification event to be pushed after the stream was enabled again ("
					+ Duration.between(Instant.now(), deadline).toSeconds() + "s left)");
				continue;
			}

			AtomicBoolean wasSolicited = new AtomicBoolean(false);
			eventLog.runBlock("Validate SET pushed after the stream was enabled again", () -> {
				callAndStopOnFailure(OIDSSFExtractVerificationEventFromPushRequest.class, "OIDSSF-8.1.4.1");
				parseVerificationEventInResponse();

				if (!currentEventIsVerificationEvent()) {
					eventLog.log(getName(),
						args("msg", "Validating the envelope of a non-verification SET pushed while waiting for the held verification event",
							"push_request_id", pushRequest.id()));
					// its SET envelope (and a stream-updated event's payload) is still validated
					verifyParsedNonVerificationSet();
					return;
				}

				verifyParsedVerificationEventCommon();

				callAndContinueOnFailure(OIDSSFEnsureUnsolicitedVerificationEventHasNoState.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.2");

				if (currentVerificationEventHasState()) {
					callAndContinueOnFailure(OIDSSFCheckVerificationEventState.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
					wasSolicited.set(currentVerificationEventIsForLatestRequest());
				} else {
					callAndContinueOnFailure(OIDSSFLogAcceptedUnsolicitedVerificationEvent.class, Condition.ConditionResult.INFO, "OIDSSF-8.1.4");
				}
			});

			if (wasSolicited.get()) {
				return true;
			}
		}
		return false;
	}
}
