package net.openid.conformance.openid.ssf;

import net.openid.conformance.openid.ssf.conditions.events.OIDSSFCallPollEndpoint;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFExtractReceivedSETs;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "openid-ssf-transmitter-stream-verification-poll-and-ack",
	displayName = "OpenID Shared Signals Framework: Stream Verification via POLL_AND_ACKNOWLEDGE",
	summary = """
		This test verifies stream verification via the POLL_AND_ACKNOWLEDGE poll mode.
		The testsuite expects to observe the following interactions:
		 * create a stream
		 * trigger a first verification event and retrieve it via POLL_ONLY
		 * trigger a second verification event
		 * acknowledge the first event and retrieve the second via POLL_AND_ACKNOWLEDGE
		 * validate the solicited second verification event

		Transmitter-initiated verification events (without 'state') in either poll
		response are accepted per SSF 1.0 §8.1.4-2.
		""",
	profile = "OIDSSF"
)
@VariantNotApplicable(parameter = SsfDeliveryMode.class, values = "push")
public class OIDSSFTransmitterStreamVerificationPollAndAckTest extends AbstractOIDSSFTransmitterStreamVerificationTest {

	@Override
	protected void performVerification() {
		// Step 1: the base class already triggered the first verification event.
		// Poll to retrieve it — this gives us SETs to acknowledge in step 3. We don't
		// require the solicited-state match here; any events returned (including
		// unsolicited stateless ones) are fine as ack targets. The solicited state
		// validation runs in step 3 on the second poll.
		eventLog.runBlock("Retrieve first verification event batch via POLL_ONLY", () -> {
			env.putString("ssf", "poll.mode", OIDSSFCallPollEndpoint.PollMode.POLL_ONLY.name());
			callAndStopOnFailure(OIDSSFCallPollEndpoint.class, "OIDSSF-8.1.4.1", "RFC8936-2.4");
			env.mapKey("ssf_polling_response", "resource_endpoint_response_full");
			callAndStopOnFailure(OIDSSFExtractReceivedSETs.class);
		});

		// Step 2: trigger a second verification event so there is something new to retrieve.
		triggerVerificationEvent();

		// Step 3: POLL_AND_ACKNOWLEDGE — ack the first event, retrieve the second.
		eventLog.runBlock("Acknowledge first and retrieve second verification event batch via POLL_AND_ACKNOWLEDGE", () -> {
			env.putString("ssf", "poll.mode", OIDSSFCallPollEndpoint.PollMode.POLL_AND_ACKNOWLEDGE.name());
			callAndStopOnFailure(OIDSSFCallPollEndpoint.class, "OIDSSF-8.1.4.1", "RFC8936-2.4");
			env.mapKey("ssf_polling_response", "resource_endpoint_response_full");
			callAndStopOnFailure(OIDSSFExtractReceivedSETs.class);
		});

		if (!iterateAndValidateVerificationEventsInPollResponse("POLL_AND_ACKNOWLEDGE")) {
			throw new TestFailureException(getId(),
				"Second poll response did not contain a solicited verification event (with matching 'state')");
		}
	}
}
