package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.HashSet;
import java.util.Set;

public class OIDSSFHandleStreamUpdate extends AbstractOIDSSFHandleReceiverRequest {

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
			log("Failed to parse SSF stream config input", args("error", resultObj.get("error")));
			return env;
		}

		String streamId = OIDFJSON.tryGetString(streamConfigInput.get("stream_id"));
		if (streamId == null) {
			resultObj.add("error", createErrorObj("bad_request", "Missing stream_id in request body"));
			resultObj.addProperty("status_code", 400);
			log("Missing stream_in in request body", args("error", resultObj.get("error")));
			return env;
		}

		JsonObject streamsObj = getOrCreateStreamsObject(env);
		if (streamsObj.isEmpty()) {
			resultObj.add("error", createErrorObj("not_found", "Stream not found"));
			resultObj.addProperty("status_code", 404);
			log("Failed to update SSF stream config", args("error", resultObj.get("error")));
			return env;
		}

		JsonElement streamConfigEl = env.getElementFromObject("ssf", "streams." + streamId);
		if (streamConfigEl == null) {
			log("Could not find stream by stream_id", args("stream_id", streamId));
			resultObj.addProperty("status_code", 404);
			return env;
		}

		Set<String> transmitterSuppliedKeys = new HashSet<>(getTransmitterSuppliedStreamConfigKeys());
		transmitterSuppliedKeys.remove("stream_id"); // ignore stream_id

		Set<String> keysNotAllowedInUpdate = new HashSet<>(transmitterSuppliedKeys);
		keysNotAllowedInUpdate.retainAll(streamConfigInput.keySet());
		if (!keysNotAllowedInUpdate.isEmpty()) {
			resultObj.add("error", createErrorObj("bad_request", "Found invalid keys for stream config update in request body"));
			resultObj.addProperty("status_code", 400);
			log("Found invalid keys for stream config update in request body", args("error", resultObj.get("error"), "invalid_keys", keysNotAllowedInUpdate));
			return env;
		}

		JsonObject streamConfigObj = streamConfigEl.getAsJsonObject();

		// Handle updates for events_requested, description
		if (streamConfigInput.has("description")) {
			streamConfigObj.addProperty("description", OIDFJSON.getString(streamConfigInput.get("description")));
		}

		if (streamConfigInput.has("events_requested")) {
			JsonObject defaultConfig = env.getElementFromObject("ssf", "default_config").getAsJsonObject();
			Set<String> eventsDelivered = computeEventsDelivered(streamConfigInput, defaultConfig);

			streamConfigObj.add("events_requested", streamConfigInput.get("events_requested"));
			streamConfigObj.add("events_delivered", OIDFJSON.convertSetToJsonArray(eventsDelivered));
		}

		streamsObj.add(streamId, streamConfigObj);
		resultObj.add("result", streamConfigObj);
		resultObj.addProperty("status_code", 200);
		log("Updated SSF stream config", args("stream_id", streamId, "stream", streamConfigObj));

		return env;
	}
}
