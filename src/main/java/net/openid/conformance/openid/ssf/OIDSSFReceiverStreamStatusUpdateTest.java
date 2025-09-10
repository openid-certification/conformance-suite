package net.openid.conformance.openid.ssf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantConfigurationFields;

import java.util.concurrent.TimeUnit;

@PublishTestModule(
	testName = "openid-ssf-receiver-stream-status-update",
	displayName = "OpenID Shared Signals Framework: Test Receiver Stream Status Management",
	summary = "This test verifies the receiver stream status management. " +
			"The test generates a dynamic transmitter and waits for a receiver to register a stream. " +
			"The testsuite expects to observe a stream create, status update with status paused, deletion request.",
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
		"ssf.transmitter.push_endpoint_authorization_header"
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
		eventLog.log(getId(), "Detected created and deleted stream.");
		super.fireTestFinished();
	}

	@Override
	protected void afterStreamCreation(String streamId, JsonObject createResult, JsonElement error) {

		if (createResult == null) {
			return;
		}

		createdStreamId = streamId;
	}

	@Override
	protected void onStreamStatusUpdateSuccess(String streamId, JsonElement result) {
		updatedStatusStreamId = streamId;
	}

	@Override
	protected void afterStreamDeletion(String streamId, JsonObject deleteResult, JsonElement error) {
		deletedStreamId = streamId;
	}
}
