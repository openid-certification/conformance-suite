package net.openid.conformance.openid.ssf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openid.ssf.conditions.OIDSSFLogSuccessCondition;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFGenerateStreamSET;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantConfigurationFields;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;

@PublishTestModule(
	testName = "openid-ssf-receiver-stream-supported-events",
	displayName = "OpenID Shared Signals Framework: Sends all supported events to the receiver",
	summary = """
		This test verifies the receiver events delivery.
		The test generates a dynamic transmitter and waits for a receiver to register a stream, it will then generate all supported events and expects a positive delivery of the events received.
		Note that if the caep_interop profile is used, only the session-revoked and credential-change events are sent.
		The testsuite expects to observe the following interactions:
		 * create a stream
		 * (generates) events
		 * Acknowledge the events
		 * delete the stream
		""",
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
public class OIDSSFReceiverSupportedEventsTest extends AbstractOIDSSFReceiverTestModule {

	volatile String createdStreamId;

	volatile String deletedStreamId;

	volatile ConcurrentMap<String, Set<String>> eventsAcked;

	volatile ConcurrentMap<String, Set<String>> eventsEnqueued;

	@Override
	public void start() {
		super.start();
		eventsAcked = new ConcurrentHashMap<>();
		eventsEnqueued = new ConcurrentHashMap<>();
		scheduleTask(new CheckTestFinishedTask(this::isFinished), 10, TimeUnit.SECONDS);
	}

	@Override
	protected void onStreamEventAcknowledged(String streamId, String jti, OIDSSFSecurityEvent event) {
		eventsAcked.computeIfAbsent(streamId, k -> new ConcurrentSkipListSet<>()).add(jti);

		if (didReceiveExpectedAcksForAllDeliveredEvents()) {
			callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected expected delivery & ack notifications for stream_id=" + streamId), Condition.ConditionResult.FAILURE);
		}
	}

	@Override
	protected void onStreamEventEnqueued(String streamId, String jti) {
		eventsEnqueued.computeIfAbsent(streamId, k -> new ConcurrentSkipListSet<>()).add(jti);
	}

	@Override
	protected void afterStreamCreation(String streamId, JsonObject createResult, JsonElement error) {

		if (createResult == null) {
			return;
		}

		createdStreamId = streamId;
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream creation for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.1");

		if (createdStreamId != null) {
			scheduleTask(() -> {

				JsonObject validSubject = env.getElementFromObject("config", "ssf.subjects.valid").getAsJsonObject();
				List<String> eventsDelivered = OIDFJSON.convertJsonArrayToList(createResult.getAsJsonObject("result").getAsJsonArray("events_delivered"));

				long timestamp = Instant.now().getEpochSecond();

				for (String eventType : eventsDelivered) {

					SsfEvent event = generateSsfEventExample(eventType, timestamp);

					var generateStreamSET = new OIDSSFGenerateStreamSET(eventStore, createdStreamId, validSubject, event, this::onStreamEventEnqueued);
					callAndContinueOnFailure(generateStreamSET, Condition.ConditionResult.WARNING, event.requirements().toArray(new String[0]));
				}

				return "done";
			}, 1, TimeUnit.SECONDS);
		}
	}

	@Override
	protected void afterStreamDeletion(String streamId, JsonObject deleteResult, JsonElement error) {
		deletedStreamId = streamId;
		callAndContinueOnFailure(new OIDSSFLogSuccessCondition("Detected Stream deletion for stream_id=" + streamId), Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.5");
	}

	@Override
	protected boolean isFinished() {
		return createdStreamId != null
			&& !eventsEnqueued.isEmpty()
			&& eventsEnqueued.get(createdStreamId) != null
			&& eventsAcked.get(createdStreamId) != null
			&& didReceiveExpectedAcksForAllDeliveredEvents()
			&& createdStreamId.equals(deletedStreamId);
	}

	protected boolean didReceiveExpectedAcksForAllDeliveredEvents() {
		return eventsAcked.get(createdStreamId).containsAll(eventsEnqueued.get(createdStreamId));
	}

	@Override
	public void fireTestFinished() {
		eventLog.log(getId(), "Detected acknowledgements for published events.");
		super.fireTestFinished();
	}
}
