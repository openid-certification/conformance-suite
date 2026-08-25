package net.openid.conformance.openid.ssf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.WaitForOneSecond;
import net.openid.conformance.openid.ssf.conditions.OIDSSFLogSuccessCondition;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureReceiverAcknowledgedAllCaepInteropSubjectFormats;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFEnsureStreamContainsCaepInteropEvent;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFGenerateStreamSET;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamUtils;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
		 * retrieve and acknowledge the requested CAEP events (at least one of 'session-revoked', 'credential-change' and 'device-compliance-change' must be requested)
		Each requested CAEP event is sent once per subject listed in the 'SSF valid SubjectId' field, which must include at least one 'email' and one 'iss_sub' subject, as receivers must accept events with any of the subject identifier formats of the CAEP Interop Profile (section 2.5). 'complex' subjects listed there are sent as well.""",
	profile = "OIDSSF"
)
public class OIDSSFReceiverStreamCaepInteropTest extends AbstractOIDSSFReceiverTestModule {

	private static final Map<String, String> CAEP_INTEROP_EVENT_SPEC_REFS = Map.of( //
		SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE, "CAEPIOP-3.1", //
		SsfEvents.CAEP_CREDENTIAL_CHANGE_EVENT_TYPE, "CAEPIOP-3.2", //
		SsfEvents.CAEP_DEVICE_COMPLIANCE_CHANGE_EVENT_TYPE, "CAEPIOP-3.3" //
	);

	volatile String createdStreamId;

	volatile String readStreamId;

	volatile String readStreamStatusStreamId;

	volatile String verificationStreamId;

	volatile ConcurrentMap<String, Set<String>> eventsAcked;

	volatile ConcurrentMap<String, Set<String>> eventsEnqueued;

	/**
	 * Subject identifier format used for each generated CAEP event, keyed by {@code jti}.
	 * Used to verify that the receiver acknowledged events for every format required by
	 * CAEP Interop Profile §2.5.
	 */
	volatile ConcurrentMap<String, String> subjectFormatByJti;

	volatile boolean caepInteropEventsGenerated;

	@Override
	public void start() {
		super.start();
		eventsAcked = new ConcurrentHashMap<>();
		eventsEnqueued = new ConcurrentHashMap<>();
		subjectFormatByJti = new ConcurrentHashMap<>();
		caepInteropEventsGenerated = false;
		scheduleTask(new CheckTestFinishedTask(this::isFinished), 4, TimeUnit.SECONDS);
	}

	@Override
	public void fireTestFinished() {
		if (createdStreamId != null) {
			callAndContinueOnFailure(new OIDSSFEnsureReceiverAcknowledgedAllCaepInteropSubjectFormats(subjectFormatByJti,
				eventsAcked.getOrDefault(createdStreamId, Set.of())), Condition.ConditionResult.FAILURE, "CAEPIOP-2.5");
		}
		eventLog.log(getName(), "Detected all stream operations required by CAEP Interop Profile.");
		super.fireTestFinished();
	}

	@Override
	protected boolean isFinished() {

		boolean detectedCreateStream = createdStreamId != null;
		if (!detectedCreateStream) {
			return false;
		}

		boolean detectedReadStream = createdStreamId.equals(readStreamId);
		boolean detectedReadStreamStatus = createdStreamId.equals(readStreamStatusStreamId);
		boolean detectedStreamVerification = createdStreamId.equals(verificationStreamId);

		// Events that could not be delivered because the receiver deleted the stream are never
		// acknowledged; waiting for them would stall the test until it times out. Whether the
		// receiver saw every required subject identifier format is the actual verdict and is
		// checked by OIDSSFEnsureReceiverAcknowledgedAllCaepInteropSubjectFormats when the test finishes.
		Set<String> expectedAcks = new LinkedHashSet<>(eventsEnqueued.getOrDefault(createdStreamId, Set.of()));
		expectedAcks.removeAll(getUndeliveredEventJtis());
		boolean detectedAllExpectedAcknowledgedEvents = caepInteropEventsGenerated
			&& eventsAcked.getOrDefault(createdStreamId, Set.of()).containsAll(expectedAcks);

		return detectedReadStream
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
		callAndContinueOnFailure(new OIDSSFEnsureStreamContainsCaepInteropEvent(streamId), Condition.ConditionResult.FAILURE, "CAEPIOP-3");
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
			return;
		}

		// Track non-verification events as acknowledged when successfully pushed
		eventsAcked.computeIfAbsent(streamId, k -> new ConcurrentSkipListSet<>()).add(event.jti());
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

		// Track non-verification events as acknowledged via poll
		eventsAcked.computeIfAbsent(streamId, k -> new ConcurrentSkipListSet<>()).add(jti);
	}

	@Override
	protected void onStreamEventEnqueued(String streamId, String jti) {
		eventsEnqueued.computeIfAbsent(streamId, k -> new ConcurrentSkipListSet<>()).add(jti);
	}

	protected void afterInitialStreamVerification(String streamId, OIDSSFSecurityEvent verificationEvent) {

		// generate the CAEP Interop events requested by the receiver
		callAndStopOnFailure(WaitForOneSecond.class);

		long now = System.currentTimeMillis();

		JsonObject streamConfig = OIDSSFStreamUtils.getStreamConfig(env, streamId);
		Set<String> deliveredCaepInteropEvents = getDeliveredCaepInteropEventTypes(streamConfig);

		// CAEP Interop Profile §2.5: receivers MUST be prepared to accept events with any of the
		// profile's subject identifier formats, so every requested event type is generated once
		// per declared valid subject (covering at least 'email' and 'iss_sub', see getEventSubjects()).
		List<JsonObject> subjects = getEventSubjects();

		for (String eventType : SsfEvents.CAEP_INTEROP_EVENT_TYPES) {
			if (!deliveredCaepInteropEvents.contains(eventType)) {
				eventLog.log(getName(), "Skipping CAEP event '%s' which was not requested by the receiver for stream_id=%s".formatted(eventType, streamId));
				continue;
			}

			for (JsonObject subject : subjects) {
				String subjectFormat = SsfSubjectIdentifiers.getFormat(subject);
				SsfEvent event = generateSsfEventExample(eventType, now);
				var generateSecurityEventToken = new OIDSSFGenerateStreamSET(eventStore, streamId, subject, event,
					(sid, jti) -> {
						subjectFormatByJti.put(jti, subjectFormat);
						onStreamEventEnqueued(sid, jti);
					});
				callAndContinueOnFailure(generateSecurityEventToken, Condition.ConditionResult.WARNING, CAEP_INTEROP_EVENT_SPEC_REFS.get(eventType), "CAEPIOP-2.5");
			}
		}

		caepInteropEventsGenerated = true;

		// if push delivery is used - send out the events immediately
		if (OIDSSFStreamUtils.isPushDelivery(streamConfig)) {
			scheduleTask(new OIDSSFHandlePushDeliveryTask(streamId), 1, java.util.concurrent.TimeUnit.SECONDS);
		}
	}

	protected Set<String> getDeliveredCaepInteropEventTypes(JsonObject streamConfig) {

		if (streamConfig == null || streamConfig.get("events_delivered") == null) {
			return Set.of();
		}

		Set<String> eventTypes = new LinkedHashSet<>(OIDFJSON.convertJsonArrayToList(streamConfig.get("events_delivered").getAsJsonArray()));
		eventTypes.retainAll(SsfEvents.CAEP_INTEROP_EVENT_TYPES);
		return eventTypes;
	}
}
