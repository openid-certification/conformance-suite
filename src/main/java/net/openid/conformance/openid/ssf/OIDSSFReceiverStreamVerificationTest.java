package net.openid.conformance.openid.ssf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openid.ssf.conditions.OIDSSFLogSuccessCondition;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantConfigurationFields;

import java.util.concurrent.TimeUnit;

@PublishTestModule(
	testName = "openid-ssf-receiver-stream-verification",
	displayName = "OpenID Shared Signals Framework: Test Receiver Stream Verification",
	summary = """
		This test verifies the receiver stream verification.
		The test generates a dynamic transmitter and waits for a receiver to register a stream.
		The testsuite expects to observe the following interactions:
		 * create a stream
		 * trigger a verification event
		 * acknowledge a verification event (200 OK on PUSH, or explicit ack on POLL)
		 * delete the stream
		""",
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
	})
public class OIDSSFReceiverStreamVerificationTest extends AbstractOIDSSFReceiverTestModule {

	volatile String createdStreamId;

	volatile String verificationStreamId;

	volatile String deletedStreamId;

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
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream creation for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.1");
	}

	@Override
	protected void afterPushDeliverySuccess(String streamId, OIDSSFSecurityEvent event) {
		// needed if SSF Receiver uses push delivery
		if (SsfEvents.isVerificationEvent(event.type())) {
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream Verification via PUSH delivery for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "CAEPIOP-2.3.8.2");
			afterStreamVerification(streamId, event);
		}
	}

	@Override
	protected void onStreamEventAcknowledged(String streamId, String jti, OIDSSFSecurityEvent event) {
		// needed if SSF Receiver uses push delivery
		if (SsfEvents.isVerificationEvent(event.type())) {
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream Verification via POLL delivery for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "CAEPIOP-2.3.8.2");
			afterStreamVerification(streamId, event);
		}
	}

	protected void afterStreamVerification(String streamId, OIDSSFSecurityEvent verificationEvent) {
		verificationStreamId = streamId;
	}

	@Override
	protected void afterStreamDeletion(String streamId, JsonObject deleteResult, JsonElement error) {
		deletedStreamId = streamId;
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream deletion for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.5");
	}

	@Override
	protected boolean isFinished() {
		return createdStreamId != null && createdStreamId.equals(verificationStreamId) && createdStreamId.equals(deletedStreamId);
	}

	@Override
	public void fireTestFinished() {
		eventLog.log(getName(), "Detected all expected stream operations.");
		super.fireTestFinished();
	}
}
