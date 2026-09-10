package net.openid.conformance.openid.ssf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openid.ssf.conditions.OIDSSFFindingCondition;
import net.openid.conformance.openid.ssf.conditions.OIDSSFLogSuccessCondition;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureReceiverAcknowledgedAllCaepInteropSubjectFormats;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFEnsureStreamContainsCaepInteropEvent;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFGenerateStreamSET;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamUtils;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;

import java.time.Instant;
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
		Each requested CAEP event is sent once per subject listed in the 'SSF valid SubjectId' field, which must include at least one 'email' and one 'iss_sub' subject, as receivers must accept events with any of the subject identifier formats of the CAEP Interop Profile (section 2.5). 'complex' subjects listed there are sent as well; since draft-01 of the profile does not list Complex Subjects (see openid/sharedsignals#351), a receiver rejecting those events is reported as a warning only.
		The testsuite expects to observe the following interactions:
		 * create a stream
		 * read the stream configuration
		 * read the stream status
		 * trigger a stream verification
		 * acknowledge the stream verification
		 * retrieve and acknowledge the requested CAEP events (at least one of the qualifying use cases 'session-revoked', 'credential-change' or 'device-compliance-change' must be requested; 'risk-level-change' is additionally generated when requested, but does not qualify on its own since it is not part of the published CAEP Interop Profile draft-01)
		 * delete the stream""",
	profile = "OIDSSF"
)
public class OIDSSFReceiverStreamCaepInteropTest extends AbstractOIDSSFReceiverTestModule {

	private static final Map<String, String> CAEP_INTEROP_EVENT_SPEC_REFS = Map.of( //
		SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE, "CAEPIOP-3.1", //
		SsfEvents.CAEP_CREDENTIAL_CHANGE_EVENT_TYPE, "CAEPIOP-3.2", //
		SsfEvents.CAEP_DEVICE_COMPLIANCE_CHANGE_EVENT_TYPE, "CAEPIOP-3.3", //
		// risk-level-change is not a use case of the published interop draft-01 (only the
		// WG head defines CAEPIOP-3.4); anchor the generated event at its CAEP 1.0 definition
		SsfEvents.CAEP_RISK_LEVEL_CHANGE_EVENT_TYPE, "OIDCAEP-3.8" //
	);

	volatile String createdStreamId;

	volatile String readStreamId;

	volatile String readStreamStatusStreamId;

	volatile String verificationStreamId;

	volatile String deletedStreamId;

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
		eventLog.log(getName(), "Detected all stream operations required by CAEP Interop Profile.");
		// The subject-format coverage verdict (CAEPIOP 2.5) is separate from the stream
		// operations logged above and may still fail the test here.
		if (createdStreamId != null) {
			callAndContinueOnFailure(new OIDSSFEnsureReceiverAcknowledgedAllCaepInteropSubjectFormats(subjectFormatByJti,
				eventsAcked.getOrDefault(createdStreamId, Set.of())), Condition.ConditionResult.FAILURE, "CAEPIOP-2.5");
		}
		// CAEP Interop Profile 2.4.2: "The Receiver MUST obtain the Transmitter's signing
		// key(s) using the jwks_uri from the Transmitter Configuration Metadata."
		if (isJwksEndpointFetched()) {
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Receiver fetched the transmitter's signing keys from the advertised jwks_uri"),
				Condition.ConditionResult.FAILURE, "CAEPIOP-2.4.2");
		} else {
			callAndContinueOnFailure(new OIDSSFFindingCondition(
					"The receiver never fetched the transmitter's signing keys from the advertised jwks_uri. "
						+ "Receivers must obtain the transmitter's signing key(s) via the jwks_uri to validate event signatures (CAEP Interop Profile 2.4.2)."),
				Condition.ConditionResult.FAILURE, "CAEPIOP-2.4.2");
		}
		super.fireTestFinished();
	}

	/**
	 * Draft-01 of the CAEP Interop Profile (2.5) requires receivers to accept {@code email} and
	 * {@code iss_sub} subjects only; Complex Subjects are expected to be added
	 * (openid/sharedsignals#351) but a receiver rejecting one today is within the profile.
	 * Everything else the receiver rejects stays a FAILURE (RFC 8935 2.2).
	 */
	@Override
	protected Condition.ConditionResult getPushDeliveryRejectionSeverity(OIDSSFSecurityEvent event) {
		if (SsfSubjectIdentifiers.FORMAT_COMPLEX.equals(subjectFormatByJti.get(event.jti()))) {
			eventLog.log(getName(), args(
				"msg", "The receiver did not accept a CAEP event with a Complex Subject; graded as a warning because "
					+ "CAEP Interop Profile draft-01 section 2.5 does not require receivers to accept Complex Subjects (openid/sharedsignals#351)",
				"jti", event.jti(), "event_type", event.type()));
			return Condition.ConditionResult.WARNING;
		}
		return super.getPushDeliveryRejectionSeverity(event);
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

		// Events for which no acknowledgement can arrive any more (never delivered, resolved
		// via setErrs, push delivery rejected, or left unresolved when the receiver deleted
		// the stream) must not stall the test until it times out. Whether the receiver saw
		// every required subject identifier format is the actual verdict and is checked by
		// OIDSSFEnsureReceiverAcknowledgedAllCaepInteropSubjectFormats when the test finishes.
		Set<String> expectedAcks = new LinkedHashSet<>(eventsEnqueued.getOrDefault(createdStreamId, Set.of()));
		expectedAcks.removeAll(getResolvedWithoutAckJtis());
		boolean detectedAllExpectedAcknowledgedEvents = caepInteropEventsGenerated
			&& eventsAcked.getOrDefault(createdStreamId, Set.of()).containsAll(expectedAcks);

		boolean detectedStreamDeletion = createdStreamId.equals(deletedStreamId);

		return detectedReadStream
			&& detectedReadStreamStatus
			&& detectedStreamVerification
			&& detectedAllExpectedAcknowledgedEvents
			&& detectedStreamDeletion;
	}

	@Override
	protected void afterStreamCreation(String streamId, JsonObject createResult, JsonElement error) {

		if (createResult == null) {
			return;
		}

		createdStreamId = streamId;
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream creation for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "CAEPIOP-2.4.5.2");
		callAndContinueOnFailure(new OIDSSFEnsureStreamContainsCaepInteropEvent(streamId), Condition.ConditionResult.FAILURE, "CAEPIOP-3");
	}

	@Override
	protected void afterStreamLookup(String streamId, JsonObject lookupResult, JsonElement error) {
		readStreamId = streamId;
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream Lookup for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "CAEPIOP-2.4.5.2");
	}

	@Override
	protected void onStatusStatusLookup(String streamId, JsonObject statusOpResult) {
		readStreamStatusStreamId = streamId;
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream Status Lookup for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "CAEPIOP-2.4.5.2");
	}

	@Override
	protected void afterPushDeliverySuccess(String streamId, OIDSSFSecurityEvent event) {
		// needed if SSF Receiver uses push delivery
		if (SsfEvents.isVerificationEvent(event.type()) && verificationStreamId == null) {
			verificationStreamId = streamId;
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream Verification via PUSH delivery for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "CAEPIOP-2.4.5.2");

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
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream Verification via POLL delivery for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "CAEPIOP-2.4.5.2");

			scheduleAfterStreamVerification(() -> afterInitialStreamVerification(streamId, event));
			return;
		}

		// Track non-verification events as acknowledged via poll
		eventsAcked.computeIfAbsent(streamId, k -> new ConcurrentSkipListSet<>()).add(jti);
	}

	@Override
	protected void afterStreamDeletion(String streamId, JsonObject deleteResult, JsonElement error) {
		if (error != null || streamId == null) {
			// deletion failed (e.g. 404 for an unknown or already-deleted stream) - do not
			// record it as the successful deletion or reset previously recorded state
			return;
		}
		deletedStreamId = streamId;
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream deletion for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "CAEPIOP-2.4.5.2", "OIDSSF-8.1.1.5");
	}

	@Override
	protected void onStreamEventEnqueued(String streamId, String jti) {
		eventsEnqueued.computeIfAbsent(streamId, k -> new ConcurrentSkipListSet<>()).add(jti);
	}

	protected void afterInitialStreamVerification(String streamId, OIDSSFSecurityEvent verificationEvent) {

		// generate the CAEP Interop events requested by the receiver; event_timestamp is
		// "the number of seconds from 1970-01-01T0:0:0Z" (CAEP 1.0 section 2)
		long now = Instant.now().getEpochSecond();

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
			schedulePushDelivery(streamId);
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
