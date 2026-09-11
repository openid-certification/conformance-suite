package net.openid.conformance.openid.ssf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openid.ssf.conditions.OIDSSFFindingCondition;
import net.openid.conformance.openid.ssf.conditions.OIDSSFLogSuccessCondition;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFGenerateStreamSET;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFGenerateStreamStatusChangeSET;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamUtils;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamUtils.StreamStatusValue;
import net.openid.conformance.openid.ssf.variant.SsfProfile;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@PublishTestModule(
	testName = "openid-ssf-receiver-transmitter-initiated-status-change",
	displayName = "OpenID Shared Signals Framework: Test Receiver copes with a stream the transmitter pauses and enables again",
	summary = """
		This test verifies that the receiver handles a stream status change the transmitter makes on its own.
		The test generates a dynamic transmitter and waits for a receiver to register a stream and verify it. The transmitter then announces with a stream-updated event that it pauses the stream (SSF 1.0 8.1.5: the event is sent before the stream is stopped), pauses it once the receiver acknowledged the announcement, holds an ordinary event while the stream is paused, and after a while enables the stream again, announcing that with a second stream-updated event; the held event is delivered afterwards.
		The receiver must acknowledge both stream-updated events and the held event (HTTP 202 on PUSH delivery, 'ack' on POLL delivery); rejecting one, reporting it via 'setErrs' or deleting the stream without acknowledging it fails the test. Whether the receiver reads the stream status while it is paused is only logged.
		The testsuite expects to observe the following interactions:
		 * create a stream and verify it
		 * acknowledge the stream-updated event announcing 'paused'
		 * (the stream is paused for about 15 seconds; nothing is delivered)
		 * acknowledge the stream-updated event announcing 'enabled' and the event held while the stream was paused
		 * delete the stream
		""",
	profile = "OIDSSF"
)
@VariantNotApplicable(parameter = SsfProfile.class, values = "caep_interop")
public class OIDSSFReceiverTransmitterInitiatedStatusChangeTest extends AbstractOIDSSFReceiverTestModule {

	/** How long the transmitter keeps the stream paused. */
	protected static final int PAUSE_SECONDS = 15;

	volatile String createdStreamId;

	volatile String verificationStreamId;

	volatile String deletedStreamId;

	volatile String pauseAnnouncementJti;

	volatile String heldEventJti;

	volatile String enableAnnouncementJti;

	volatile boolean streamPaused;

	volatile boolean streamEnabledAgain;

	volatile boolean statusReadWhilePaused;

	/** {@code jti} values of the events this test generated, in generation order. */
	final Set<String> generatedJtis = ConcurrentHashMap.newKeySet();

	final Set<String> ackedJtis = ConcurrentHashMap.newKeySet();

	@Override
	public void start() {
		super.start();
		scheduleTask(new CheckTestFinishedTask(this::isFinished), 5, TimeUnit.SECONDS);
	}

	@Override
	protected boolean isFinished() {
		if (createdStreamId == null || !createdStreamId.equals(deletedStreamId)) {
			return false;
		}
		Set<String> outstanding = new LinkedHashSet<>(generatedJtis);
		outstanding.removeAll(ackedJtis);
		outstanding.removeAll(getResolvedWithoutAckJtis());
		return outstanding.isEmpty();
	}

	@Override
	protected void afterStreamCreation(String streamId, JsonObject createResult, JsonElement error) {
		if (createResult == null || error != null || streamId == null) {
			return;
		}
		createdStreamId = streamId;
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream creation for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.1");
	}

	@Override
	protected void afterStreamDeletion(String streamId, JsonObject deleteResult, JsonElement error) {
		if (error != null || streamId == null) {
			return;
		}
		deletedStreamId = streamId;
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream deletion for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.5");
	}

	@Override
	protected void onStatusStatusLookup(String streamId, JsonObject statusOpResult) {
		if (streamPaused && !streamEnabledAgain && !statusReadWhilePaused) {
			statusReadWhilePaused = true;
			eventLog.log(getName(), args("msg", "The receiver read the stream status while the transmitter had paused the stream", "stream_id", streamId,
				"status", statusOpResult.get("result")));
		}
	}

	@Override
	protected void afterPushDeliverySuccess(String streamId, OIDSSFSecurityEvent event) {
		if (SsfEvents.isVerificationEvent(event.type()) && verificationStreamId == null) {
			verificationStreamId = streamId;
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream Verification via PUSH delivery for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
			announcePause(streamId);
			return;
		}
		onGeneratedEventAcknowledged(streamId, event.jti(), "PUSH");
	}

	@Override
	protected void onStreamEventAcknowledged(String streamId, String jti, OIDSSFSecurityEvent event) {
		if (SsfEvents.isVerificationEvent(event.type()) && verificationStreamId == null) {
			verificationStreamId = streamId;
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream Verification via POLL delivery for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.4.1");
			scheduleAfterStreamVerification(() -> announcePause(streamId));
			return;
		}
		onGeneratedEventAcknowledged(streamId, jti, "POLL");
	}

	/** SSF 1.0 8.1.5: the transmitter announces the pause before it stops the stream. */
	private void announcePause(String streamId) {
		JsonObject streamConfig = OIDSSFStreamUtils.getStreamConfig(env, streamId);
		if (streamConfig == null) {
			return;
		}
		callAndContinueOnFailure(new OIDSSFGenerateStreamStatusChangeSET(eventStore, streamId, StreamStatusValue.paused, "Transmitter maintenance",
				(sid, jti) -> {
					pauseAnnouncementJti = jti;
					generatedJtis.add(jti);
				}),
			Condition.ConditionResult.FAILURE, "OIDSSF-8.1.5");
		if (OIDSSFStreamUtils.isPushDelivery(streamConfig)) {
			schedulePushDelivery(streamId);
		}
	}

	private void onGeneratedEventAcknowledged(String streamId, String jti, String deliveryMethod) {
		if (!generatedJtis.contains(jti)) {
			return;
		}
		ackedJtis.add(jti);
		if (jti.equals(pauseAnnouncementJti)) {
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Receiver acknowledged the stream-updated event announcing 'paused' via " + deliveryMethod + " delivery (jti=" + jti + ")"),
				Condition.ConditionResult.FAILURE, "OIDSSF-8.1.5", acknowledgementRequirement());
			pauseStream(streamId);
		} else if (jti.equals(enableAnnouncementJti)) {
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Receiver acknowledged the stream-updated event announcing 'enabled' via " + deliveryMethod + " delivery (jti=" + jti + ")"),
				Condition.ConditionResult.FAILURE, "OIDSSF-8.1.5", acknowledgementRequirement());
		} else if (jti.equals(heldEventJti)) {
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Receiver acknowledged the event held while the stream was paused via " + deliveryMethod + " delivery (jti=" + jti + ")"),
				Condition.ConditionResult.FAILURE, "OIDSSF-8.1.2.1", acknowledgementRequirement());
		}
	}

	/**
	 * The announcement was acknowledged: the stream is paused, an ordinary event is queued and
	 * held (SSF 1.0 8.1.2.1), and the stream is enabled again after {@link #PAUSE_SECONDS}.
	 */
	private void pauseStream(String streamId) {
		if (streamPaused) {
			return;
		}
		JsonObject streamConfig = OIDSSFStreamUtils.getStreamConfig(env, streamId);
		if (streamConfig == null) {
			return;
		}
		streamPaused = true;
		OIDSSFStreamUtils.updateStreamStatus(streamConfig, StreamStatusValue.paused, "Transmitter maintenance");
		eventLog.log(getName(), args("msg", "The transmitter paused the stream for " + PAUSE_SECONDS + " seconds; events generated meanwhile are held", "stream_id", streamId));

		String eventType = selectSubjectEventType(streamConfig);
		if (eventType != null) {
			SsfEvent event = generateSsfEventExample(eventType, Instant.now().getEpochSecond());
			callAndContinueOnFailure(new OIDSSFGenerateStreamSET(eventStore, streamId, getPrimaryEventSubject(), event, (sid, jti) -> {
					heldEventJti = jti;
					generatedJtis.add(jti);
				}),
				Condition.ConditionResult.WARNING, event.requirements().toArray(new String[0]));
		}

		scheduleTask(() -> {
			enableStreamAgain(streamId);
			return "done";
		}, PAUSE_SECONDS, TimeUnit.SECONDS);
	}

	/** SSF 1.0 8.1.5: the transmitter announces the re-enabling upon re-enabling the stream. */
	private void enableStreamAgain(String streamId) {
		JsonObject streamConfig = OIDSSFStreamUtils.getStreamConfig(env, streamId);
		if (streamConfig == null) {
			eventLog.log(getName(), args("msg", "The receiver deleted the stream while it was paused", "stream_id", streamId));
			return;
		}
		streamEnabledAgain = true;
		OIDSSFStreamUtils.updateStreamStatus(streamConfig, StreamStatusValue.enabled, null);
		callAndContinueOnFailure(new OIDSSFGenerateStreamStatusChangeSET(eventStore, streamId, StreamStatusValue.enabled, "Transmitter maintenance finished",
				(sid, jti) -> {
					enableAnnouncementJti = jti;
					generatedJtis.add(jti);
				}),
			Condition.ConditionResult.FAILURE, "OIDSSF-8.1.5");
		eventLog.log(getName(), args("msg", "The transmitter enabled the stream again; the held event and the announcement are delivered", "stream_id", streamId));
		if (OIDSSFStreamUtils.isPushDelivery(streamConfig)) {
			schedulePushDelivery(streamId);
		}
	}

	/** The first delivered event type that is about a subject rather than about the stream itself. */
	private static String selectSubjectEventType(JsonObject streamConfig) {
		JsonElement eventsDelivered = streamConfig.get("events_delivered");
		if (eventsDelivered == null || !eventsDelivered.isJsonArray()) {
			return null;
		}
		return OIDFJSON.convertJsonArrayToList(eventsDelivered.getAsJsonArray()).stream()
			.filter(eventType -> !SsfEvents.SSF_EVENT_TYPES.contains(eventType))
			.findFirst()
			.orElse(null);
	}

	@Override
	public void fireTestFinished() {
		if (!streamPaused) {
			callAndContinueOnFailure(new OIDSSFFindingCondition("The stream was never paused: the receiver did not acknowledge the stream-updated event announcing the pause"
					+ (pauseAnnouncementJti == null ? " (the announcement was never generated, the stream was not verified)" : "") + "."),
				Condition.ConditionResult.FAILURE, "OIDSSF-8.1.5");
		}
		Set<String> undelivered = getUndeliveredEventJtis();
		Set<String> rejected = new LinkedHashSet<>(getErrorReportedEventJtis());
		rejected.addAll(getRejectedPushEventJtis());
		for (String jti : generatedJtis) {
			if (ackedJtis.contains(jti)) {
				continue;
			}
			String what = jti.equals(heldEventJti) ? "the event held while the stream was paused" : "a stream-updated event announcing the transmitter's status change";
			if (rejected.contains(jti)) {
				callAndContinueOnFailure(new OIDSSFFindingCondition("The receiver rejected " + what + " (jti=" + jti + "). Receivers must accept and acknowledge it."),
					Condition.ConditionResult.FAILURE, "OIDSSF-8.1.5", "OIDSSF-8.1.2.1", acknowledgementRequirement());
			} else if (undelivered.contains(jti)) {
				eventLog.log(getName(), args("msg", "The receiver deleted the stream before " + what + " was delivered", "jti", jti));
			} else {
				callAndContinueOnFailure(new OIDSSFFindingCondition("The receiver never acknowledged " + what + " (jti=" + jti + ") before deleting the stream."),
					Condition.ConditionResult.FAILURE, "OIDSSF-8.1.5", acknowledgementRequirement());
			}
		}
		if (streamEnabledAgain && !statusReadWhilePaused) {
			eventLog.log(getName(), "The receiver did not read the stream status while the stream was paused");
		}
		super.fireTestFinished();
	}
}
