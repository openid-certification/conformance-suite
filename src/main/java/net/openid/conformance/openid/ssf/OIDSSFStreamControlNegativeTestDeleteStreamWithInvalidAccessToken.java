package net.openid.conformance.openid.ssf;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs401;
import net.openid.conformance.openid.ssf.conditions.OIDSSFEnsureWwwAuthenticateErrorCode;
import net.openid.conformance.openid.ssf.conditions.OIDSSFEnsureWwwAuthenticateHeaderPresent;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCreateAndDeleteStreamConditionSequence;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFDeleteStreamConfigCall;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFInjectInvalidAccessTokenOverride;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-ssf-stream-control-error-delete-stream-with-invalid-token",
	displayName = "Attempt to delete a Stream Configuration with invalid access token.",
	summary = """
		This test verifies that the transmitter rejects a stream deletion with an invalid access token.
		The testsuite expects to observe the following interactions:
		 * create a stream
		 * attempt to delete the stream with an invalid access token
		 * transmitter rejects the request with a 401 response
		 * the 401 response should carry a Bearer 'WWW-Authenticate' challenge with
		   error="invalid_token" (RFC 6750 sections 3 and 3.1; reported as a warning if absent,
		   since CAEP Interop 2.7.2 only cites RFC 6750 section 3.1)
		""",
	profile = "OIDSSF"
)
public class OIDSSFStreamControlNegativeTestDeleteStreamWithInvalidAccessToken extends AbstractStreamControlErrorTest {

	@Override
	protected void testTransmitter() {

		eventLog.runBlock("Create throw-away stream to collect valid but unknown stream_id", () -> {
			call(sequence(OIDSSFCreateAndDeleteStreamConditionSequence.class));
		});

		// Expect 401	if authorization failed or it is missing
		eventLog.runBlock("Delete Stream Configuration with invalid access token", () -> {
			callAndStopOnFailure(OIDSSFInjectInvalidAccessTokenOverride.class);
			callAndContinueOnFailure(OIDSSFDeleteStreamConfigCall.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.5");
			OIDSSFInjectInvalidAccessTokenOverride.undo(env);
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs401.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.5");
			// WARNING: the WWW-Authenticate MUST is RFC 6750 section 3; CAEPIOP 2.7.2 only
			// cites section 3.1 (the error codes), so the profile's normative chain to the
			// header is imprecise - see the condition's javadoc. The 401 above is the
			// FAILURE-level check.
			callAndContinueOnFailure(OIDSSFEnsureWwwAuthenticateHeaderPresent.class, Condition.ConditionResult.WARNING, "CAEPIOP-2.7.2", "RFC6750-3", "RFC6750-3.1");
			callAndContinueOnFailure(new OIDSSFEnsureWwwAuthenticateErrorCode("invalid_token"), Condition.ConditionResult.WARNING, "CAEPIOP-2.7.2", "RFC6750-3.1");
			call(exec().unmapKey("endpoint_response"));
		});
	}
}
