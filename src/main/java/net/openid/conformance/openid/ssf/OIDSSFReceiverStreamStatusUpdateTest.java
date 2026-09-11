package net.openid.conformance.openid.ssf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openid.ssf.conditions.OIDSSFFindingCondition;
import net.openid.conformance.openid.ssf.conditions.OIDSSFLogSuccessCondition;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFGenerateStreamStatusUpdatedSETWithUnknownMember;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamUtils;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamUtils.StreamStatusValue;
import net.openid.conformance.openid.ssf.variant.SsfProfile;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@PublishTestModule(
	testName = "openid-ssf-receiver-stream-status-update",
	displayName = "OpenID Shared Signals Framework: Test Receiver Stream Status Management",
	summary = """
		This test verifies the receiver stream status management.
		The test generates a dynamic transmitter and waits for a receiver to register a stream.
		Each time the receiver updates the stream status, the transmitter sends a stream-updated event reporting the new status (SSF 1.0 8.1.5). The event object carries one additional member that no specification defines; receivers must ignore members they do not understand (SSF 1.0 4.2.3), so the event must be accepted like any other.
		If the new status is 'enabled', the receiver must acknowledge the stream-updated event (HTTP 202 on PUSH delivery, 'ack' on POLL delivery); rejecting it, reporting it via 'setErrs' or deleting the stream without acknowledging it fails the test. If the receiver sets 'paused' or 'disabled', the event is queued and held, like all events, until the receiver enables the stream again.
		The testsuite expects to observe the following interactions:
		 * create a stream
		 * update the stream status
		 * acknowledge the stream-updated event (when the stream is enabled)
		 * delete the stream
		""",
	profile = "OIDSSF"
)
@VariantNotApplicable(parameter=SsfProfile.class, values="caep_interop")
public class OIDSSFReceiverStreamStatusUpdateTest extends AbstractOIDSSFReceiverTestModule {

	volatile String createdStreamId;

	volatile String updatedStatusStreamId;

	volatile String deletedStreamId;

	/** {@code jti} values of the stream-updated events generated for the receiver's status updates. */
	final Set<String> streamUpdatedEventJtis = ConcurrentHashMap.newKeySet();

	/** {@code jti} values of the stream-updated events the receiver acknowledged (202 on PUSH, ack on POLL). */
	final Set<String> streamUpdatedEventsAcked = ConcurrentHashMap.newKeySet();

	@Override
	public void start() {
		super.start();
		scheduleTask(new CheckTestFinishedTask(this::isFinished), 4, TimeUnit.SECONDS);
	}

	@Override
	protected boolean isFinished() {
		if (createdStreamId == null
			|| !createdStreamId.equals(deletedStreamId)
			|| !createdStreamId.equals(updatedStatusStreamId)) {
			return false;
		}
		// Stream-updated events for which no acknowledgement can arrive any more (held events
		// purged by the delete, resolved via setErrs, rejected pushes, retrieved but unresolved
		// at deletion) must not stall the test; they are graded in fireTestFinished.
		Set<String> pendingAcks = new LinkedHashSet<>(streamUpdatedEventJtis);
		pendingAcks.removeAll(streamUpdatedEventsAcked);
		pendingAcks.removeAll(getResolvedWithoutAckJtis());
		return pendingAcks.isEmpty();
	}

	@Override
	public void fireTestFinished() {
		eventLog.log(getName(), "Detected all expected stream operations.");
		gradeStreamUpdatedEvents();
		super.fireTestFinished();
	}

	/**
	 * A stream-updated event the receiver retrieved (or was pushed) must have been acknowledged:
	 * a rejection or setErrs report violates the stream-updated event handling (SSF 1.0 8.1.5)
	 * and the rule to ignore unknown members (4.2.3); a missing acknowledgement violates the
	 * delivery method's acknowledgement rule. Events the delete purged before delivery cannot
	 * be assessed.
	 */
	private void gradeStreamUpdatedEvents() {
		Set<String> generated = new LinkedHashSet<>(streamUpdatedEventJtis);
		if (generated.isEmpty()) {
			return;
		}
		Set<String> undelivered = new LinkedHashSet<>(generated);
		undelivered.retainAll(getUndeliveredEventJtis());

		Set<String> rejected = new LinkedHashSet<>(generated);
		rejected.removeAll(undelivered);
		Set<String> resolvedByError = new LinkedHashSet<>(getErrorReportedEventJtis());
		resolvedByError.addAll(getRejectedPushEventJtis());
		rejected.retainAll(resolvedByError);

		Set<String> unacknowledged = new LinkedHashSet<>(generated);
		unacknowledged.removeAll(undelivered);
		unacknowledged.removeAll(rejected);
		unacknowledged.removeAll(streamUpdatedEventsAcked);

		if (!undelivered.isEmpty()) {
			eventLog.log(getName(), args(
				"msg", "The receiver deleted the stream before " + undelivered.size() + " stream-updated event(s) were delivered (the stream was paused or disabled, or not polled again); whether the receiver accepts them cannot be assessed",
				"undelivered_jtis", undelivered));
		}
		if (!rejected.isEmpty()) {
			callAndContinueOnFailure(new OIDSSFFindingCondition(
					"The receiver rejected " + rejected.size() + " stream-updated event(s) reporting the stream status it had requested itself (jtis: " + rejected + "). "
						+ "A receiver must accept a stream-updated event and must ignore members of an event it does not understand, such as the '" + SsfEvents.UNKNOWN_EVENT_MEMBER_NAME + "' member these events carry."),
				Condition.ConditionResult.FAILURE, "OIDSSF-8.1.5", "OIDSSF-4.2.3", acknowledgementRequirement());
		}
		if (!unacknowledged.isEmpty()) {
			callAndContinueOnFailure(new OIDSSFFindingCondition(
					"The receiver retrieved " + unacknowledged.size() + " stream-updated event(s) but never acknowledged them before deleting the stream (jtis: " + unacknowledged + "). "
						+ "Accepted SETs must be acknowledged via 'ack' on POLL delivery or a 202 response on PUSH delivery."),
				Condition.ConditionResult.FAILURE, "OIDSSF-8.1.5", acknowledgementRequirement());
		}
		if (rejected.isEmpty() && unacknowledged.isEmpty() && !streamUpdatedEventsAcked.isEmpty()) {
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("The receiver acknowledged every delivered stream-updated event, including the unknown member '" + SsfEvents.UNKNOWN_EVENT_MEMBER_NAME + "' it carried"),
				Condition.ConditionResult.FAILURE, "OIDSSF-8.1.5", "OIDSSF-4.2.3", acknowledgementRequirement());
		}
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
	protected void onStreamStatusUpdateSuccess(String streamId, JsonElement result) {
		updatedStatusStreamId = streamId;
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream status update for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.2.2");

		JsonObject streamConfig = OIDSSFStreamUtils.getStreamConfig(env, streamId);
		if (streamConfig == null) {
			return;
		}
		StreamStatusValue newStatus = OIDSSFStreamUtils.getStreamStatusValue(streamConfig);

		// Report the new status back with a stream-updated event; the event object carries one
		// member no specification defines, which the receiver has to ignore.
		callAndContinueOnFailure(new OIDSSFGenerateStreamStatusUpdatedSETWithUnknownMember(eventStore, streamId, (sid, jti) -> streamUpdatedEventJtis.add(jti)),
			Condition.ConditionResult.FAILURE, "OIDSSF-8.1.5", "OIDSSF-4.2.3");

		if (!newStatus.isEventDeliveryEnabled()) {
			// SSF 1.0 8.1.2.1: a paused or disabled stream does not transmit events; the base
			// module releases held events once the receiver enables the stream again.
			eventLog.log(getName(), args(
				"msg", "Stream status is now " + newStatus + ": the stream-updated event is queued and held until the receiver enables the stream again",
				"stream_id", streamId, "status", newStatus.name()));
			return;
		}
		if (OIDSSFStreamUtils.isPushDelivery(streamConfig)) {
			schedulePushDelivery(streamId);
		}
	}

	@Override
	protected void afterPushDeliverySuccess(String streamId, OIDSSFSecurityEvent event) {
		if (streamUpdatedEventJtis.contains(event.jti())) {
			onStreamUpdatedEventAcknowledged(streamId, event.jti(), "PUSH");
		}
	}

	@Override
	protected void onStreamEventAcknowledged(String streamId, String jti, OIDSSFSecurityEvent event) {
		if (streamUpdatedEventJtis.contains(jti)) {
			onStreamUpdatedEventAcknowledged(streamId, jti, "POLL");
		}
	}

	/** A rejected push of a stream-updated event is recorded at INFO here and graded once at the end of the test. */
	@Override
	protected Condition.ConditionResult getPushDeliveryRejectionSeverity(OIDSSFSecurityEvent event) {
		if (streamUpdatedEventJtis.contains(event.jti())) {
			return Condition.ConditionResult.INFO;
		}
		return super.getPushDeliveryRejectionSeverity(event);
	}

	private void onStreamUpdatedEventAcknowledged(String streamId, String jti, String deliveryMethod) {
		streamUpdatedEventsAcked.add(jti);
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Receiver acknowledged the stream-updated event via " + deliveryMethod + " delivery for stream_id=" + streamId + " (jti=" + jti + ")"),
			Condition.ConditionResult.FAILURE, "OIDSSF-8.1.5", "OIDSSF-4.2.3", acknowledgementRequirement());
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
}
