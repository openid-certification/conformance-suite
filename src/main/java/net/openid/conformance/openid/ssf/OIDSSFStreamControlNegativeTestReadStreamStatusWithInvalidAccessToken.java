package net.openid.conformance.openid.ssf;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs401;
import net.openid.conformance.openid.ssf.conditions.OIDSSFEnsureWwwAuthenticateErrorCode;
import net.openid.conformance.openid.ssf.conditions.OIDSSFEnsureWwwAuthenticateHeaderPresent;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCreateStreamConditionSequence;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFDeleteStreamConfigCall;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFInjectInvalidAccessTokenOverride;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFReadStreamStatusCall;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-ssf-stream-control-error-read-stream-status-with-invalid-token",
	displayName = "Attempt to read Stream Status with invalid access token.",
	summary = """
		This test verifies that the transmitter rejects a stream status read with an invalid access
		token (SSF 1.0 8.1.2.1, Table 6: "401 if authorization failed or it is missing").
		The testsuite expects to observe the following interactions:
		 * create a stream
		 * attempt to read the stream status with an invalid access token
		 * transmitter rejects the request with a 401 response
		 * the 401 response should carry a Bearer 'WWW-Authenticate' challenge with
		   error="invalid_token" (RFC 6750 sections 3 and 3.1; reported as a warning if absent,
		   since CAEP Interop 2.7.2 only cites RFC 6750 section 3.1)
		 * delete the stream

		The test is skipped if the transmitter metadata has no status_endpoint (OPTIONAL per
		SSF 1.0 7.1); under the CAEP Interop Profile the endpoint is required (2.3.5) and the
		test fails instead.
		""",
	profile = "OIDSSF"
)
public class OIDSSFStreamControlNegativeTestReadStreamStatusWithInvalidAccessToken extends AbstractStreamControlErrorTest {

	@Override
	protected void onTransmitterMetadataFetched() {
		requireStatusEndpoint();
	}

	@Override
	protected void testTransmitter() {

		eventLog.runBlock("Create Stream Configuration", () -> {
			call(sequence(OIDSSFCreateStreamConditionSequence.class));
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs201.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1.1");
			call(exec().unmapKey("endpoint_response"));
		});

		eventLog.runBlock("Attempt to read the Stream Status with invalid access token", () -> {
			callAndStopOnFailure(OIDSSFInjectInvalidAccessTokenOverride.class);
			callAndContinueOnFailure(OIDSSFReadStreamStatusCall.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.2.1");
			OIDSSFInjectInvalidAccessTokenOverride.undo(env);
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs401.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.2.1");
			// WARNING: the WWW-Authenticate MUST is RFC 6750 section 3; CAEPIOP 2.7.2 only
			// cites section 3.1 (the error codes), so the profile's normative chain to the
			// header is imprecise - see the condition's javadoc. The 401 above is the
			// FAILURE-level check.
			callAndContinueOnFailure(OIDSSFEnsureWwwAuthenticateHeaderPresent.class, Condition.ConditionResult.WARNING, "CAEPIOP-2.7.2", "RFC6750-3", "RFC6750-3.1");
			callAndContinueOnFailure(new OIDSSFEnsureWwwAuthenticateErrorCode("invalid_token"), Condition.ConditionResult.WARNING, "CAEPIOP-2.7.2", "RFC6750-3.1");
			call(exec().unmapKey("endpoint_response"));
		});
	}

	@Override
	public void cleanup() {
		eventLog.runBlock("Cleanup", () -> {
			callAndContinueOnFailure(OIDSSFDeleteStreamConfigCall.class, Condition.ConditionResult.INFO, "OIDSSF-8.1.1.5");
			super.cleanup();
		});
	}
}
