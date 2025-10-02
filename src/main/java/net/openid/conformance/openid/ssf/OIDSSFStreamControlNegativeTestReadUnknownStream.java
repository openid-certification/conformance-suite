package net.openid.conformance.openid.ssf;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs404;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCreateAndDeleteStreamConditionSequence;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFReadStreamConfigCall;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-ssf-stream-control-error-read-unknown-stream",
	displayName = "Attempt to read an non existing Stream Configuration a valid access token.",
	summary = "This test verifies the behavior of the stream control for error cases. It attempts to read a non-existing stream configuration a valid access token.",
	profile = "OIDSSF",
	configurationFields = {
		"ssf.transmitter.issuer",
		"ssf.transmitter.metadata_suffix", // see: https://openid.net/specs/openid-sharedsignals-framework-1_0.html#section-6.2.1
	}
)
public class OIDSSFStreamControlNegativeTestReadUnknownStream extends AbstractStreamControlErrorTest {

	@Override
	protected void testTransmitter() {

		eventLog.runBlock("Create throw-away stream to collect valid but unknown stream_id", () -> {
			call(sequence(OIDSSFCreateAndDeleteStreamConditionSequence.class));
		});

		// expect 404	if there is no Event Stream with the given "stream_id" for this Event Receiver
		eventLog.runBlock("Attempt to read an non existing Stream Configuration a valid access token", () -> {
			// try to read stream config with valid but unknown stream_id
			callAndStopOnFailure(OIDSSFReadStreamConfigCall.class, "OIDSSF-7.1.1.2");
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs404.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.2");
			call(exec().unmapKey("endpoint_response"));
		});
	}
}
