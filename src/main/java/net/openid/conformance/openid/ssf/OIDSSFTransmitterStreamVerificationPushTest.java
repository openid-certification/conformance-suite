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
		the solicited response are accepted per SSF 1.0 §8.1.4-2; the test succeeds
		once a verification event with matching 'state' is delivered.
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

	@Override
	protected void performVerification() {
		int unsolicitedSeen = 0;

		while (unsolicitedSeen < MAX_UNSOLICITED_EVENTS) {
			// Wait for push OUTSIDE runBlock to avoid eventLog monitor deadlock
			SSfPushRequest pushRequest = lookupNextPushRequest();
			if (pushRequest == null) {
				throw new TestFailureException(getId(),
					"Did not receive a solicited verification event (with 'state') via PUSH delivery"
						+ (unsolicitedSeen > 0
							? " after " + unsolicitedSeen + " transmitter-initiated event(s)"
							: ""));
			}

			AtomicBoolean wasSolicited = new AtomicBoolean(false);
			String blockTitle = unsolicitedSeen == 0
				? "Verify verification event received via PUSH delivery"
				: "Verify verification event received via PUSH delivery (after " + unsolicitedSeen + " transmitter-initiated verification)";
			eventLog.runBlock(blockTitle, () -> {
				callAndStopOnFailure(OIDSSFExtractVerificationEventFromPushRequest.class, "OIDSSF-8.1.4.1");
				parseVerificationEventInResponse();
				verifyParsedVerificationEventCommon();

				callAndContinueOnFailure(OIDSSFEnsureUnsolicitedVerificationEventHasNoState.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.2");

				if (currentVerificationEventHasState()) {
					callAndContinueOnFailure(OIDSSFCheckVerificationEventState.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
					wasSolicited.set(true);
				} else {
					callAndContinueOnFailure(OIDSSFLogAcceptedUnsolicitedVerificationEvent.class, Condition.ConditionResult.INFO, "OIDSSF-8.1.4");
				}
			});

			if (wasSolicited.get()) {
				return;
			}

			unsolicitedSeen++;
		}

		throw new TestFailureException(getId(),
			"Received " + unsolicitedSeen + " transmitter-initiated verification events without a solicited one — "
				+ "transmitter never echoed the state from the verification request");
	}
}
