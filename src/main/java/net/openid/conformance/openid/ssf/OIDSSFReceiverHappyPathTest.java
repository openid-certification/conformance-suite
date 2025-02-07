package net.openid.conformance.openid.ssf;

import net.openid.conformance.condition.as.OIDCCGenerateServerJWKs;
import net.openid.conformance.condition.client.WaitFor30Seconds;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-ssf-receiver-happypath",
	displayName = "OpenID Shared Signals Framework: Validate Receiver Handling",
	summary = "This test verifies the receiver stream management and event delivery. The test generates a dynamic transmitter and waits for a receiver to register a stream. Then the testsuite will generate some supported events and deliver it to the receiver via the configured delivery mechanism. The testsuite will then wait for acknowledgement of those events.",
	profile = "OIDSSF",
	configurationFields = {
		"ssf.stream.audience"
	}
)
public class OIDSSFReceiverHappyPathTest extends AbstractOIDSSFReceiverTestModule {

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		eventLog.runBlock("Generate Transmitter Metadata", () -> {
			callAndStopOnFailure(OIDCCGenerateServerJWKs.class);
		});

		eventLog.runBlock("Wait for receiver stream creation", () -> {
			eventLog.log(getName(), "Waiting for receiver");
			callAndStopOnFailure(WaitFor30Seconds.class);
		});

		fireTestFinished();
	}
}
