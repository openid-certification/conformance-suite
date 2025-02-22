package net.openid.conformance.openid.ssf;

import net.openid.conformance.openid.ssf.conditions.OIDSSFPushPendingSecurityEvents;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFGenerateCaepEvent;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFUseValidSubject;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFWaitForSetAcknowledgment;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFWaitForStreamReadiness;
import net.openid.conformance.openid.ssf.mock.OIDSSFGenerateServerJWKs;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-ssf-receiver-happypath",
	displayName = "OpenID Shared Signals Framework: Validate Receiver Handling",
	summary = "This test verifies the receiver stream management and event delivery. The test generates a dynamic transmitter and waits for a receiver to register a stream. Then the testsuite will generate some supported events and deliver it to the receiver via the configured delivery mechanism. The testsuite will then wait for acknowledgement of those events.",
	profile = "OIDSSF",
	configurationFields = {
		"ssf.transmitter.access_token",
		"ssf.stream.audience",
		"ssf.subjects.valid",
		"ssf.subjects.invalid"
	}
)
public class OIDSSFReceiverHappyPathTest extends AbstractOIDSSFReceiverTestModule {

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		eventLog.runBlock("Generate Transmitter Metadata", () -> {
			callAndStopOnFailure(OIDSSFGenerateServerJWKs.class);
		});

		eventLog.runBlock("Setup subjects", () -> {
			callAndStopOnFailure(OIDSSFUseValidSubject.class);
		});

		eventLog.runBlock("Wait for receiver stream creation", () -> {
			callAndStopOnFailure(OIDSSFWaitForStreamReadiness.class);
		});

		eventLog.runBlock("Emit CAEP event: Session Revoked", () -> {
			callAndStopOnFailure(new OIDSSFGenerateCaepEvent(OIDSSFGenerateCaepEvent.CAEP_SESSION_REVOKED));

			SsfDeliveryMode variant = getVariant(SsfDeliveryMode.class);
			switch(variant) {
				case PUSH -> {
					callAndStopOnFailure(OIDSSFPushPendingSecurityEvents.class);
				}
				case POLL -> {

				}
			}
			callAndStopOnFailure(OIDSSFWaitForSetAcknowledgment.class);
		});

		eventLog.runBlock("Emit CAEP event: Credentials Changed", () -> {
			callAndStopOnFailure(new OIDSSFGenerateCaepEvent(OIDSSFGenerateCaepEvent.CAEP_CREDENTIALS_CHANGED));

			SsfDeliveryMode variant = getVariant(SsfDeliveryMode.class);
			switch(variant) {
				case PUSH -> {
					callAndStopOnFailure(OIDSSFPushPendingSecurityEvents.class);
				}
				case POLL -> {

				}
			}

			callAndStopOnFailure(OIDSSFWaitForSetAcknowledgment.class);
		});

//		eventLog.runBlock("Wait for receiver interaction", () -> {
//			eventLog.log(getName(), "Waiting for receiver 1");
//			callAndStopOnFailure(WaitFor30Seconds.class);
//
//			eventLog.log(getName(), "Waiting for receiver 2");
//			callAndStopOnFailure(WaitFor30Seconds.class);
//
//			eventLog.log(getName(), "Waiting for receiver 3");
//			callAndStopOnFailure(WaitFor30Seconds.class);
//
//		});

		fireTestFinished();
	}
}
