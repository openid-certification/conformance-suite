package net.openid.conformance.openid.ssf;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs204;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIsAnyOf;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCreateStreamConditionSequence;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFDeleteStreamConfigCall;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFEnsurePollEndpointUrlsDifferPerStream;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "openid-ssf-transmitter-poll-endpoint-url-unique-per-stream",
	displayName = "OpenID Shared Signals Framework: Poll endpoint_url is unique per stream",
	summary = """
		This test verifies that a transmitter supporting multiple streams per receiver hands out a
		distinct poll endpoint_url for each stream. SSF 1.0 6.1.2: poll endpoint_url values "MAY be
		reused across Receivers, but MUST be unique per stream for a given Receiver."
		The testsuite expects to observe the following interactions:
		 * create a first poll stream
		 * attempt to create a second poll stream with the same access token
		 * if the transmitter rejects the second stream with 409 (it supports only one stream per
		   receiver), the test is skipped because uniqueness cannot be exercised
		 * otherwise the two streams must report different poll endpoint_url values
		 * delete both streams
		""",
	profile = "OIDSSF"
)
@VariantNotApplicable(parameter = SsfDeliveryMode.class, values = "push")
public class OIDSSFTransmitterPollEndpointUrlUniquePerStreamTest extends AbstractStreamControlErrorTest {

	private volatile String firstStreamId;

	private volatile String secondStreamId;

	@Override
	protected void testTransmitter() {

		eventLog.runBlock("Create first Stream Configuration", () -> {
			call(sequence(OIDSSFCreateStreamConditionSequence.class));
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndStopOnFailure(EnsureHttpStatusCodeIs201.class, "OIDSSF-8.1.1.1");
			call(exec().unmapKey("endpoint_response"));

			JsonObject firstStream = env.getElementFromObject("ssf", "stream").getAsJsonObject().deepCopy();
			env.putObject("ssf", "first_stream", firstStream);
			firstStreamId = env.getString("ssf", "stream.stream_id");
		});

		eventLog.runBlock("Attempt to create a second Stream Configuration with the same access token", () -> {
			call(sequence(OIDSSFCreateStreamConditionSequence.class));
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndStopOnFailure(new EnsureHttpStatusCodeIsAnyOf(201, 409), "OIDSSF-8.1.1.1");
			call(exec().unmapKey("endpoint_response"));
		});

		int secondCreateStatus = env.getInteger("resource_endpoint_response_full", "status");
		if (secondCreateStatus == 409) {
			eventLog.runBlock("Delete first Stream Configuration", this::deleteFirstStream);
			fireTestSkipped("The transmitter rejected the second stream with 409, i.e. it does not support multiple streams "
				+ "per receiver, so the per-stream uniqueness of the poll endpoint_url (SSF 1.0 6.1.2) cannot be exercised.");
		}

		eventLog.runBlock("Compare the poll endpoint_url of both streams", () -> {
			JsonObject secondStream = env.getElementFromObject("ssf", "stream").getAsJsonObject().deepCopy();
			env.putObject("ssf", "second_stream", secondStream);
			secondStreamId = env.getString("ssf", "stream.stream_id");

			callAndContinueOnFailure(OIDSSFEnsurePollEndpointUrlsDifferPerStream.class, Condition.ConditionResult.FAILURE, "OIDSSF-6.1.2");
		});

		eventLog.runBlock("Delete second Stream Configuration", () -> deleteStream(secondStreamId, () -> secondStreamId = null));

		eventLog.runBlock("Delete first Stream Configuration", this::deleteFirstStream);
	}

	private void deleteFirstStream() {
		deleteStream(firstStreamId, () -> firstStreamId = null);
	}

	/**
	 * Deletes the given stream; the id is passed via {@code ssf.stream.stream_id_override}
	 * because {@code ssf.stream} only holds the most recently created stream.
	 */
	private void deleteStream(String streamId, Runnable onDeleted) {
		env.putString("ssf", "stream.stream_id_override", streamId);
		try {
			callAndStopOnFailure(OIDSSFDeleteStreamConfigCall.class, "OIDSSF-8.1.1.5");
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndStopOnFailure(EnsureHttpStatusCodeIs204.class, "OIDSSF-8.1.1.5");
			call(exec().unmapKey("endpoint_response"));
			onDeleted.run();
		} finally {
			env.removeElement("ssf", "stream.stream_id_override");
		}
	}

	@Override
	public void cleanup() {
		eventLog.runBlock("Cleanup", () -> {
			for (String streamId : new String[] {secondStreamId, firstStreamId}) {
				if (streamId != null) {
					env.putString("ssf", "stream.stream_id_override", streamId);
					callAndContinueOnFailure(OIDSSFDeleteStreamConfigCall.class, Condition.ConditionResult.INFO);
					env.removeElement("ssf", "stream.stream_id_override");
				}
			}
			super.cleanup();
		});
	}
}
