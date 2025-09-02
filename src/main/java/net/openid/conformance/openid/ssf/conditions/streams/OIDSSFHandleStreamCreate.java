package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Set;
import java.util.UUID;

public class OIDSSFHandleStreamCreate extends AbstractOIDSSFHandleReceiverRequest {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject resultObj = new JsonObject();
		env.putObject("ssf", "stream_op_result", resultObj);

		String ssfIssuer = env.getString("ssf", "issuer");
		String audience = OIDFJSON.getString(env.getElementFromObject("config", "ssf.stream.audience"));

		JsonObject streamConfigInput;

		try {
			streamConfigInput = env.getElementFromObject("incoming_request", "body_json").getAsJsonObject();
		} catch (Exception e) {
			resultObj.add("error", createErrorObj("parsing_error", e.getMessage()));
			resultObj.addProperty("status_code", 400);
			log("Failed to parse SSF stream config input", args("error", resultObj.get("error")));
			return env;
		}

		JsonObject streamsObj = getOrCreateStreamsObject(env);
		if (!streamsObj.isEmpty()) {
			resultObj.add("error", createErrorObj("conflict", "Only one stream allowed for receiver"));
			resultObj.addProperty("status_code", 409);
			log("Failed to create SSF stream config", args("error", resultObj.get("error")));
			return env;
		}

		Set<String> keysNotAllowedInUpdate = checkForInvalidKeysInStreamConfigInput(streamConfigInput);
		if (!keysNotAllowedInUpdate.isEmpty()) {
			resultObj.add("error", createErrorObj("bad_request", "Found invalid keys for stream config in request body"));
			resultObj.addProperty("status_code", 400);
			log("Found invalid keys for stream config in request body", args("error", resultObj.get("error"), "invalid_keys", keysNotAllowedInUpdate));
			return env;
		}

		JsonObject defaultConfig = env.getElementFromObject("ssf", "default_config").getAsJsonObject();
		Set<String> eventsDelivered = computeEventsDelivered(streamConfigInput, defaultConfig);

		JsonObject streamConfig = streamConfigInput.deepCopy();

		try {
			String streamId = "stream_" + UUID.randomUUID();
			streamConfig.addProperty("stream_id", streamId);
			streamConfig.addProperty("iss", ssfIssuer);
			streamConfig.addProperty("aud", audience);
			streamConfig.add("events_supported", defaultConfig.get("events_supported"));
			streamConfig.add("events_delivered", OIDFJSON.convertSetToJsonArray(eventsDelivered));

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
						log("endpoint_url must be set for urn:ietf:rfc:8935 PUSH delivery", args("error", resultObj.get("error")));
						return env;
					}
					break;
			}

			streamsObj.add(streamId, streamConfig);
			resultObj.add("result", streamConfig);
			resultObj.addProperty("status_code", 201);
			log("Created SSF stream config", args("stream_id", streamId));
		} catch (Exception e) {
			resultObj.add("error", createErrorObj("bad_request", e.getMessage()));
			resultObj.addProperty("status_code", 400);
			log("Failed to create SSF stream config", args("error", resultObj.get("error")));
		}

		// TODO handle 403	if the Event Receiver is not allowed to create a stream
		// see: https://openid.github.io/sharedsignals/openid-sharedsignals-framework-1_0.html#section-8.1.1.1-11

		return env;
	}
}
