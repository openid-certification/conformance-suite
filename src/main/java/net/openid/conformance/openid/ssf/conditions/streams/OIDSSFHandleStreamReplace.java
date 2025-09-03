package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamUtils.StreamStatusValue;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Set;

public class OIDSSFHandleStreamReplace extends AbstractOIDSSFHandleReceiverRequest {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject resultObj = new JsonObject();
		env.putObject("ssf", "stream_op_result", resultObj);

		JsonObject streamConfigInput;
		try {
			streamConfigInput = env.getElementFromObject("incoming_request", "body_json").getAsJsonObject();
		} catch (Exception e) {
			resultObj.add("error", createErrorObj("parsing_error", e.getMessage()));
			resultObj.addProperty("status_code", 400);
			log("Failed to handle stream replacement request: Failed to parse SSF stream config input", args("error", resultObj.get("error")));
			return env;
		}

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

		JsonElement streamConfigEl = env.getElementFromObject("ssf", "streams." + streamId);
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

		if (streamConfigInput.has("delivery")) {
			JsonObject delivery = streamConfigInput.getAsJsonObject("delivery");
			String deliveryMethod = OIDFJSON.getString(delivery.get("method"));
			switch (deliveryMethod) {
				case "urn:ietf:rfc:8936":
					String pollEndpointUrl = env.getString("ssf", "poll_endpoint_url");
					delivery.addProperty("endpoint_url", pollEndpointUrl);
					break;
				case "urn:ietf:rfc:8935":
					JsonElement endpointUrl = delivery.get("endpoint_url");
					if (endpointUrl == null) {
						resultObj.add("error", createErrorObj("bad_request", "endpoint_url must be set for urn:ietf:rfc:8935 PUSH delivery"));
						resultObj.addProperty("status_code", 400);
						log("Failed to handle stream replacement request: endpoint_url must be set for urn:ietf:rfc:8935 PUSH delivery", args("error", resultObj.get("error")));
						return env;
					}
					break;
			}
			streamConfig.add("delivery", delivery);
		}

		OIDSSFStreamUtils.updateStreamStatus(streamConfig, StreamStatusValue.enabled, null);

		streamsObj.add(streamId, streamConfig);
		JsonObject streamConfigResult = copyConfigObjectWithoutInternalFields(streamConfig);

		resultObj.add("result", streamConfigResult);
		resultObj.addProperty("status_code", 200);
		log("Handled stream replacement request: Replaced stream config", args("stream_id", streamId, "stream", streamConfigResult));

		return env;
	}
}
