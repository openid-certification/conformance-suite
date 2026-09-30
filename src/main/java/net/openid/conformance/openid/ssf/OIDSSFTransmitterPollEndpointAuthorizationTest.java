package net.openid.conformance.openid.ssf;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs401;
import net.openid.conformance.openid.ssf.conditions.OIDSSFEnsureWwwAuthenticateErrorCode;
import net.openid.conformance.openid.ssf.conditions.OIDSSFEnsureWwwAuthenticateHeaderPresent;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFCallPollEndpoint;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCreateStreamConditionSequence;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFDeleteStreamConfigCall;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFEnsureNoAccessTokenInAuthorizationHeaderOverride;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFInjectInvalidAccessTokenOverride;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "openid-ssf-transmitter-poll-endpoint-authorization",
	displayName = "Poll endpoint rejects requests without a valid access token.",
	summary = """
		This test verifies that the transmitter protects the poll endpoint of a POLL stream with
		the same authorization scheme as the stream management API. SSF 1.0 (7.1.1) says the
		advertised authorization schemes "SHOULD also be used to protect any polling endpoint";
		the CAEP Interop Profile (2.7.2) does not name the poll endpoint explicitly, so a poll
		endpoint that answers an unauthenticated request is reported as a warning under both
		profiles. A poll endpoint that does answer 401 uses HTTP authentication, and RFC 8936
		(section 3) then requires it to name the supported schemes in a 'WWW-Authenticate'
		header, so a 401 without that header is a failure; the error code inside the header
		is a warning.
		The testsuite expects to observe the following interactions:
		 * create a stream with poll delivery
		 * poll the stream's endpoint_url without an Authorization header
		 * transmitter should reject the request with a 401 response carrying a Bearer
		   'WWW-Authenticate' challenge (RFC 8936 section 3, RFC 6750 section 3; no error code
		   is expected for a request without credentials, RFC 6750 section 3.1)
		 * poll the stream's endpoint_url with an invalid access token
		 * transmitter should reject the request with a 401 response carrying a Bearer
		   'WWW-Authenticate' challenge with error="invalid_token" (RFC 6750 sections 3 and 3.1)
		 * delete the stream
		""",
	profile = "OIDSSF"
)
@VariantNotApplicable(parameter = SsfDeliveryMode.class, values = "push")
public class OIDSSFTransmitterPollEndpointAuthorizationTest extends AbstractStreamControlErrorTest {

	@Override
	protected void testTransmitter() {

		eventLog.runBlock("Create Stream Configuration", () -> {
			call(sequence(OIDSSFCreateStreamConditionSequence.class));
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs201.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1.1");
			call(exec().unmapKey("endpoint_response"));
		});

		env.putString("ssf", "poll.mode", OIDSSFCallPollEndpoint.PollMode.POLL_ONLY.name());

		eventLog.runBlock("Poll the stream without an Authorization header", () -> {
			callAndStopOnFailure(OIDSSFEnsureNoAccessTokenInAuthorizationHeaderOverride.class, "OIDSSF-7.1.1");
			callAndContinueOnFailure(OIDSSFCallPollEndpoint.class, Condition.ConditionResult.FAILURE, "OIDSSF-6.1.2", "RFC8936-2.4");
			OIDSSFEnsureNoAccessTokenInAuthorizationHeaderOverride.undo(env);
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			// WARNING: SSF 1.0 7.1.1 is a SHOULD and CAEPIOP 2.7.2 does not name the poll
			// endpoint, so a poll endpoint that answers an unauthenticated request is a finding,
			// not a failure. RFC 6750 3.1: no error code is expected without credentials.
			callAndContinueOnFailure(EnsureHttpStatusCodeIs401.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1", "RFC8936-3");
			if (pollEndpointAnswered401()) {
				// RFC 8936 3: a delivery endpoint using HTTP authentication SHALL name the schemes
				// it supports in a WWW-Authenticate header
				callAndContinueOnFailure(OIDSSFEnsureWwwAuthenticateHeaderPresent.class, Condition.ConditionResult.FAILURE, "RFC8936-3", "RFC6750-3");
			}
			call(exec().unmapKey("endpoint_response"));
		});

		eventLog.runBlock("Poll the stream with an invalid access token", () -> {
			callAndStopOnFailure(OIDSSFInjectInvalidAccessTokenOverride.class, "OIDSSF-7.1.1");
			callAndContinueOnFailure(OIDSSFCallPollEndpoint.class, Condition.ConditionResult.FAILURE, "OIDSSF-6.1.2", "RFC8936-2.4");
			OIDSSFInjectInvalidAccessTokenOverride.undo(env);
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			// WARNING: see above - SSF 1.0 7.1.1 is a SHOULD for the poll endpoint.
			callAndContinueOnFailure(EnsureHttpStatusCodeIs401.class, Condition.ConditionResult.WARNING, "OIDSSF-7.1.1", "RFC8936-3");
			if (pollEndpointAnswered401()) {
				callAndContinueOnFailure(OIDSSFEnsureWwwAuthenticateHeaderPresent.class, Condition.ConditionResult.FAILURE, "RFC8936-3", "RFC6750-3");
				callAndContinueOnFailure(new OIDSSFEnsureWwwAuthenticateErrorCode("invalid_token"), Condition.ConditionResult.WARNING, "OIDSSF-7.1.1", "RFC6750-3.1");
			}
			call(exec().unmapKey("endpoint_response"));
		});
	}

	/** Whether the poll endpoint rejected the last request with 401, i.e. it uses HTTP authentication. */
	private boolean pollEndpointAnswered401() {
		Integer status = env.getInteger("endpoint_response", "status");
		return status != null && status == 401;
	}

	@Override
	public void cleanup() {
		eventLog.runBlock("Cleanup", () -> {
			callAndContinueOnFailure(OIDSSFDeleteStreamConfigCall.class, Condition.ConditionResult.INFO, "OIDSSF-8.1.1.5");
			super.cleanup();
		});
	}
}
