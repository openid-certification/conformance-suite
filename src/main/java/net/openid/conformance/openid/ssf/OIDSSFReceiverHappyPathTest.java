package net.openid.conformance.openid.ssf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openid.ssf.conditions.OIDSSFLogSuccessCondition;
import net.openid.conformance.openid.ssf.variant.SsfProfile;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@PublishTestModule(
	testName = "openid-ssf-receiver-happypath",
	displayName = "OpenID Shared Signals Framework: Test Receiver Stream Management",
	summary = """
		This test verifies the receiver stream management.
		The test generates a dynamic transmitter and waits for a receiver to register a stream.
		The testsuite expects to observe the following interactions:
		 * create a stream
		 * read the stream
		 * update the stream (*)
		 * replace the stream (*)
		 * delete the stream

		(*) Note that stream update/replacements are skipped when the CAEP Interop Profile is used.
		""",
	profile = "OIDSSF"
)
public class OIDSSFReceiverHappyPathTest extends AbstractOIDSSFReceiverTestModule {

	volatile String createdStreamId;

	volatile String readStreamId;

	volatile String updatedStreamId;

	volatile String replacedStreamId;

	volatile String deletedStreamId;

	@Override
	public void start() {
		super.start();
		// emit the initial "waiting for the receiver to: ..." checklist right away
		logExpectedInteractionsProgress();
		scheduleTask(new CheckTestFinishedTask(this::isFinished), 3, TimeUnit.SECONDS);
	}

	@Override
	public void fireTestFinished() {
		eventLog.log(getName(), "Detected stream create, read, update, replace, delete.");
		super.fireTestFinished();
	}

	@Override
	protected List<ExpectedInteraction> expectedInteractions() {
		List<ExpectedInteraction> interactions = new ArrayList<>();
		interactions.add(new ExpectedInteraction("ssf_create_stream", "create a stream", () -> createdStreamId != null));
		interactions.add(new ExpectedInteraction("ssf_read_stream_config", "read the stream configuration", () -> createdStreamId != null && createdStreamId.equals(readStreamId)));
		if (!isSsfProfileEnabled(SsfProfile.CAEP_INTEROP)) {
			// stream update/replace are skipped under the CAEP Interop Profile
			interactions.add(new ExpectedInteraction("ssf_update_stream", "update the stream", () -> createdStreamId != null && createdStreamId.equals(updatedStreamId)));
			interactions.add(new ExpectedInteraction("ssf_replace_stream", "replace the stream", () -> createdStreamId != null && createdStreamId.equals(replacedStreamId)));
		}
		interactions.add(new ExpectedInteraction("ssf_delete_stream", "delete the stream", () -> createdStreamId != null && createdStreamId.equals(deletedStreamId)));
		return List.copyOf(interactions);
	}

	@Override
	protected boolean isFinished() {
		// derived from the same checklist that drives the progress log entries,
		// so "still waiting for" can never disagree with the finish condition
		return expectedInteractions().stream().allMatch(interaction -> interaction.detected().getAsBoolean());
	}

	@Override
	protected void afterStreamCreation(String streamId, JsonObject result, JsonElement error) {
		createdStreamId = streamId;
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream creation for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.1");
	}

	@Override
	protected void afterStreamLookup(String streamId, JsonObject lookupResult, JsonElement error) {
		if (streamId != null) {
			readStreamId = streamId;
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream Lookup for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.2");
		}
	}

	@Override
	protected void afterStreamUpdate(String streamId, JsonObject result, JsonElement error) {
		if (streamId != null) {
			updatedStreamId = streamId;
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream update for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.3");
		}
	}

	@Override
	protected void afterStreamReplace(String streamId, JsonObject result, JsonElement error) {
		if (streamId != null) {
			replacedStreamId = streamId;
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream replace for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.4");
		}
	}

	@Override
	protected void afterStreamDeletion(String streamId, JsonObject result, JsonElement error) {
		if (streamId != null) {
			deletedStreamId = streamId;
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream deletion stream_id="+streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.5");
		}
	}
}
