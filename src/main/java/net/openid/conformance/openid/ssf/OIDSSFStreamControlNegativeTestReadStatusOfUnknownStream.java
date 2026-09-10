package net.openid.conformance.openid.ssf;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs404;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCreateAndDeleteStreamConditionSequence;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFReadStreamStatusCall;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-ssf-stream-control-error-read-status-of-unknown-stream",
	displayName = "Attempt to read the Stream Status of a non-existing stream with a valid access token.",
	summary = """
		This test verifies that the transmitter rejects a status read for a non-existing stream
		(SSF 1.0 8.1.2.1, Table 6: "404 if there is no Event Stream with the given stream_id for
		this Event Receiver").
		The testsuite expects to observe the following interactions:
		 * create and delete a stream to obtain a valid but unknown stream_id
		 * attempt to read the status of that stream with a valid access token
		 * transmitter rejects the request with a 404 response

		The test is skipped if the transmitter metadata has no status_endpoint (OPTIONAL per
		SSF 1.0 7.1); under the CAEP Interop Profile the endpoint is required (2.3.5) and the
		test fails instead.
		""",
	profile = "OIDSSF"
)
public class OIDSSFStreamControlNegativeTestReadStatusOfUnknownStream extends AbstractStreamControlErrorTest {

	@Override
	protected void onTransmitterMetadataFetched() {
		requireStatusEndpoint();
	}

	@Override
	protected void testTransmitter() {

		eventLog.runBlock("Create throw-away stream to collect valid but unknown stream_id", () -> {
			call(sequence(OIDSSFCreateAndDeleteStreamConditionSequence.class));
		});

		eventLog.runBlock("Attempt to read the Stream Status of a non-existing stream", () -> {
			callAndContinueOnFailure(OIDSSFReadStreamStatusCall.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.2.1");
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs404.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.2.1");
			call(exec().unmapKey("endpoint_response"));
		});
	}
}
