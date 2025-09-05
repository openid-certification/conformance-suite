package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.HashSet;
import java.util.Set;

import static net.openid.conformance.openid.ssf.SsfConstants.DELIVERY_METHOD_POLL_RFC_8936_URI;
import static net.openid.conformance.openid.ssf.SsfConstants.DELIVERY_METHOD_PUSH_RFC_8935_URI;

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
			log("Failed to handle stream update request: Failed to parse stream input", args("error", resultObj.get("error")));
			return env;
		}

		String streamId = OIDFJSON.tryGetString(streamConfigInput.get("stream_id"));
		if (streamId == null) {
			resultObj.add("error", createErrorObj("bad_request", "Missing stream_id in request body"));
			resultObj.addProperty("status_code", 400);
			log("Failed to handle stream update request: Missing stream_in in request body", args("error", resultObj.get("error")));
			return env;
		}

		JsonObject streamsObj = getOrCreateStreamsObject(env);
		if (streamsObj.isEmpty()) {
			resultObj.add("error", createErrorObj("not_found", "Stream not found"));
			resultObj.addProperty("status_code", 404);
			log("Failed to handle stream update request: No streams configured", args("error", resultObj.get("error")));
			return env;
		}

		JsonElement streamConfigEl = OIDSSFStreamUtils.getStreamConfig(env, streamId);
		if (streamConfigEl == null) {
			log("Failed to handle stream update request: Stream not found", args("stream_id", streamId));
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
			log("Failed to handle stream update request: Found invalid keys", args("error", resultObj.get("error"), "invalid_keys", keysNotAllowedInUpdate));
			return env;
		}

		JsonObject streamConfig = streamConfigEl.getAsJsonObject();

		// Handle updates for events_requested, description, delivery
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
				case DELIVERY_METHOD_POLL_RFC_8936_URI:
					String pollEndpointUrl = env.getString("ssf", "poll_endpoint_url");
					delivery.addProperty("endpoint_url", pollEndpointUrl);
					break;
				case DELIVERY_METHOD_PUSH_RFC_8935_URI:
					JsonElement endpointUrl = delivery.get("endpoint_url");
					if (endpointUrl == null) {
						resultObj.add("error", createErrorObj("bad_request", "endpoint_url must be set for urn:ietf:rfc:8935 PUSH delivery"));
						resultObj.addProperty("status_code", 400);
						log("Failed to handle stream update request: Delivery endpoint_url missing", args("error", resultObj.get("error")));
						return env;
					}
					break;
			}

			streamConfig.add("delivery", delivery);
		}


		streamsObj.add(streamId, streamConfig);

		JsonObject streamConfigResult = copyConfigObjectWithoutInternalFields(streamConfig);

		resultObj.add("result", streamConfigResult);
		resultObj.addProperty("status_code", 200);
		logSuccess("Handled stream update request: Updated stream for stream_id=" + streamId, args("stream_id", streamId, "stream", streamConfigResult));

		return env;
	}
}
