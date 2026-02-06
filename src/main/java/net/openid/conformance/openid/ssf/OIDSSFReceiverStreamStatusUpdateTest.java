package net.openid.conformance.openid.ssf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openid.ssf.conditions.OIDSSFLogSuccessCondition;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantConfigurationFields;

import java.util.concurrent.TimeUnit;

@PublishTestModule(
	testName = "openid-ssf-receiver-stream-status-update",
	displayName = "OpenID Shared Signals Framework: Test Receiver Stream Status Management",
	summary = """
		This test verifies the receiver stream status management.
		The test generates a dynamic transmitter and waits for a receiver to register a stream.
		The testsuite expects to observe the following interactions:
		 * create a stream
		 * update the stream status
		 * delete the stream
		""",
	profile = "OIDSSF",
	configurationFields = {
		"ssf.transmitter.access_token",
		"ssf.stream.audience",
		"ssf.subjects.valid",
		"ssf.subjects.invalid"
	})
@VariantConfigurationFields(
	parameter = SsfDeliveryMode.class,
	value = "push",
	configurationFields = {
	})
public class OIDSSFReceiverStreamStatusUpdateTest extends AbstractOIDSSFReceiverTestModule {

	volatile String createdStreamId;

	volatile String updatedStatusStreamId;

	volatile String deletedStreamId;

	@Override
	public void start() {
		super.start();
		scheduleTask(new CheckTestFinishedTask(this::isFinished), 4, TimeUnit.SECONDS);
	}

	@Override
	protected boolean isFinished() {
		return createdStreamId != null
			&& createdStreamId.equals(deletedStreamId)
			&& createdStreamId.equals(updatedStatusStreamId);
	}

	@Override
	public void fireTestFinished() {
		eventLog.log(getName(), "Detected all expected stream operations.");
		super.fireTestFinished();
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
	protected void onStreamStatusUpdateSuccess(String streamId, JsonElement result) {
		updatedStatusStreamId = streamId;
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream status update for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.2.2");
	}

	@Override
	protected void afterStreamDeletion(String streamId, JsonObject deleteResult, JsonElement error) {
		deletedStreamId = streamId;
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream deletion for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.5");
	}
}
