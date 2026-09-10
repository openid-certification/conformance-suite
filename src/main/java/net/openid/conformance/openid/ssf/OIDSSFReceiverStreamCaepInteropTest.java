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
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
		Receivers must interpret all allowable values of 'change_type' and 'credential_type' in credential-change events and of 'previous_status' and 'current_status' in device-compliance-change events (CAEP Interop Profile 3.2 and 3.3). For the first listed subject the credential-change event is therefore sent once per credential_type listed in CAEP 1.0 (password, pin, x509, fido2-platform, fido2-roaming, fido-u2f, verifiable-credential, phone-voice, phone-sms, app), cycling through the change_type values create, revoke, update and delete, and the device-compliance-change event is sent for both status transitions (compliant to not-compliant and back). Every event carries a non-empty reason_admin.
		Every delivered CAEP event must be acknowledged (HTTP 202 on PUSH delivery, 'ack' on POLL delivery): an event the receiver rejects or reports via 'setErrs' fails the test (a warning for Complex Subjects), as does deleting the stream with retrieved but unacknowledged events.
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

	/** Event type of each generated CAEP event, keyed by {@code jti}, for the finish-time grading. */
	volatile ConcurrentMap<String, String> eventTypeByJti;

	volatile boolean caepInteropEventsGenerated;

	@Override
	public void start() {
		super.start();
		eventsAcked = new ConcurrentHashMap<>();
		eventsEnqueued = new ConcurrentHashMap<>();
		subjectFormatByJti = new ConcurrentHashMap<>();
		eventTypeByJti = new ConcurrentHashMap<>();
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
			gradeDeliveredCaepEvents();
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

			for (int i = 0; i < subjects.size(); i++) {
				JsonObject subject = subjects.get(i);
				String subjectFormat = SsfSubjectIdentifiers.getFormat(subject);
				// CAEP Interop Profile 3.2 / 3.3: receivers MUST interpret all allowable values of
				// change_type, credential_type, previous_status and current_status - covered once,
				// with the first subject; the other subjects cover the subject formats.
				List<SsfEvent> events = i == 0 ? generateCaepInteropEventValueVariants(eventType, now) : List.of(generateSsfEventExample(eventType, now));
				for (SsfEvent event : events) {
					var generateSecurityEventToken = new OIDSSFGenerateStreamSET(eventStore, streamId, subject, event,
						(sid, jti) -> {
							subjectFormatByJti.put(jti, subjectFormat);
							eventTypeByJti.put(jti, eventType);
							onStreamEventEnqueued(sid, jti);
						});
					callAndContinueOnFailure(generateSecurityEventToken, Condition.ConditionResult.WARNING, CAEP_INTEROP_EVENT_SPEC_REFS.get(eventType), "CAEPIOP-2.5");
				}
			}
		}

		caepInteropEventsGenerated = true;

		// if push delivery is used - send out the events immediately
		if (OIDSSFStreamUtils.isPushDelivery(streamConfig)) {
			schedulePushDelivery(streamId);
		}
	}

	/**
	 * One event per allowable value of the event fields a CAEP Interop receiver must interpret
	 * (CAEP Interop Profile 3.2 / 3.3), using the fewest events: credential-change once per
	 * {@code credential_type} of CAEP 1.0 3.3.1, cycling {@code change_type} through its four
	 * values; device-compliance-change once per status transition (CAEP 1.0 3.5.1). A single
	 * example event for every other type.
	 */
	protected List<SsfEvent> generateCaepInteropEventValueVariants(String eventType, long timestamp) {
		SsfEvent example = generateSsfEventExample(eventType, timestamp);
		List<SsfEvent> variants = new ArrayList<>();
		switch (eventType) {
			case SsfEvents.CAEP_CREDENTIAL_CHANGE_EVENT_TYPE -> {
				List<String> changeTypes = SsfEvents.CAEP_CREDENTIAL_CHANGE_TYPES;
				List<String> credentialTypes = SsfEvents.CAEP_CREDENTIAL_TYPES;
				for (int i = 0; i < credentialTypes.size(); i++) {
					String credentialType = credentialTypes.get(i);
					Map<String, Object> data = new LinkedHashMap<>(example.data());
					data.put("credential_type", credentialType);
					data.put("change_type", changeTypes.get(i % changeTypes.size()));
					if (!credentialType.startsWith("fido2")) {
						// the example's FIDO2 authenticator details only fit a FIDO2 credential
						data.remove("fido2_aaguid");
						data.put("friendly_name", "Jane's " + credentialType + " credential");
					}
					variants.add(new SsfEvent(eventType, data, example.requirements()));
				}
			}
			case SsfEvents.CAEP_DEVICE_COMPLIANCE_CHANGE_EVENT_TYPE -> {
				for (String previousStatus : SsfEvents.CAEP_DEVICE_COMPLIANCE_STATUSES) {
					for (String currentStatus : SsfEvents.CAEP_DEVICE_COMPLIANCE_STATUSES) {
						if (previousStatus.equals(currentStatus)) {
							continue;
						}
						Map<String, Object> data = new LinkedHashMap<>(example.data());
						data.put("previous_status", previousStatus);
						data.put("current_status", currentStatus);
						data.put("reason_admin", Map.of("en", "Device compliance changed from " + previousStatus + " to " + currentStatus));
						data.put("reason_user", Map.of("en", "Your device is now " + currentStatus + " with the device policy."));
						variants.add(new SsfEvent(eventType, data, example.requirements()));
					}
				}
			}
			default -> variants.add(example);
		}
		return variants;
	}

	/**
	 * Grades every generated CAEP event the receiver got but did not acknowledge: rejected
	 * pushes and setErrs reports violate the event support the profile requires (a warning for
	 * Complex Subjects, see {@link #getPushDeliveryRejectionSeverity}), events retrieved but
	 * never acknowledged before the delete violate the delivery method's acknowledgement rule.
	 * Events the delete purged before delivery cannot be assessed.
	 */
	private void gradeDeliveredCaepEvents() {
		Set<String> generated = new LinkedHashSet<>(eventsEnqueued.getOrDefault(createdStreamId, Set.of()));
		if (generated.isEmpty()) {
			return;
		}
		Set<String> acked = eventsAcked.getOrDefault(createdStreamId, Set.of());
		Set<String> undelivered = getUndeliveredEventJtis();
		Set<String> resolvedByError = new LinkedHashSet<>(getErrorReportedEventJtis());
		resolvedByError.addAll(getRejectedPushEventJtis());

		Map<String, Set<String>> rejectedByEventType = new LinkedHashMap<>();
		Map<String, Set<String>> rejectedComplexSubjectByEventType = new LinkedHashMap<>();
		Set<String> unacknowledged = new LinkedHashSet<>();
		int deliveredCount = 0;
		for (String jti : generated) {
			if (undelivered.contains(jti)) {
				continue;
			}
			deliveredCount++;
			if (acked.contains(jti)) {
				continue;
			}
			if (resolvedByError.contains(jti)) {
				String eventType = eventTypeByJti.getOrDefault(jti, "unknown");
				boolean complexSubject = SsfSubjectIdentifiers.FORMAT_COMPLEX.equals(subjectFormatByJti.get(jti));
				(complexSubject ? rejectedComplexSubjectByEventType : rejectedByEventType)
					.computeIfAbsent(eventType, k -> new LinkedHashSet<>()).add(jti);
			} else {
				unacknowledged.add(jti);
			}
		}

		for (Map.Entry<String, Set<String>> entry : rejectedByEventType.entrySet()) {
			String eventType = entry.getKey();
			callAndContinueOnFailure(new OIDSSFFindingCondition(
					"The receiver rejected " + entry.getValue().size() + " '" + eventType + "' event(s) (jtis: " + entry.getValue() + "). "
						+ "A receiver must accept every event of the CAEP event types it requested" + allowableValuesHint(eventType) + "."),
				Condition.ConditionResult.FAILURE, CAEP_INTEROP_EVENT_SPEC_REFS.getOrDefault(eventType, "CAEPIOP-3"));
		}
		for (Map.Entry<String, Set<String>> entry : rejectedComplexSubjectByEventType.entrySet()) {
			String eventType = entry.getKey();
			callAndContinueOnFailure(new OIDSSFFindingCondition(
					"The receiver rejected " + entry.getValue().size() + " '" + eventType + "' event(s) with a Complex Subject (jtis: " + entry.getValue() + "). "
						+ "Graded as a warning because draft-01 of the CAEP Interop Profile does not require receivers to accept Complex Subjects."),
				Condition.ConditionResult.WARNING, CAEP_INTEROP_EVENT_SPEC_REFS.getOrDefault(eventType, "CAEPIOP-3"), "CAEPIOP-2.5");
		}
		if (!unacknowledged.isEmpty()) {
			callAndContinueOnFailure(new OIDSSFFindingCondition(
					"The receiver retrieved " + unacknowledged.size() + " of the delivered CAEP events but never acknowledged them before deleting the stream (jtis: " + unacknowledged + "). "
						+ "Accepted SETs must be acknowledged via 'ack' on POLL delivery or a 202 response on PUSH delivery."),
				Condition.ConditionResult.FAILURE, "RFC8936-2.4");
		}
		if (!undelivered.isEmpty()) {
			eventLog.log(getName(), args(
				"msg", "The receiver deleted the stream before " + undelivered.size() + " generated CAEP event(s) were delivered; whether it accepts them cannot be assessed",
				"undelivered_jtis", undelivered));
		}
		if (deliveredCount > 0 && rejectedByEventType.isEmpty() && rejectedComplexSubjectByEventType.isEmpty() && unacknowledged.isEmpty()) {
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("The receiver acknowledged all " + deliveredCount + " delivered CAEP events, including every allowable field value sent"),
				Condition.ConditionResult.FAILURE, "CAEPIOP-3");
		}
	}

	private static String allowableValuesHint(String eventType) {
		return switch (eventType) {
			case SsfEvents.CAEP_CREDENTIAL_CHANGE_EVENT_TYPE -> ", interpreting every allowable value of 'change_type' and 'credential_type'";
			case SsfEvents.CAEP_DEVICE_COMPLIANCE_CHANGE_EVENT_TYPE -> ", interpreting every allowable value of 'previous_status' and 'current_status'";
			default -> "";
		};
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
