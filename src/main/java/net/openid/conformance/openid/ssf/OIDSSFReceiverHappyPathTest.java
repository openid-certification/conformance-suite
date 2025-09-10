package net.openid.conformance.openid.ssf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantConfigurationFields;

import java.util.concurrent.TimeUnit;

@PublishTestModule(
	testName = "openid-ssf-receiver-happypath",
	displayName = "OpenID Shared Signals Framework: Test Receiver Stream Management",
	summary = "This test verifies the receiver stream management. " +
		"The test generates a dynamic transmitter and waits for a receiver to register a stream. " +
		"The testsuite expects to observe a stream create, update, replacement and deletion request. " +
		"The test completes once the events are observed",
	profile = "OIDSSF",
	configurationFields = {
		"ssf.transmitter.access_token",
		"ssf.stream.audience",
		"ssf.subjects.valid",
		"ssf.subjects.invalid"
	}
)
@VariantConfigurationFields(
	parameter = SsfDeliveryMode.class,
	value = "push",
	configurationFields = {"ssf.transmitter.push_endpoint_authorization_header"}
)
public class OIDSSFReceiverHappyPathTest extends AbstractOIDSSFReceiverTestModule {

	volatile String createdStreamId;

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
		eventLog.log(getId(), "Detected stream creation, update, replace, deletion.");
		super.fireTestFinished();
	}

	@Override
	protected boolean isFinished() {
		return createdStreamId != null
			&& createdStreamId.equals(updatedStreamId)
			&& createdStreamId.equals(replacedStreamId)
			&& createdStreamId.equals(deletedStreamId);
	}

	@Override
	protected void afterStreamCreation(String streamId, JsonObject result, JsonElement error) {
		createdStreamId = streamId;
	}

	@Override
	protected void afterStreamUpdate(String streamId, JsonObject result, JsonElement error) {
		if (streamId != null) {
			updatedStreamId = streamId;
		}
	}

	@Override
	protected void afterStreamReplace(String streamId, JsonObject result, JsonElement error) {
		if (streamId != null) {
			replacedStreamId = streamId;
		}
	}

	@Override
	protected void afterStreamDeletion(String streamId, JsonObject result, JsonElement error) {
		if (streamId != null) {
			deletedStreamId = streamId;
		}
	}
}
