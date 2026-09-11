package net.openid.conformance.openid.ssf;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openid.ssf.SsfConstants.StreamStatus;
import net.openid.conformance.openid.ssf.conditions.OIDSSFFindingCondition;
import net.openid.conformance.openid.ssf.conditions.OIDSSFLogSuccessCondition;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFCallPollEndpoint;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-ssf-transmitter-disabled-stream-drops-events",
	displayName = "OpenID Shared Signals Framework: A disabled stream transmits no events and transmits again once it is enabled",
	summary = """
		This test verifies the transmitter's handling of the 'disabled' stream status. SSF 1.0 8.1.2.1,
		status disabled: "The Transmitter MUST NOT transmit events over the stream and will not hold any
		events for later transmission". A verification event requested while the stream is disabled is
		used as the event that must not be transmitted.
		The testsuite expects to observe the following interactions:
		 * create a stream and read its status, which must be 'enabled'
		 * update the stream status to 'disabled'
		 * trigger a verification event
		 * observe the disabled stream for about 20 seconds: no SET may be pushed to the receiver or
		   returned by the poll endpoint (a failure)
		 * update the stream status to 'enabled'; whether the earlier verification event is delivered now
		   is only logged, as a disabled stream holds nothing but nothing forbids delivering it either
		 * trigger a verification event again and receive it within about 60 seconds (a failure if it
		   does not arrive: the enabled stream must transmit events again)
		 * delete the stream

		Updating the stream status is not required by the CAEP Interop Profile (2.3.5 only requires
		reading the status); a transmitter whose metadata has no status_endpoint skips this test
		under the default profile and fails it under the CAEP Interop Profile.
		""",
	profile = "OIDSSF"
)
public class OIDSSFTransmitterDisabledStreamDropsEventsTest extends OIDSSFTransmitterPausedStreamHoldsEventsTest {

	@Override
	protected StreamStatus stoppedStatus() {
		return StreamStatus.disabled;
	}

	@Override
	protected void performVerification() {

		observeStoppedStream();

		enableStream();

		// The verification event requested while disabled is not expected any more (SSF 1.0
		// 8.1.2.1: a disabled stream holds nothing); should it still turn up, the wait below
		// accepts it as a late echo of an earlier request and goes on waiting for the new one.
		eventLog.runBlock("Trigger verification event on the enabled stream", () -> {
			triggerVerificationEventAndRequireAcceptance();
			call(exec().unmapKey("endpoint_response"));
		});

		boolean verificationReceived = switch (deliveryMode) {
			case PUSH -> awaitHeldVerificationEventViaPush();
			case POLL -> pollForSolicitedVerificationEvent("POLL_ONLY after enabling the stream", OIDSSFCallPollEndpoint.PollMode.POLL_ONLY);
		};

		eventLog.runBlock("Events after the stream was enabled again", () -> {
			if (verificationReceived) {
				callAndContinueOnFailure(new OIDSSFLogSuccessCondition("The transmitter transmitted the verification event requested after the stream was enabled again"),
					Condition.ConditionResult.FAILURE, "OIDSSF-8.1.2.1");
			} else {
				callAndContinueOnFailure(new OIDSSFFindingCondition("The verification event requested after the stream was enabled again was not delivered within "
						+ HELD_EVENT_WAIT_WINDOW.toSeconds() + " seconds. An enabled stream must transmit events again."),
					Condition.ConditionResult.FAILURE, "OIDSSF-8.1.2.1");
			}
		});
	}
}
