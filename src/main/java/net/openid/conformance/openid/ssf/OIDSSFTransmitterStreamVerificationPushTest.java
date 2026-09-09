package net.openid.conformance.openid.ssf;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFCheckVerificationEventState;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureUnsolicitedVerificationEventHasNoState;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFExtractVerificationEventFromPushRequest;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFLogAcceptedUnsolicitedVerificationEvent;
import net.openid.conformance.openid.ssf.delivery.SSfPushRequest;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;

import java.util.concurrent.atomic.AtomicBoolean;

@PublishTestModule(
	testName = "openid-ssf-transmitter-stream-verification-push",
	displayName = "OpenID Shared Signals Framework: Stream Verification via PUSH delivery",
	summary = """
		This test verifies stream verification via PUSH delivery.
		The testsuite expects to observe the following interactions:
		 * create a stream with a push delivery endpoint
		 * trigger a verification event
		 * receive the verification event via PUSH delivery
		 * validate the verification event

		Transmitter-initiated verification events (without 'state') that arrive before
		the solicited response are accepted per SSF 1.0 §8.1.4-2, and other SETs the
		transmitter pushes in the meantime (SSF 1.0 §8.1.4.2 does not require the
		verification event to be delivered first) are parsed and skipped; the test
		succeeds once a verification event with matching 'state' is delivered.
		""",
	profile = "OIDSSF"
)
@VariantNotApplicable(parameter = SsfDeliveryMode.class, values = "poll")
public class OIDSSFTransmitterStreamVerificationPushTest extends AbstractOIDSSFTransmitterStreamVerificationTest {

	/**
	 * Maximum number of unsolicited (stateless) verification events to tolerate before
	 * giving up on receiving the solicited one. Per SSF 1.0 §8.1.4-2, a transmitter MAY
	 * deliver any number of unsolicited verification events, so we don't want an
	 * unbounded loop — but we also must not fail on the first unsolicited event we see.
	 */
	private static final int MAX_UNSOLICITED_EVENTS = 10;

	/**
	 * Maximum number of SETs that are not verification events (CAEP events, stream-updated,
	 * ...) to skip while waiting for the solicited verification event. SSF 1.0 §8.1.4.2 lets
	 * the transmitter deliver the verification event in any order relative to the rest of
	 * the stream, so such SETs are legitimate here; the bound only keeps a flooding
	 * transmitter from holding the test forever.
	 */
	private static final int MAX_NON_VERIFICATION_EVENTS = 50;

	@Override
	protected void performVerification() {
		int unsolicitedSeen = 0;
		int nonVerificationSeen = 0;

		while (unsolicitedSeen < MAX_UNSOLICITED_EVENTS && nonVerificationSeen < MAX_NON_VERIFICATION_EVENTS) {
			// Wait for push OUTSIDE runBlock to avoid eventLog monitor deadlock
			SSfPushRequest pushRequest = lookupNextPushRequest();
			if (pushRequest == null) {
				throw new TestFailureException(getId(),
					"Did not receive a solicited verification event (with 'state') via PUSH delivery"
						+ (unsolicitedSeen > 0
							? " after " + unsolicitedSeen + " transmitter-initiated event(s)"
							: "")
						+ (nonVerificationSeen > 0
							? " and " + nonVerificationSeen + " other SET(s)"
							: ""));
			}

			AtomicBoolean wasSolicited = new AtomicBoolean(false);
			AtomicBoolean wasVerificationEvent = new AtomicBoolean(false);
			String blockTitle = unsolicitedSeen == 0
				? "Verify verification event received via PUSH delivery"
				: "Verify verification event received via PUSH delivery (after " + unsolicitedSeen + " transmitter-initiated verification)";
			eventLog.runBlock(blockTitle, () -> {
				callAndStopOnFailure(OIDSSFExtractVerificationEventFromPushRequest.class, "OIDSSF-8.1.4.1");
				parseVerificationEventInResponse();

				if (!currentEventIsVerificationEvent()) {
					// SSF 1.0 §8.1.4.2: the verification event need not be delivered before
					// other queued SETs, so a CAEP or stream-updated event arriving here is
					// not a verification event and must not be validated as one.
					eventLog.log(getName(),
						args("msg", "Skipping non-verification SET pushed while waiting for the verification event",
							"push_request_id", pushRequest.id()));
					return;
				}
				wasVerificationEvent.set(true);

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
				return;
			}

			if (wasVerificationEvent.get()) {
				unsolicitedSeen++;
			} else {
				nonVerificationSeen++;
			}
		}

		if (unsolicitedSeen >= MAX_UNSOLICITED_EVENTS) {
			throw new TestFailureException(getId(),
				"Received " + unsolicitedSeen + " transmitter-initiated verification events without a solicited one — "
					+ "transmitter never echoed the state from the verification request");
		}
		throw new TestFailureException(getId(),
			"Received " + nonVerificationSeen + " SETs via PUSH delivery, none of them the solicited verification event — "
				+ "transmitter never echoed the state from the verification request");
	}
}
