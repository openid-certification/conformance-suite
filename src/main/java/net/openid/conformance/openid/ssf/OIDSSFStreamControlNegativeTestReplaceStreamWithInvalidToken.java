package net.openid.conformance.openid.ssf;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs401;
import net.openid.conformance.openid.ssf.conditions.OIDSSFEnsureWwwAuthenticateErrorCode;
import net.openid.conformance.openid.ssf.conditions.OIDSSFEnsureWwwAuthenticateHeaderPresent;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFInjectInvalidAccessTokenOverride;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFReplaceStreamConditionSequence;
import net.openid.conformance.openid.ssf.variant.SsfProfile;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "openid-ssf-stream-control-error-replace-stream-with-invalid-token",
	displayName = "Attempt to replace Stream Configuration with invalid access token.",
	summary = """
		This test verifies that the transmitter rejects a stream replacement with an invalid access token.
		The testsuite expects to observe the following interactions:
		 * create a stream
		 * attempt to replace the stream with an invalid access token
		 * transmitter rejects the request with a 401 response
		 * the 401 response should carry a Bearer 'WWW-Authenticate' challenge with
		   error="invalid_token" (RFC 6750 sections 3 and 3.1, required by CAEP Interop 2.7.2)
		""",
	profile = "OIDSSF"
)
@VariantNotApplicable(parameter = SsfProfile.class, values = "caep_interop")
public class OIDSSFStreamControlNegativeTestReplaceStreamWithInvalidToken extends AbstractStreamControlErrorTest {

	@Override
	protected void testTransmitter() {

		//	Expect 401	if authorization failed or it is missing
		eventLog.runBlock("Attempt to replace a Stream Configuration with invalid access token", () -> {
			callAndStopOnFailure(OIDSSFInjectInvalidAccessTokenOverride.class);
			call(sequence(OIDSSFReplaceStreamConditionSequence.class));
			OIDSSFInjectInvalidAccessTokenOverride.undo(env);
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs401.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.4");
			// RFC 6750 section 3: the 401 MUST carry a WWW-Authenticate challenge, and CAEP
			// Interop 2.7.2 requires the error codes of RFC 6750 section 3.1 inside it.
			callAndContinueOnFailure(OIDSSFEnsureWwwAuthenticateHeaderPresent.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.7.2", "RFC6750-3", "RFC6750-3.1");
			callAndContinueOnFailure(new OIDSSFEnsureWwwAuthenticateErrorCode("invalid_token"), Condition.ConditionResult.FAILURE, "CAEPIOP-2.7.2", "RFC6750-3.1");
			call(exec().unmapKey("endpoint_response"));
		});
	}
}
