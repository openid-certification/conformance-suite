package net.openid.conformance.openid.ssf;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFInsertBrokenStreamConfigJsonOverride;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFUpdateStreamConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-ssf-stream-control-error-update-stream-with-invalid-body",
	displayName = "Attempt to update Stream Configuration with invalid body.",
	summary = "This test verifies the behavior of the stream control for error cases. It attempts to update a stream configuration with an invalid body.",
	profile = "OIDSSF",
	configurationFields = {
		"ssf.transmitter.issuer",
		"ssf.transmitter.metadata_suffix", // see: https://openid.net/specs/openid-sharedsignals-framework-1_0.html#section-6.2.1
	}
)
public class OIDSSFStreamControlNegativeTestUpdateStreamWithInvalidBody extends AbstractStreamControlErrorTest {

	@Override
	protected void testTransmitter() {

		// expect 400	if the request body cannot be parsed, a Transmitter-Supplied property is incorrect, or if the request is otherwise invalid
		eventLog.runBlock("Attempt to update Stream Configuration with invalid body", () -> {
			callAndStopOnFailure(OIDSSFInsertBrokenStreamConfigJsonOverride.class);
			call(sequence(OIDSSFUpdateStreamConditionSequence.class));
			OIDSSFInsertBrokenStreamConfigJsonOverride.undo(env);
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs400.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.3");
			call(exec().unmapKey("endpoint_response"));
		});
	}
}
