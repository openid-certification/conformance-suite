package net.openid.conformance.openid.ssf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantConfigurationFields;

import java.util.concurrent.TimeUnit;

@PublishTestModule(
	testName = "openid-ssf-receiver-stream-verification",
	displayName = "OpenID Shared Signals Framework: Test Receiver Stream Verification",
	summary = "This test verifies the receiver stream verification. " +
		"The test generates a dynamic transmitter and waits for a receiver to register a stream. " +
		"The testsuite expects to observe a stream create, verification, deletion request.",
	profile = "OIDSSF",
	configurationFields = {
		"ssf.transmitter.access_token",
		"ssf.stream.audience",
		"ssf.subjects.valid",
		"ssf.subjects.invalid"
	})
@VariantConfigurationFields(parameter = SsfDeliveryMode.class,
	value = "push",
	configurationFields = {
		"ssf.transmitter.push_endpoint_authorization_header"
	})
public class OIDSSFReceiverStreamVerificationTest extends AbstractOIDSSFReceiverTestModule {

	volatile String createdStreamId;

	volatile String verifiedStreamId;

	@Override
	public void start() {
		super.start();
		scheduleTask(new CheckTestFinishedTask(this::isFinished), 10, TimeUnit.SECONDS);
	}

	@Override
	protected void afterStreamCreation(String streamId, JsonObject createResult, JsonElement error) {

		if (createResult == null) {
			return;
		}

		createdStreamId = streamId;
	}

	@Override
	protected boolean isFinished() {
		return createdStreamId != null && createdStreamId.equals(verifiedStreamId);
	}

	@Override
	protected void afterStreamVerificationSuccess(String streamId) {
		verifiedStreamId = streamId;
	}

	@Override
	public void fireTestFinished() {
		eventLog.log(getId(), "Detected created and verified stream.");
		super.fireTestFinished();
	}
}
