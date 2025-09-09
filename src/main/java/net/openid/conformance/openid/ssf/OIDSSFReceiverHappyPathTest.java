package net.openid.conformance.openid.ssf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantConfigurationFields;

import java.util.concurrent.TimeUnit;

@PublishTestModule(
	testName = "openid-ssf-receiver-happypath",
	displayName = "OpenID Shared Signals Framework: Validate Receiver Handling",
	summary = "This test verifies the receiver stream management and event delivery. " +
		"The test generates a dynamic transmitter and waits for a receiver to register a stream. " +
		"Then the testsuite will generate some supported events and deliver it to the receiver via the configured delivery mechanism. " +
		"The testsuite will then wait for acknowledgement of those events.",
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

	String createdStreamId;

	String updatedStreamId;

	String updatedStatusStreamId;

	String replacedStreamId;

	String deletedStreamId;

	@Override
	protected void afterStreamCreation(String streamId, JsonObject result, JsonElement error) {

		// if cape interop profile
		// enqueue supported cape events
		createdStreamId = streamId;

		scheduleDeferredAction(new DeferredAction("Wait for stream management operations") {

			@Override
			public void run() {

				if (isFinished()) {

					eventLog.log(getId(), "Detected stream creation, update, updateStatus, replace, deletion.");
					fireTestFinished();
					return;
				}

				scheduleDeferredAction(this, 3, TimeUnit.SECONDS);

			}
		}, 10, TimeUnit.SECONDS);
	}

	@Override
	protected boolean isFinished() {
		return createdStreamId != null
			&& createdStreamId.equals(updatedStreamId)
			&& createdStreamId.equals(updatedStatusStreamId)
			&& createdStreamId.equals(replacedStreamId)
			&& createdStreamId.equals(deletedStreamId);
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

	@Override
	protected void onStreamStatusUpdateSuccess(String streamId, JsonElement result) {
		if (streamId != null) {
			updatedStatusStreamId = streamId;
		}
	}
}
