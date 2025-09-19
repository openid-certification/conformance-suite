package net.openid.conformance.openid.ssf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantConfigurationFields;

import java.util.concurrent.TimeUnit;

@PublishTestModule(
	testName = "openid-ssf-receiver-stream-caep-interop",
	displayName = "OpenID Shared Signals Framework: Test CAEP Interop Receiver Stream Management",
	summary = "This test verifies the receiver stream management according to the capabilities listed in the CAEP Interop Profile 1.0. " +
			"The test generates a dynamic transmitter and waits for a receiver to register a stream. " +
			"The testsuite expects to observe a stream create, reading the stream configuration, reading stream status and stream verification.",
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
public class OIDSSFReceiverStreamCaepInteropTest extends AbstractOIDSSFReceiverTestModule {

	volatile String createdStreamId;

	volatile String readStreamId;

	volatile String readStreamStatusStreamId;

	volatile String verificationStreamId;

	@Override
	public void start() {
		super.start();
		scheduleTask(new CheckTestFinishedTask(this::isFinished), 4, TimeUnit.SECONDS);
	}

	@Override
	public void fireTestFinished() {
		eventLog.log(getId(), "Detected all stream operations required by CAEP Interop Profile.");
		super.fireTestFinished();
	}

	@Override
	protected boolean isFinished() {
		return createdStreamId != null
			&& createdStreamId.equals(readStreamId)
			&& createdStreamId.equals(readStreamStatusStreamId)
			&& createdStreamId.equals(verificationStreamId);
	}

	@Override
	protected void afterStreamCreation(String streamId, JsonObject createResult, JsonElement error) {

		if (createResult == null) {
			return;
		}

		createdStreamId = streamId;
	}

	@Override
	protected void afterStreamLookup(String streamId, JsonObject lookupResult, JsonElement error) {
		readStreamId = streamId;
	}

	@Override
	protected void onStatusStatusLookup(String streamId, JsonObject statusOpResult) {
		readStreamStatusStreamId = streamId;
	}

	@Override
	protected void afterPushDeliverySuccess(String streamId, OIDSSFSecurityEvent event) {
		// needed if SSF Receiver uses push delivery
		if (SsfEvents.isVerificationEvent(event.type())) {
			verificationStreamId = streamId;
		}
	}

	@Override
	protected void onStreamEventAcknowledged(String streamId, String jti, OIDSSFSecurityEvent event) {
		// needed if SSF Receiver uses push delivery
		if (SsfEvents.isVerificationEvent(event.type())) {
			verificationStreamId = streamId;
		}
	}
}
