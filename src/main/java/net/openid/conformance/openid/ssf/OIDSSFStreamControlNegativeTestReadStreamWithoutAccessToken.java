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
		failed or is missing; RFC 6750 section 3 requires the rejection to carry a
		'WWW-Authenticate' challenge (the CAEP Interop Profile 2.7.2 cites RFC 6750
		section 3.1, which defines the error codes carried in that challenge).
		The testsuite expects to observe the following interactions:
		 * attempt to read a stream configuration without an Authorization header
		 * transmitter rejects the request with a 401 response
		 * the 401 response should carry a Bearer 'WWW-Authenticate' challenge (RFC 6750
		   section 3; reported as a warning if absent, since CAEP Interop 2.7.2 only cites
		   RFC 6750 section 3.1)
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
			// WARNING: the WWW-Authenticate MUST is RFC 6750 section 3; CAEPIOP 2.7.2 only
			// cites section 3.1 (the error codes), so the profile's normative chain to the
			// header is imprecise - see the condition's javadoc. The 401 above is the
			// FAILURE-level check.
			callAndContinueOnFailure(OIDSSFEnsureWwwAuthenticateHeaderPresent.class, Condition.ConditionResult.WARNING, "CAEPIOP-2.7.2", "RFC6750-3", "RFC6750-3.1");
			call(exec().unmapKey("endpoint_response"));
		});
	}
}
