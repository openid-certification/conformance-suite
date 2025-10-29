package net.openid.conformance.openid.ssf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.WaitForOneSecond;
import net.openid.conformance.openid.ssf.conditions.OIDSSFLogSuccessCondition;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFGenerateStreamSET;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamUtils;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantConfigurationFields;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;

@PublishTestModule(
	testName = "openid-ssf-receiver-stream-caep-interop",
	displayName = "OpenID Shared Signals Framework: Test CAEP Interop Receiver Stream Management",
	summary = """
		This test verifies the receiver stream management according to the capabilities listed in the CAEP Interop Profile 1.0.
		The test generates a dynamic transmitter and waits for a receiver to register a stream.
		The testsuite expects to observe the following interactions:
		 * create a stream
		 * read the stream configuration
		 * read the stream status
		 * trigger a stream verification
		 * acknowledge the stream verification.
		 * retrieve and acknowledge the CAEP events 'session-revoked' and 'credential-change'""",
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

	volatile ConcurrentMap<String, Set<String>> eventsAcked;

	volatile ConcurrentMap<String, Set<String>> eventsEnqueued;

	@Override
	public void start() {
		super.start();
		eventsAcked = new ConcurrentHashMap<>();
		eventsEnqueued = new ConcurrentHashMap<>();
		scheduleTask(new CheckTestFinishedTask(this::isFinished), 4, TimeUnit.SECONDS);
	}

	@Override
	public void fireTestFinished() {
		eventLog.log(getId(), "Detected all stream operations required by CAEP Interop Profile.");
		super.fireTestFinished();
	}

	@Override
	protected boolean isFinished() {

		boolean detectedCreateStream = createdStreamId != null;
		boolean detectedReadStream = createdStreamId.equals(readStreamId);
		boolean detectedReadStreamStatus = createdStreamId.equals(readStreamStatusStreamId);
		boolean detectedStreamVerification = createdStreamId.equals(verificationStreamId);
		boolean detectedAllExpectedAcknowledgedEvents = eventsAcked.get(createdStreamId) != null
			&& eventsAcked.get(createdStreamId).containsAll(eventsEnqueued.get(createdStreamId));

		return detectedCreateStream
			&& detectedReadStream
			&& detectedReadStreamStatus
			&& detectedStreamVerification
			&& detectedAllExpectedAcknowledgedEvents;
	}

	@Override
	protected void afterStreamCreation(String streamId, JsonObject createResult, JsonElement error) {

		if (createResult == null) {
			return;
		}

		createdStreamId = streamId;
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream creation for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "CAEPIOP-2.3.8.2");
	}

	@Override
	protected void afterStreamLookup(String streamId, JsonObject lookupResult, JsonElement error) {
		readStreamId = streamId;
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream Lookup for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "CAEPIOP-2.3.8.2");
	}

	@Override
	protected void onStatusStatusLookup(String streamId, JsonObject statusOpResult) {
		readStreamStatusStreamId = streamId;
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream Status Lookup for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "CAEPIOP-2.3.8.2");
	}

	@Override
	protected void afterPushDeliverySuccess(String streamId, OIDSSFSecurityEvent event) {
		// needed if SSF Receiver uses push delivery
		if (SsfEvents.isVerificationEvent(event.type()) && verificationStreamId == null) {
			verificationStreamId = streamId;
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream Verification via PUSH delivery for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "CAEPIOP-2.3.8.2");

			afterInitialStreamVerification(streamId, event);
		}
	}

	@Override
	protected void onStreamEventAcknowledged(String streamId, String jti, OIDSSFSecurityEvent event) {
		// needed if SSF Receiver uses poll delivery
		if (SsfEvents.isVerificationEvent(event.type()) && verificationStreamId == null) {
			verificationStreamId = streamId;
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream Verification via POLL delivery for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "CAEPIOP-2.3.8.2");

			afterInitialStreamVerification(streamId, event);
			return;
		}

		eventsAcked.computeIfAbsent(streamId, k -> new ConcurrentSkipListSet<>()).add(jti);
	}

	@Override
	protected void onStreamEventEnqueued(String streamId, String jti) {
		eventsEnqueued.computeIfAbsent(streamId, k -> new ConcurrentSkipListSet<>()).add(jti);
	}

	protected void afterInitialStreamVerification(String streamId, OIDSSFSecurityEvent verificationEvent) {

		// generate CAEP Interop events
		callAndStopOnFailure(WaitForOneSecond.class);

		long now = System.currentTimeMillis();
		JsonObject validSubject = env.getElementFromObject("config", "ssf.subjects.valid").getAsJsonObject();

		SsfEvent sessionRevokedEvent = generateSsfEventExample(SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE, now);
		var generateSecurityEventToken = new OIDSSFGenerateStreamSET(eventStore, streamId, validSubject, sessionRevokedEvent, this::onStreamEventEnqueued);
		callAndContinueOnFailure(generateSecurityEventToken, Condition.ConditionResult.WARNING, "CAEPIOP-3.1");

		SsfEvent credentialChange = generateSsfEventExample(SsfEvents.CAEP_CREDENTIAL_CHANGE_EVENT_TYPE, now);
		generateSecurityEventToken = new OIDSSFGenerateStreamSET(eventStore, streamId, validSubject, credentialChange, this::onStreamEventEnqueued);
		callAndContinueOnFailure(generateSecurityEventToken, Condition.ConditionResult.WARNING, "CAEPIOP-3.2");

		// if push delivery is used - send out the events immediately
		if (OIDSSFStreamUtils.isPushDelivery(OIDSSFStreamUtils.getStreamConfig(env, streamId))) {
			scheduleTask(new OIDSSFHandlePushDeliveryTask(streamId), 1, java.util.concurrent.TimeUnit.SECONDS);
		}
	}
}
