package net.openid.conformance.openid.ssf;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs401;
import net.openid.conformance.openid.ssf.conditions.OIDSSFEnsureWwwAuthenticateHeaderPresent;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFEnsureNoAccessTokenInAuthorizationHeaderOverride;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFReadStreamConfigCall;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-ssf-stream-control-error-read-stream-without-token",
	displayName = "Attempt to read Stream Configuration without an access token.",
	summary = """
		This test verifies that the transmitter rejects a stream read that carries no
		authorization at all. SSF 1.0 (8.1.1.2) requires a 401 response if authorization
		failed or is missing, and the CAEP Interop Profile (2.7.2) requires the error to
		follow RFC 6750 3.1, i.e. to carry a 'WWW-Authenticate' challenge.
		The testsuite expects to observe the following interactions:
		 * attempt to read a stream configuration without an Authorization header
		 * transmitter rejects the request with a 401 response
		 * the 401 response carries a Bearer 'WWW-Authenticate' challenge
		""",
	profile = "OIDSSF"
)
public class OIDSSFStreamControlNegativeTestReadStreamWithoutAccessToken extends AbstractStreamControlErrorTest {

	@Override
	protected void testTransmitter() {

		eventLog.runBlock("Attempt to read an existing Stream Configuration without an access token", () -> {

			callAndStopOnFailure(OIDSSFEnsureNoAccessTokenInAuthorizationHeaderOverride.class, "OIDSSF-8.1.1.2");
			callAndContinueOnFailure(OIDSSFReadStreamConfigCall.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.2");
			OIDSSFEnsureNoAccessTokenInAuthorizationHeaderOverride.undo(env);
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs401.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.2");
			callAndContinueOnFailure(OIDSSFEnsureWwwAuthenticateHeaderPresent.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.7.2", "RFC6750-3.1");
			call(exec().unmapKey("endpoint_response"));
		});
	}
}
