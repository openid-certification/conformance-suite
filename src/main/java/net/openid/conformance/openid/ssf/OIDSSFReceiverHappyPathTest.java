package net.openid.conformance.openid.ssf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openid.ssf.conditions.OIDSSFLogSuccessCondition;
import net.openid.conformance.testmodule.PublishTestModule;

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
		 * update the stream
		 * replace the stream
		 * delete the stream""",
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
		scheduleTask(new CheckTestFinishedTask(this::isFinished), 3, TimeUnit.SECONDS);
	}

	@Override
	public void fireTestFinished() {
		eventLog.log(getName(), "Detected stream create, read, update, replace, delete.");
		super.fireTestFinished();
	}

	@Override
	protected boolean isFinished() {
		return createdStreamId != null
			&& createdStreamId.equals(readStreamId)
			&& createdStreamId.equals(updatedStreamId)
			&& createdStreamId.equals(replacedStreamId)
			&& createdStreamId.equals(deletedStreamId);
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
