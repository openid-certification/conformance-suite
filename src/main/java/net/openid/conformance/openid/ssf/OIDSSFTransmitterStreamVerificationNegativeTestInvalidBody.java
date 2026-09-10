package net.openid.conformance.openid.ssf;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFTriggerVerificationEvent;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCreateStreamConditionSequence;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFDeleteStreamConfigCall;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-ssf-transmitter-stream-verification-error-invalid-body",
	displayName = "Attempt to trigger a Stream Verification with an unparsable request body.",
	summary = """
		This test verifies that the transmitter rejects a verification request whose body is not
		valid JSON (SSF 1.0 8.1.4.2, Table 10: "400 if the request body cannot be parsed or if the
		request is otherwise invalid").
		The testsuite expects to observe the following interactions:
		 * create a stream
		 * POST a truncated JSON document to the verification endpoint with a valid access token
		 * transmitter rejects the request with a 400 response
		 * delete the stream

		The test is skipped if the transmitter metadata has no verification_endpoint (OPTIONAL
		per SSF 1.0 7.1); under the CAEP Interop Profile the endpoint is required (2.3.6) and the
		test fails instead.
		""",
	profile = "OIDSSF"
)
public class OIDSSFTransmitterStreamVerificationNegativeTestInvalidBody extends AbstractStreamControlErrorTest {

	@Override
	protected void onTransmitterMetadataFetched() {
		requireVerificationEndpoint();
	}

	@Override
	protected void testTransmitter() {

		eventLog.runBlock("Create Stream Configuration", () -> {
			call(sequence(OIDSSFCreateStreamConditionSequence.class));
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs201.class, Condition.ConditionResult.WARNING, "OIDSSF-8.1.1.1");
			call(exec().unmapKey("endpoint_response"));
		});

		eventLog.runBlock("Attempt to trigger a verification event with an unparsable request body", () -> {
			env.putString("ssf", "verification.request_body_override", "{\"stream_id\": ");
			callAndContinueOnFailure(OIDSSFTriggerVerificationEvent.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.2");
			env.removeElement("ssf", "verification.request_body_override");
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs400.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.2");
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
