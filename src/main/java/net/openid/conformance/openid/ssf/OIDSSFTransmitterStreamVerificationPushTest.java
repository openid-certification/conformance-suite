package net.openid.conformance.openid.ssf;

import net.openid.conformance.openid.ssf.conditions.events.OIDSSFExtractVerificationEventFromPushRequest;
import net.openid.conformance.openid.ssf.delivery.SSfPushRequest;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "openid-ssf-transmitter-stream-verification-push",
	displayName = "OpenID Shared Signals Framework: Stream Verification via PUSH delivery",
	summary = """
		This test triggers a verification event and awaits the SET delivered \
		to the exposed push endpoint. \
		The test succeeds if the verification event is successfully received and validated.""",
	profile = "OIDSSF"
)
@VariantNotApplicable(parameter = SsfDeliveryMode.class, values = "poll")
public class OIDSSFTransmitterStreamVerificationPushTest extends AbstractOIDSSFTransmitterStreamVerificationTest {

	@Override
	protected void performVerification() {
		// Wait for push OUTSIDE runBlock to avoid eventLog monitor deadlock
		SSfPushRequest pushRequest = lookupNextPushRequest();
		if (pushRequest == null) {
			throw new TestFailureException(getId(), "Did not receive push delivery of verification event");
		}

		eventLog.runBlock("Verify verification event received via PUSH delivery", () -> {
			callAndStopOnFailure(OIDSSFExtractVerificationEventFromPushRequest.class, "OIDSSF-8.1.4.1");

			verifySetInResponse();
		});
	}
}
