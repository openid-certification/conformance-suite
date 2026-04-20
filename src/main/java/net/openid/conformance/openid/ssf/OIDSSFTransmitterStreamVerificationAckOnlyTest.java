package net.openid.conformance.openid.ssf;

import net.openid.conformance.openid.ssf.conditions.events.OIDSSFCallPollEndpoint;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFExtractReceivedSETs;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "openid-ssf-transmitter-stream-verification-ack-only",
	displayName = "OpenID Shared Signals Framework: Stream Verification via ACKNOWLEDGE_ONLY",
	summary = """
		This test triggers a verification event, retrieves it via POLL_ONLY, \
		then acknowledges it using ACKNOWLEDGE_ONLY mode (acknowledge without retrieving new events). \
		Transmitter-initiated verification events (without 'state') are accepted per SSF 1.0 8.1.4-2; \
		the test succeeds once a verification event carrying the expected 'state' is found and \
		validated, and the subsequent acknowledgment request succeeds.""",
	profile = "OIDSSF"
)
@VariantNotApplicable(parameter = SsfDeliveryMode.class, values = "push")
public class OIDSSFTransmitterStreamVerificationAckOnlyTest extends AbstractOIDSSFTransmitterStreamVerificationTest {

	@Override
	protected void performVerification() {
		eventLog.runBlock("Poll for verification events via POLL_ONLY", () -> {
			env.putString("ssf", "poll.mode", OIDSSFCallPollEndpoint.PollMode.POLL_ONLY.name());
			callAndStopOnFailure(OIDSSFCallPollEndpoint.class, "OIDSSF-8.1.4.1", "RFC8936-2.4");
			env.mapKey("ssf_polling_response", "resource_endpoint_response_full");
			callAndStopOnFailure(OIDSSFExtractReceivedSETs.class);
		});

		if (!iterateAndValidateVerificationEventsInPollResponse("POLL_ONLY")) {
			throw new TestFailureException(getId(),
				"Poll response did not contain a solicited verification event (with matching 'state')");
		}

		eventLog.runBlock("Acknowledge verification event via ACKNOWLEDGE_ONLY", () -> {
			env.putString("ssf", "poll.mode", OIDSSFCallPollEndpoint.PollMode.ACKNOWLEDGE_ONLY.name());
			callAndStopOnFailure(OIDSSFCallPollEndpoint.class, "OIDSSF-8.1.4.1", "RFC8936-2.4");
		});
	}
}
