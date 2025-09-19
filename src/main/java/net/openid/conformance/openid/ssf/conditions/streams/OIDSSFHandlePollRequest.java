package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.conditions.OIDSSFEventAckConsumer;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;
import net.openid.conformance.openid.ssf.eventstore.OIDSSFEventStore;
import net.openid.conformance.openid.ssf.eventstore.OIDSSFEventStore.EventsBatch;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;
import java.util.Map;

public class OIDSSFHandlePollRequest extends AbstractOIDSSFHandleReceiverRequest {

	protected final OIDSSFEventStore eventStore;

	protected OIDSSFEventAckConsumer onStreamEventAcknowledged;

	public OIDSSFHandlePollRequest(OIDSSFEventStore eventStore, OIDSSFEventAckConsumer onStreamEventAcknowledged) {
		this.eventStore = eventStore;
		this.onStreamEventAcknowledged = onStreamEventAcknowledged;
	}

	@Override
	public Environment evaluate(Environment env) {

		JsonObject resultObj = new JsonObject();
		env.putObject("ssf", "poll_result", resultObj);

		JsonObject queryParams = env.getElementFromObject("incoming_request", "query_string_params").getAsJsonObject();

		String streamId = OIDFJSON.tryGetString(queryParams.get("stream_id"));

		if (streamId == null) {
			resultObj.add("error", createErrorObj("invalid_request", "Missing stream_id in request parameter"));
			resultObj.addProperty("status_code", 400);
			log("Failed to handle stream poll request: Missing stream_in in request parameter", args("error", resultObj.get("error")));
			return env;
		}

		JsonObject streamsObj = getOrCreateStreamsObject(env);
		if (streamsObj.isEmpty()) {
			resultObj.add("error", createErrorObj("not_found", "Stream not found"));
			resultObj.addProperty("status_code", 404);
			log("Failed to handle stream poll request: No streams configured", args("error", resultObj.get("error")));
			return env;
		}

		JsonElement streamConfigEl = OIDSSFStreamUtils.getStreamConfig(env, streamId);
		if (streamConfigEl == null) {
			log("Failed to handle stream poll request: Stream not found", args("stream_id", streamId));
			resultObj.addProperty("status_code", 404);
			return env;
		}

		JsonObject pollRequestInput;
		try {
			pollRequestInput = env.getElementFromObject("incoming_request", "body_json").getAsJsonObject();
		} catch (Exception e) {
			resultObj.add("error", createErrorObj("parsing_error", e.getMessage()));
			resultObj.addProperty("status_code", 400);
			log("Failed to handle stream poll request: Failed to parse request body", args("error", resultObj.get("error")));
			return env;
		}

		JsonElement returnImmediatelyEl = pollRequestInput.get("returnImmediately");
		JsonElement maxEventsEl = pollRequestInput.get("maxEvents");
		JsonArray ackArrayEl = pollRequestInput.getAsJsonArray("ack");
		JsonElement setErrsEl = pollRequestInput.getAsJsonObject("setErrs");


		JsonObject streamConfig = streamConfigEl.getAsJsonObject();
		// if stream is paused or disabled, don't return events!

		JsonObject setsObject = new JsonObject();

		JsonObject pollResultObj = new JsonObject();
		pollResultObj.add("sets", setsObject);

		if (!OIDSSFStreamUtils.getStreamStatusValue(streamConfig).isEventDeliveryEnabled()) {
			// return empty list
			resultObj.add("result", pollResultObj);
			resultObj.addProperty("status_code", 200);
			return env;
		}

		// process acknowledgements if necessary
		if (ackArrayEl != null && !ackArrayEl.isEmpty()) {
			List<String> acks = OIDFJSON.convertJsonArrayToList(ackArrayEl);
			log("Process acknowledgements for stream events for stream_id=" + streamId, args("ack", acks));
			for (String jti : acks) {
				OIDSSFSecurityEvent ackedEvent = eventStore.registerAckForStreamEvent(streamId, jti);
				onStreamEventAcknowledged.accept(streamId, jti, ackedEvent);
			}
		}

		// process errors if necessary
		if (setErrsEl != null) {
			JsonObject setErrsObj = setErrsEl.getAsJsonObject();
			log("Process errors for stream events for stream_id=" + streamId, args("setErrs", setErrsObj));
			for (Map.Entry<String, JsonElement> entry : setErrsObj.entrySet()) {
				String jti = entry.getKey();
				JsonObject errorObj = entry.getValue().getAsJsonObject();
				eventStore.registerErrorForStreamEvent(streamId, jti, errorObj);
			}
		}

		// retrieve events if necessary
		int maxCount = maxEventsEl != null ? OIDFJSON.getInt(maxEventsEl) : 16;
		if (maxCount > 0) {

			boolean returnImmediately = returnImmediatelyEl != null && OIDFJSON.getBoolean(returnImmediatelyEl);

			log("Deliver stream events for stream_id=" + streamId, args("maxCount", maxCount, "returnImmediately", returnImmediately));
			int maxWaitTimeSeconds = 10;
			boolean waitForEvents = !returnImmediately;

			EventsBatch eventsBatch = eventStore.pollEvents(streamId, maxCount, waitForEvents, maxWaitTimeSeconds);

			// merge jti-SET pairs into the poll response
			// see: https://www.rfc-editor.org/rfc/rfc8936.html#section-2.5
			for (OIDSSFSecurityEvent setEvent : eventsBatch.events()) {
				setsObject.addProperty(setEvent.jti(), setEvent.securityEventToken());
			}

			if (eventsBatch.moreAvailable()) {
				pollResultObj.addProperty("moreAvailable", true);
			}
		}

		logSuccess("Handled stream events polling request for stream_id=" + streamId, args("stream_id", streamId, "polling_request", pollRequestInput));

		resultObj.addProperty("stream_id", streamId);
		resultObj.add("result", pollResultObj);
		resultObj.addProperty("status_code", 200);
		return env;
	}
}
