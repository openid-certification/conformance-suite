package net.openid.conformance.openid.ssf;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs401;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFInjectInvalidAccessTokenOverride;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFUpdateStreamConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-ssf-stream-control-error-update-stream-with-invalid-token",
	displayName = "Attempt to update Stream Configuration with invalid access token.",
	summary = "This test verifies the behavior of the stream control for error cases. It attempts to update a stream configuration with an invalid access token.",
	profile = "OIDSSF",
	configurationFields = {
		"ssf.transmitter.issuer",
		"ssf.transmitter.metadata_suffix", // see: https://openid.net/specs/openid-sharedsignals-framework-1_0.html#section-6.2.1
	}
)
public class OIDSSFStreamControlNegativeTestUpdateStreamWithInvalidToken extends AbstractStreamControlErrorTest {

	@Override
	protected void testTransmitter() {

		// expect 401	if authorization failed or it is missing
		eventLog.runBlock("Attempt to update Stream Configuration with invalid access token", () -> {
			callAndStopOnFailure(OIDSSFInjectInvalidAccessTokenOverride.class);
			call(sequence(OIDSSFUpdateStreamConditionSequence.class));
			OIDSSFInjectInvalidAccessTokenOverride.undo(env);
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs401.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.3");
			call(exec().unmapKey("endpoint_response"));
		});
	}
}
