package net.openid.conformance.openid.ssf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantConfigurationFields;

import java.util.concurrent.TimeUnit;

@PublishTestModule(testName = "openid-ssf-receiver-stream-create-delete", displayName = "OpenID Shared Signals Framework: Create and Delete Stream", summary = "This test verifies the receiver stream management. " + "The test generates a dynamic transmitter and waits for a receiver to register a stream. ", profile = "OIDSSF", configurationFields = {"ssf.transmitter.access_token", "ssf.stream.audience", "ssf.subjects.valid", "ssf.subjects.invalid"})
@VariantConfigurationFields(parameter = SsfDeliveryMode.class, value = "push", configurationFields = {"ssf.transmitter.push_endpoint_authorization_header"})
public class OIDSSFReceiverStreamCreateDeleteTest extends AbstractOIDSSFReceiverTestModule {

	String createdStreamId;

	String deletedStreamId;

	@Override
	protected void afterStreamCreation(String streamId, JsonObject createResult, JsonElement error) {

		// if cape interop profile
		// enqueue supported cape events
		if (createResult == null) {
			return;
		}

		createdStreamId = OIDFJSON.tryGetString(createResult.get("stream_id"));

		scheduleDeferredAction(new DeferredAction("Wait for stream verification") {

			@Override
			public void run() {

				if (isFinished()) {

					eventLog.log(getId(), "Detected created and verified stream.");
					fireTestFinished();
					return;
				}

				scheduleDeferredAction(this, 3, TimeUnit.SECONDS);

			}
		}, 4, TimeUnit.SECONDS);
	}

	@Override
	protected boolean isFinished() {
		return createdStreamId != null && createdStreamId.equals(deletedStreamId);
	}


	@Override
	protected void afterStreamDeletion(String streamId, JsonObject deleteResult, JsonElement error) {
		deletedStreamId = streamId;
	}
}
