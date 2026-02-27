package net.openid.conformance.openid.ssf;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs404;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCreateAndDeleteStreamConditionSequence;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFUpdateStreamConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-ssf-stream-control-error-update-unknown-stream",
	displayName = "Attempt to update Stream Configuration for unknown stream.",
	summary = "This test verifies the behavior of the stream control for error cases. It attempts to update a stream configuration for an unknown stream.",
	profile = "OIDSSF"
)
public class OIDSSFStreamControlNegativeTestUpdateUnknownStream extends AbstractStreamControlErrorTest {

	@Override
	protected void testTransmitter() {

		eventLog.runBlock("Create throw-away stream to collect valid but unknown stream_id", () -> {
			call(sequence(OIDSSFCreateAndDeleteStreamConditionSequence.class));
		});

		// expect 404	if there is no Event Stream with the given "stream_id" for this Event Receiver
		eventLog.runBlock("Attempt to update Stream Configuration for unknown stream", () -> {
			call(sequence(OIDSSFUpdateStreamConditionSequence.class));
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs404.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.3");
			call(exec().unmapKey("endpoint_response"));
		});
	}
}
