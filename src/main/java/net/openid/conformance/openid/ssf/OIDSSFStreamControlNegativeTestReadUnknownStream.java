package net.openid.conformance.openid.ssf;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs404;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCreateAndDeleteStreamConditionSequence;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFReadStreamConfigCall;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-ssf-stream-control-error-read-unknown-stream",
	displayName = "Attempt to read an non existing Stream Configuration a valid access token.",
	summary = """
		This test verifies that the transmitter rejects a read of a non-existing stream.
		The testsuite expects to observe the following interactions:
		 * attempt to read a non-existing stream with a valid access token
		 * transmitter rejects the request with a 404 response
		""",
	profile = "OIDSSF"
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
