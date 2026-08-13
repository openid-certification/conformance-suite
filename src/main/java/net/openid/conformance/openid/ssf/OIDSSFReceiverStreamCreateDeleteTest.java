package net.openid.conformance.openid.ssf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openid.ssf.conditions.OIDSSFLogSuccessCondition;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.List;
import java.util.concurrent.TimeUnit;

@PublishTestModule(
	testName = "openid-ssf-receiver-stream-create-delete",
	displayName = "OpenID Shared Signals Framework: Test Basic Receiver Stream Management",
	summary = """
		This test verifies minimal receiver stream management.
		The test generates a dynamic transmitter and waits for a receiver to register a stream.
		The testsuite expects to observe the following interactions:
		 * create a stream
		 * delete the stream
		""",
	profile = "OIDSSF"
)
public class OIDSSFReceiverStreamCreateDeleteTest extends AbstractOIDSSFReceiverTestModule {

	volatile String createdStreamId;

	volatile String deletedStreamId;

	@Override
	public void start() {
		super.start();
		// emit the initial "waiting for the receiver to: ..." checklist right away
		logExpectedInteractionsProgress();
		scheduleTask(new CheckTestFinishedTask(this::isFinished), 4, TimeUnit.SECONDS);
	}

	@Override
	public void fireTestFinished() {
		eventLog.log(getName(), "Detected all expected stream operations.");
		super.fireTestFinished();
	}

	@Override
	protected List<ExpectedInteraction> expectedInteractions() {
		return List.of(
			new ExpectedInteraction("ssf_create_stream", "create a stream", () -> createdStreamId != null),
			new ExpectedInteraction("ssf_delete_stream", "delete the stream", () -> createdStreamId != null && createdStreamId.equals(deletedStreamId))
		);
	}

	@Override
	protected boolean isFinished() {
		// derived from the same checklist that drives the progress log entries,
		// so "still waiting for" can never disagree with the finish condition
		return expectedInteractions().stream().allMatch(interaction -> interaction.detected().getAsBoolean());
	}

	@Override
	protected void afterStreamCreation(String streamId, JsonObject createResult, JsonElement error) {

		if (createResult == null) {
			return;
		}

		createdStreamId = streamId;
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream creation for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.1");
	}

	@Override
	protected void afterStreamDeletion(String streamId, JsonObject deleteResult, JsonElement error) {
		deletedStreamId = streamId;
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream deletion for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.5");
	}
}
