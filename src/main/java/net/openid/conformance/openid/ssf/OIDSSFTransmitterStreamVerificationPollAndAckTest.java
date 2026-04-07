package net.openid.conformance.openid.ssf;

import net.openid.conformance.openid.ssf.conditions.events.OIDSSFCallPollEndpoint;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFExtractReceivedSETs;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFExtractVerificationEventFromReceivedSETs;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "openid-ssf-transmitter-stream-verification-poll-and-ack",
	displayName = "OpenID Shared Signals Framework: Stream Verification via POLL_AND_ACKNOWLEDGE",
	summary = """
		This test verifies the POLL_AND_ACKNOWLEDGE poll mode by triggering two verification events. \
		The first event is retrieved via POLL_ONLY to obtain SETs for acknowledgment. \
		A second verification event is then triggered, and POLL_AND_ACKNOWLEDGE is used to \
		simultaneously acknowledge the first event and retrieve the second. \
		The test succeeds if the second verification event is successfully retrieved and validated.""",
	profile = "OIDSSF"
)
@VariantNotApplicable(parameter = SsfDeliveryMode.class, values = "push")
public class OIDSSFTransmitterStreamVerificationPollAndAckTest extends AbstractOIDSSFTransmitterStreamVerificationTest {

	@Override
	protected void performVerification() {
		// Step 1: the base class already triggered the first verification event.
		// Poll to retrieve it — this gives us SETs to acknowledge in step 3.
		eventLog.runBlock("Retrieve first verification event via POLL_ONLY", () -> {
			env.putString("ssf", "poll.mode", OIDSSFCallPollEndpoint.PollMode.POLL_ONLY.name());
			callAndStopOnFailure(OIDSSFCallPollEndpoint.class, "OIDSSF-8.1.4.1", "RFC8936-2.4");
			env.mapKey("ssf_polling_response", "resource_endpoint_response_full");
			callAndStopOnFailure(OIDSSFExtractReceivedSETs.class);
		});

		// Step 2: trigger a second verification event so there is something new to retrieve.
		triggerVerificationEvent();

		// Step 3: POLL_AND_ACKNOWLEDGE — ack the first event, retrieve the second.
		eventLog.runBlock("Acknowledge first and retrieve second verification event via POLL_AND_ACKNOWLEDGE", () -> {
			env.putString("ssf", "poll.mode", OIDSSFCallPollEndpoint.PollMode.POLL_AND_ACKNOWLEDGE.name());
			callAndStopOnFailure(OIDSSFCallPollEndpoint.class, "OIDSSF-8.1.4.1", "RFC8936-2.4");
			env.mapKey("ssf_polling_response", "resource_endpoint_response_full");
			callAndStopOnFailure(OIDSSFExtractReceivedSETs.class);
			callAndStopOnFailure(OIDSSFExtractVerificationEventFromReceivedSETs.class);

			verifySetInResponse();
		});
	}
}
