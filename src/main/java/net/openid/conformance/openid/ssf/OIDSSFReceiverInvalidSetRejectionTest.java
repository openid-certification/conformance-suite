package net.openid.conformance.openid.ssf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.WaitForOneSecond;
import net.openid.conformance.openid.ssf.conditions.OIDSSFFindingCondition;
import net.openid.conformance.openid.ssf.conditions.OIDSSFLogSuccessCondition;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureReceiverRejectedPushDelivery;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFValidatePushDeliveryErrorResponse;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFWarnPushDeliveryErrorCodeMismatch;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFWarnPushDeliveryErrorCodeNotRegistered;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFGenerateTamperedStreamSET;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFGenerateTamperedStreamSET.TamperMode;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandlePushDeliveryToReceiver;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamUtils;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@PublishTestModule(
	testName = "openid-ssf-receiver-invalid-set-rejection",
	displayName = "OpenID Shared Signals Framework: Test Receiver rejects invalid Security Event Tokens",
	summary = """
		This test verifies that the receiver rejects invalid Security Event Tokens.
		The test generates a dynamic transmitter and waits for a receiver to register a stream and verify it; once verified, it delivers four deliberately invalid SETs: one with a corrupted signature, one with a wrong 'iss' claim, one with a wrong 'aud' claim and one signed with a key that is not in the transmitter's JWKS.
		With PUSH delivery the error response to each rejected SET is validated as well: it must be a 400 with an 'application/json' body carrying 'err' and 'description' (RFC 8935 2.3); an 'err' that is not a registered Security Event Token Error Code, or not the code matching the defect ('invalid_key', 'invalid_issuer', 'invalid_audience'; RFC 8935 2.4), raises a warning.
		Note: invalid SETs that were retrieved but neither acknowledged nor reported via 'setErrs' are noted after 60 seconds (reporting via 'setErrs' is a MAY, RFC 8936 2.4); deleting the stream without ever retrieving an invalid SET fails the test, since the rejection behavior was not exercised: keep the stream open and keep polling until the invalid SETs were retrieved. The test still waits for the stream deletion before it finishes.
		The testsuite expects to observe the following interactions:
		 * create a stream
		 * verify the stream
		 * reject each of the four invalid SETs - PUSH delivery: answer the delivery with an error response (RFC 8935 2.3); POLL delivery: do not list their 'jti' values in 'ack', report them via 'setErrs' instead (RFC 8936 2.4)
		 * delete the stream
		""",
	profile = "OIDSSF"
)
public class OIDSSFReceiverInvalidSetRejectionTest extends AbstractOIDSSFReceiverTestModule {

	protected static final int SILENT_DROP_RESOLUTION_TIMEOUT_SECONDS = 60;

	volatile String createdStreamId;

	volatile String verificationStreamId;

	volatile String deletedStreamId;

	volatile JsonObject lastCreateResult;

	volatile boolean invalidSetsGenerated;

	volatile ConcurrentMap<String, TamperMode> invalidSetJtis;

	volatile Set<String> resolvedInvalidSetJtis;

	@Override
	public void start() {
		super.start();
		invalidSetJtis = new ConcurrentHashMap<>();
		resolvedInvalidSetJtis = new ConcurrentSkipListSet<>();
		invalidSetsGenerated = false;
		scheduleTask(new CheckTestFinishedTask(this::isFinished), 10, TimeUnit.SECONDS);
	}

	@Override
	protected boolean isFinished() {
		return createdStreamId != null
			&& createdStreamId.equals(verificationStreamId)
			&& invalidSetsGenerated
			&& resolvedInvalidSetJtis.containsAll(invalidSetJtis.keySet())
			&& invalidSetJtis.size() == TamperMode.values().length
			&& createdStreamId.equals(deletedStreamId);
	}

	@Override
	protected void afterStreamCreation(String streamId, JsonObject createResult, JsonElement error) {
		if (createResult == null) {
			return;
		}
		createdStreamId = streamId;
		lastCreateResult = createResult;
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream creation for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.1");
	}

	@Override
	protected void afterPushDeliverySuccess(String streamId, OIDSSFSecurityEvent event) {
		// PUSH delivery: the first successfully delivered verification event is the
		// cue that the receiver completed stream verification.
		if (SsfEvents.isVerificationEvent(event.type()) && verificationStreamId == null) {
			verificationStreamId = streamId;
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream Verification via PUSH delivery for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
			afterInitialStreamVerification(streamId);
		}
	}

	@Override
	protected void onStreamEventAcknowledged(String streamId, String jti, OIDSSFSecurityEvent event) {
		// POLL delivery: the ack of the verification SET is the cue to start.
		if (SsfEvents.isVerificationEvent(event.type()) && verificationStreamId == null) {
			verificationStreamId = streamId;
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream Verification via POLL delivery for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
			scheduleAfterStreamVerification(() -> afterInitialStreamVerification(streamId));
			return;
		}

		TamperMode tamperMode = invalidSetJtis.get(jti);
		if (tamperMode != null) {
			// record the finding BEFORE marking the jti resolved: isFinished may become true
			// the moment the last jti is resolved, and the finding must be in the log by then
			callAndContinueOnFailure(new OIDSSFFindingCondition(
					"Receiver acknowledged an invalid SET (" + tamperMode.description() + ", jti=" + jti + "). "
						+ "Receivers must validate delivered SETs and must not acknowledge invalid ones."),
				Condition.ConditionResult.FAILURE, requirementsFor(tamperMode, "RFC8936-2.4"));
			resolvedInvalidSetJtis.add(jti);
		}
	}

	@Override
	protected void onStreamEventErrorReported(String streamId, String jti, JsonObject error) {
		super.onStreamEventErrorReported(streamId, jti, error);
		TamperMode tamperMode = invalidSetJtis.get(jti);
		if (tamperMode != null) {
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition(
					"Receiver reported an error for the invalid SET (" + tamperMode.description() + ", jti=" + jti + "): " + error),
				Condition.ConditionResult.FAILURE, "RFC8936-2.4");
			resolvedInvalidSetJtis.add(jti);
		}
	}

	@Override
	protected void afterStreamDeletion(String streamId, JsonObject deleteResult, JsonElement error) {
		if (error != null || streamId == null) {
			// deletion failed (e.g. 404 for an unknown or already-deleted stream) - do not
			// record it as the successful deletion or reset previously recorded state
			return;
		}
		deletedStreamId = streamId;
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream deletion for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.5");
	}

	protected void afterInitialStreamVerification(String streamId) {

		JsonObject streamConfig = OIDSSFStreamUtils.getStreamConfig(env, streamId);
		boolean pushDelivery = OIDSSFStreamUtils.isPushDelivery(streamConfig);

		// 'SSF valid SubjectId' is validated and profile-filtered by OIDSSFResolveEventSubjects
		// at configure time and may hold a list; the receiver must reject the tampered SETs on
		// the SET envelope regardless of which valid subject they carry, so one subject suffices
		// (not a Complex Subject, which the profile lets a receiver reject on its own account).
		JsonObject validSubject = getPrimaryEventSubject();
		SsfEvent ssfEvent = generateSsfEventExample(pickEventType(), Instant.now().getEpochSecond());

		List<OIDSSFSecurityEvent> generatedForPush = new CopyOnWriteArrayList<>();

		for (TamperMode tamperMode : TamperMode.values()) {
			var generateTamperedSet = new OIDSSFGenerateTamperedStreamSET(eventStore, streamId, validSubject, ssfEvent,
				tamperMode, !pushDelivery, event -> {
					invalidSetJtis.put(event.jti(), tamperMode);
					if (pushDelivery) {
						generatedForPush.add(event);
					}
				});
			callAndStopOnFailure(generateTamperedSet, requirementsFor(tamperMode));
		}

		invalidSetsGenerated = true;

		if (pushDelivery) {
			scheduleTask(new PushInvalidSetsTask(streamId, generatedForPush), 1, TimeUnit.SECONDS);
		} else {
			// POLL delivery: silently dropping an invalid SET (neither ack nor setErrs)
			// keeps the outcome unobservable - resolve those as a WARNING after a while.
			scheduleTask(new ResolveSilentlyIgnoredInvalidSetsTask(streamId), SILENT_DROP_RESOLUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		}
	}

	/**
	 * Picks the event type for the invalid SETs: prefer a CAEP interop event type
	 * the stream actually delivers, then any delivered event type, falling back
	 * to session-revoked. The receiver should reject the SETs on the tampered
	 * envelope regardless of the event payload.
	 */
	protected String pickEventType() {
		if (lastCreateResult != null && lastCreateResult.getAsJsonObject("result") != null
			&& lastCreateResult.getAsJsonObject("result").getAsJsonArray("events_delivered") != null) {
			List<String> eventsDelivered = OIDFJSON.convertJsonArrayToList(
				lastCreateResult.getAsJsonObject("result").getAsJsonArray("events_delivered"));
			for (String eventType : eventsDelivered) {
				if (SsfEvents.CAEP_INTEROP_EVENT_TYPES.contains(eventType)) {
					return eventType;
				}
			}
			if (!eventsDelivered.isEmpty()) {
				return eventsDelivered.get(0);
			}
		}
		return SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE;
	}

	protected String[] requirementsFor(TamperMode tamperMode) {
		return switch (tamperMode) {
			case INVALID_SIGNATURE -> new String[] {"CAEPIOP-2.6", "CAEPIOP-2.4.2"};
			case WRONG_ISSUER -> new String[] {"OIDSSF-4.1.6"};
			case WRONG_AUDIENCE -> new String[] {"RFC7519-4.1.3", "RFC8935-2"};
			case UNKNOWN_KID -> new String[] {"CAEPIOP-2.4.2", "CAEPIOP-2.6"};
		};
	}

	/**
	 * The Security Event Token Error Code (RFC 8935 2.4) that describes the defect of a
	 * tampered SET; a receiver reporting a different code raises a warning, since the
	 * registry does not bind one code to one cause.
	 */
	protected String expectedErrorCodeFor(TamperMode tamperMode) {
		return switch (tamperMode) {
			case INVALID_SIGNATURE, UNKNOWN_KID -> OIDSSFWarnPushDeliveryErrorCodeNotRegistered.ERROR_CODE_INVALID_KEY;
			case WRONG_ISSUER -> OIDSSFWarnPushDeliveryErrorCodeNotRegistered.ERROR_CODE_INVALID_ISSUER;
			case WRONG_AUDIENCE -> OIDSSFWarnPushDeliveryErrorCodeNotRegistered.ERROR_CODE_INVALID_AUDIENCE;
		};
	}

	/** The tamper-specific requirement anchors plus a delivery-mode-specific one. */
	protected String[] requirementsFor(TamperMode tamperMode, String deliveryModeRequirement) {
		String[] base = requirementsFor(tamperMode);
		String[] combined = java.util.Arrays.copyOf(base, base.length + 1);
		combined[base.length] = deliveryModeRequirement;
		return combined;
	}

	protected class PushInvalidSetsTask implements Callable<String> {

		protected final String streamId;
		protected final List<OIDSSFSecurityEvent> events;

		protected PushInvalidSetsTask(String streamId, List<OIDSSFSecurityEvent> events) {
			this.streamId = streamId;
			this.events = events;
		}

		@Override
		public String call() throws Exception {
			for (OIDSSFSecurityEvent event : events) {
				if (OIDSSFStreamUtils.getStreamConfig(env, streamId) == null) {
					// The receiver deleted the stream mid-run - a legitimate reaction to
					// receiving invalid SETs. The remaining tampered SETs can no longer be
					// delivered (the push endpoint URL is gone); resolve them so the test
					// can finish, and leave the judgement to the deliveries that happened.
					eventLog.log(getName(), args(
						"msg", "Receiver deleted the stream before all invalid SETs could be pushed; skipping the remaining deliveries",
						"stream_id", streamId,
						"skipped_jtis", events.stream().map(OIDSSFSecurityEvent::jti).filter(jti -> !resolvedInvalidSetJtis.contains(jti)).toList()));
					events.forEach(ev -> resolvedInvalidSetJtis.add(ev.jti()));
					return "done";
				}
				TamperMode tamperMode = invalidSetJtis.get(event.jti());
				// The no-op success consumer: an accepted invalid SET is detected via the
				// response status below, not via the delivery bookkeeping.
				callAndContinueOnFailure(new OIDSSFHandlePushDeliveryToReceiver(streamId, event, (sid, ev) -> {
				}), Condition.ConditionResult.WARNING, "RFC8935-2.3");
				callAndContinueOnFailure(new OIDSSFEnsureReceiverRejectedPushDelivery(tamperMode.description() + ", jti=" + event.jti()),
					Condition.ConditionResult.FAILURE, requirementsFor(tamperMode, "RFC8935-2.3"));
				if (receiverRejectedPushDelivery()) {
					// RFC 8935 2.3 prescribes the shape of the rejection: 400, application/json,
					// a body with 'err' and 'description'. The code itself is only expected, not
					// mandated, per cause (2.4), hence the two warnings.
					callAndContinueOnFailure(OIDSSFValidatePushDeliveryErrorResponse.class, Condition.ConditionResult.FAILURE, "RFC8935-2.3");
					callAndContinueOnFailure(OIDSSFWarnPushDeliveryErrorCodeNotRegistered.class, Condition.ConditionResult.WARNING, "RFC8935-2.4");
					callAndContinueOnFailure(new OIDSSFWarnPushDeliveryErrorCodeMismatch(expectedErrorCodeFor(tamperMode)),
						Condition.ConditionResult.WARNING, requirementsFor(tamperMode, "RFC8935-2.4"));
				}
				resolvedInvalidSetJtis.add(event.jti());
				// pace the deliveries with the test lock released (see the base push task)
				callAndContinueOnFailure(WaitForOneSecond.class, Condition.ConditionResult.INFO);
				if (Set.of(Status.FINISHED, Status.INTERRUPTED).contains(getStatus())) {
					return "done";
				}
			}
			return "done";
		}
	}

	/**
	 * Whether the last push delivery was answered with an error status. The shape of a rejection
	 * is only checked for an actual rejection; an accepted invalid SET is graded once, by
	 * {@link OIDSSFEnsureReceiverRejectedPushDelivery}.
	 */
	protected boolean receiverRejectedPushDelivery() {
		Integer status = env.getInteger("endpoint_response", "status");
		return status != null && status != 0 && (status < 200 || status >= 300);
	}

	/**
	 * The receiver deleted the stream with invalid SETs it never retrieved: the rejection under
	 * test was never exercised, which is a failure with a reconfiguration hint, not a warning.
	 */
	@Override
	protected void onEventsUndeliverable(String streamId, List<OIDSSFSecurityEvent> events) {
		super.onEventsUndeliverable(streamId, events);
		for (OIDSSFSecurityEvent event : events) {
			TamperMode tamperMode = invalidSetJtis.get(event.jti());
			if (tamperMode == null || resolvedInvalidSetJtis.contains(event.jti())) {
				continue;
			}
			resolvedInvalidSetJtis.add(event.jti());
			callAndContinueOnFailure(new OIDSSFFindingCondition(
					"Receiver deleted the stream without ever retrieving the invalid SET (" + tamperMode.description() + ", jti=" + event.jti() + "), "
						+ "so its rejection could not be assessed. Keep the stream open and keep polling until the invalid SETs were retrieved and rejected, then delete it."),
				Condition.ConditionResult.FAILURE, "RFC8936-2.4");
		}
	}

	protected class ResolveSilentlyIgnoredInvalidSetsTask implements Callable<String> {

		protected final String streamId;

		protected ResolveSilentlyIgnoredInvalidSetsTask(String streamId) {
			this.streamId = streamId;
		}

		@Override
		public String call() throws Exception {
			// getUndeliveredEventJtis() covers SETs that were still queued when the receiver
			// deleted the stream (the deletion purged the event store, so getQueuedEvents
			// alone would come back empty and misgrade them as retrieved-but-silent)
			Set<String> stillQueuedJtis = new HashSet<>(getUndeliveredEventJtis());
			eventStore.getQueuedEvents(streamId).forEach(event -> stillQueuedJtis.add(event.jti()));

			for (var entry : invalidSetJtis.entrySet()) {
				if (resolvedInvalidSetJtis.contains(entry.getKey())) {
					continue;
				}
				if (stillQueuedJtis.contains(entry.getKey())) {
					// Not retrieved yet: the rejection cannot be assessed until the receiver
					// polls the SET. Graded when the stream is deleted, see onEventsUndeliverable.
					eventLog.log(getName(), args("msg", "Receiver has not retrieved the invalid SET within " + SILENT_DROP_RESOLUTION_TIMEOUT_SECONDS
							+ " seconds; its rejection is assessed once retrieved, and a stream deletion before that fails the test",
						"defect", entry.getValue().description(), "jti", entry.getKey()));
					continue;
				}
				resolvedInvalidSetJtis.add(entry.getKey());
				// Retrieved but neither acknowledged nor reported: not acknowledging an
				// invalid SET is correct, and reporting it via 'setErrs' is a MAY
				// (RFC 8936 2.4) - note it at INFO so the silent rejection is visible.
				callAndContinueOnFailure(new OIDSSFFindingCondition(
						"Receiver retrieved the invalid SET (" + entry.getValue().description() + ", jti=" + entry.getKey() + ") but neither acknowledged it "
							+ "nor reported it via 'setErrs' within " + SILENT_DROP_RESOLUTION_TIMEOUT_SECONDS + " seconds. Not acknowledging is correct; "
							+ "reporting via 'setErrs' (RFC 8936 2.4, MAY) would make the rejection observable."),
					Condition.ConditionResult.INFO, "RFC8936-2.4");
			}
			return "done";
		}
	}

	@Override
	public void fireTestFinished() {
		eventLog.log(getName(), "Detected outcomes for all delivered invalid SETs.");
		super.fireTestFinished();
	}
}
