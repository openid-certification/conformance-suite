package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamUtils.StreamStatusValue;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Set;

import static net.openid.conformance.openid.ssf.SsfConstants.DELIVERY_METHOD_POLL_RFC_8936_URI;

public class OIDSSFHandleStreamReplaceRequest extends AbstractOIDSSFHandleReceiverRequest {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject resultObj = new JsonObject();
		env.putObject("ssf", "stream_op_result", resultObj);

		JsonElement streamConfigInputEl = env.getElementFromObject("ssf", "stream_input");
		if (streamConfigInputEl == null) {
			resultObj.add("error", createErrorObj("bad_request", "Missing stream config "));
			resultObj.addProperty("status_code", 400);
			log("Failed to handle stream replacement request: Failed to parse SSF stream config input", args("error", resultObj.get("error")));
			return env;
		}

		JsonObject streamConfigInput = streamConfigInputEl.getAsJsonObject();

		String streamId = OIDFJSON.tryGetString(streamConfigInput.get("stream_id"));
		if (streamId == null) {
			resultObj.add("error", createErrorObj("bad_request", "Missing stream_id in request body"));
			resultObj.addProperty("status_code", 400);
			log("Failed to handle stream replacement request: Missing stream_in in request body", args("error", resultObj.get("error")));
			return env;
		}

		JsonObject streamsObj = getOrCreateStreamsObject(env);
		if (streamsObj.isEmpty()) {
			resultObj.add("error", createErrorObj("not_found", "Stream not found"));
			resultObj.addProperty("status_code", 404);
			log("Failed to handle stream replacement request: No streams configured", args("error", resultObj.get("error")));
			return env;
		}

		JsonElement streamConfigEl = OIDSSFStreamUtils.getStreamConfig(env, streamId);
		if (streamConfigEl == null) {
			log("Failed to handle stream replacement request: Could not find stream by stream_id", args("stream_id", streamId));
			resultObj.addProperty("status_code", 404);
			return env;
		}

		Set<String> keysNotAllowedInUpdate = checkForInvalidKeysInStreamConfigInput(streamConfigInput);
		if (!keysNotAllowedInUpdate.isEmpty()) {
			resultObj.add("error", createErrorObj("bad_request", "Found invalid keys for stream replacement in request body"));
			resultObj.addProperty("status_code", 400);
			log("Failed to handle stream replacement request: Found invalid keys", args("error", resultObj.get("error"), "invalid_keys", keysNotAllowedInUpdate));
			return env;
		}

		JsonObject streamConfig = streamConfigEl.getAsJsonObject();

		// Handle updates for events_requested, description, delibery
		if (streamConfigInput.has("description")) {
			streamConfig.addProperty("description", OIDFJSON.getString(streamConfigInput.get("description")));
		}

		if (streamConfigInput.has("events_requested")) {
			JsonObject defaultConfig = env.getElementFromObject("ssf", "default_config").getAsJsonObject();
			Set<String> eventsDelivered = computeEventsDelivered(streamConfigInput, defaultConfig);

			streamConfig.add("events_requested", streamConfigInput.get("events_requested"));
			streamConfig.add("events_delivered", OIDFJSON.convertSetToJsonArray(eventsDelivered));
		}

		JsonObject delivery = streamConfigInput.getAsJsonObject("delivery");
		if (delivery == null) {
			// If delivery is not set, we use POLL delivery method as fallback
			// see https://openid.github.io/sharedsignals/openid-sharedsignals-framework-1_0.html#section-8.1.1.1-5
			delivery = new JsonObject();
			delivery.addProperty("method", DELIVERY_METHOD_POLL_RFC_8936_URI);
		}

		// if delivery is configured and set to POLL we generate a poll delivery
		String deliveryMethod = OIDFJSON.getString(delivery.get("method"));
		if (deliveryMethod.equals(DELIVERY_METHOD_POLL_RFC_8936_URI)) {
			String pollEndpointUrl = env.getString("ssf", "poll_endpoint_url");
			String streamPollEndpointUrl = pollEndpointUrl + "?stream_id=" + streamId;
			delivery.addProperty("endpoint_url", streamPollEndpointUrl);
			log("Configured endpoint url for POLL delivery for stream_id=%s".formatted(streamId), args("endpoint_url", streamPollEndpointUrl, "delivery", delivery));
		} else {
			String pushEndpointUrl = OIDFJSON.getString(delivery.get("endpoint_url"));
			log("Found endpoint url for PUSH delivery for stream_id=%s".formatted(streamId), args("endpoint_url", pushEndpointUrl, "delivery", delivery));
		}
		streamConfig.add("delivery", delivery);

		OIDSSFStreamUtils.updateStreamStatus(streamConfig, StreamStatusValue.enabled, null);

		streamsObj.add(streamId, streamConfig);
		JsonObject streamConfigResult = copyConfigObjectWithoutInternalFields(streamConfig);

		resultObj.addProperty("stream_id", streamId);
		resultObj.add("result", streamConfigResult);
		resultObj.addProperty("status_code", 200);
		logSuccess("Handled stream replacement request: Replaced stream for stream_id=" + streamId, args("stream_id", streamId, "stream_input", streamConfigInput));

		return env;
	}
}
