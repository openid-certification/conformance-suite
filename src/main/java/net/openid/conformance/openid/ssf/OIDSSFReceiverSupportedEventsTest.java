package net.openid.conformance.openid.ssf;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
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
	summary = "This test verifies the receiver stream management. " +
		"The test generates a dynamic transmitter and waits for a receiver to register a stream. ",
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
	protected boolean isFinished() {
		return createdStreamId != null
			&& !eventsEnqueued.isEmpty()
			&& eventsEnqueued.get(createdStreamId) != null
			&& eventsAcked.get(createdStreamId) != null
			&& eventsAcked.get(createdStreamId).containsAll(eventsEnqueued.get(createdStreamId));
	}

	@Override
	public void fireTestFinished() {
		eventLog.log(getId(), "Detected acknowledgements for published events.");
		super.fireTestFinished();
	}
}
