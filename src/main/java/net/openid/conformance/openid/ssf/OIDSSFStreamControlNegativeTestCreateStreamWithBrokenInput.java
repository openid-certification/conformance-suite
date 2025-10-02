package net.openid.conformance.openid.ssf;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCreateStreamConditionSequence;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFInsertBrokenStreamConfigJsonOverride;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-ssf-stream-control-error-create-stream-with-broken-input",
	displayName = "Attempt to create Stream Configuration with broken input.",
	summary = "This test verifies the behavior of the stream control for error cases. It attempts to create a stream configuration with broken input.",
	profile = "OIDSSF",
	configurationFields = {
		"ssf.transmitter.issuer",
		"ssf.transmitter.metadata_suffix", // see: https://openid.net/specs/openid-sharedsignals-framework-1_0.html#section-6.2.1
	}
)
public class OIDSSFStreamControlNegativeTestCreateStreamWithBrokenInput extends AbstractStreamControlErrorTest {

	@Override
	protected void testTransmitter() {
		// expect 400	if the request cannot be parsed
		eventLog.runBlock("Attempt to create Stream Configuration with broken input", () -> {
			callAndStopOnFailure(OIDSSFInsertBrokenStreamConfigJsonOverride.class);
			call(sequence(OIDSSFCreateStreamConditionSequence.class));
			OIDSSFInsertBrokenStreamConfigJsonOverride.undo(env);
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs400.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.1");
			call(exec().unmapKey("endpoint_response"));
		});
	}
}
