package net.openid.conformance.openid.ssf;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs401;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCreateStreamConditionSequence;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFInjectInvalidAccessTokenOverride;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-ssf-stream-control-error-create-stream-with-invalid-token",
	displayName = "Attempt to create Stream Configuration with invalid access token.",
	summary = "This test verifies the behavior of the stream control for error cases. It attempts to create a stream configuration with an invalid access token.",
	profile = "OIDSSF"
)
public class OIDSSFStreamControlNegativeTestCreateStreamWithInvalidAccessToken extends AbstractStreamControlErrorTest {


	@Override
	protected void testTransmitter() {

		// expect 401	if authorization failed or it is missing
		eventLog.runBlock("Attempt to create Stream Configuration with invalid access token", () -> {
			callAndStopOnFailure(OIDSSFInjectInvalidAccessTokenOverride.class);
			call(sequence(OIDSSFCreateStreamConditionSequence.class));
			OIDSSFInjectInvalidAccessTokenOverride.undo(env);
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs401.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.1");
			call(exec().unmapKey("endpoint_response"));
		});
	}
}
