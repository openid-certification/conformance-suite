package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamUtils.StreamStatusValue;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Set;
import java.util.UUID;

import static net.openid.conformance.openid.ssf.SsfConstants.DELIVERY_METHOD_POLL_RFC_8936_URI;

public class OIDSSFHandleStreamCreateRequest extends AbstractOIDSSFHandleReceiverRequest {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject resultObj = new JsonObject();
		env.putObject("ssf", "stream_op_result", resultObj);

		JsonElement streamConfigInputEl = env.getElementFromObject("ssf", "stream_input");
		if (streamConfigInputEl == null) {
			resultObj.add("error", createErrorObj("bad_request", "Missing stream config "));
			resultObj.addProperty("status_code", 400);
			log("Failed to handle stream creation request: Missing stream config in request body", args("error", resultObj.get("error")));
			return env;
		}

		JsonObject streamConfigInput = streamConfigInputEl.getAsJsonObject();

		JsonObject streamsObj = getOrCreateStreamsObject(env);
		if (!streamsObj.isEmpty()) {
			resultObj.add("error", createErrorObj("conflict", "Only one stream allowed for receiver"));
			resultObj.addProperty("status_code", 409);
			log("Failed to handle stream creation request: Too many streams configured for receiver", args("error", resultObj.get("error")));
			return env;
		}

		Set<String> keysNotAllowedInUpdate = checkForInvalidKeysInStreamConfigInput(streamConfigInput);
		if (!keysNotAllowedInUpdate.isEmpty()) {
			resultObj.add("error", createErrorObj("bad_request", "Found invalid keys for stream config in request body"));
			resultObj.addProperty("status_code", 400);
			log("Failed to handle stream creation request: Found invalid keys for stream in request body", args("error", resultObj.get("error"), "invalid_keys", keysNotAllowedInUpdate));
			return env;
		}

		JsonObject defaultConfig = env.getElementFromObject("ssf", "default_config").getAsJsonObject();
		Set<String> eventsDelivered = computeEventsDelivered(streamConfigInput, defaultConfig);

		String streamId = generateStreamId();
		String ssfIssuer = env.getString("ssf", "issuer");
		String audience = getStreamAudience(env);

		try {
			JsonObject streamConfig = new JsonObject();
			streamConfig.addProperty("stream_id", streamId);
			streamConfig.addProperty("iss", ssfIssuer);
			streamConfig.addProperty("aud", audience);
			streamConfig.add("description", streamConfigInput.get("description"));
			streamConfig.add("events_supported", defaultConfig.get("events_supported"));
			streamConfig.add("events_delivered", OIDFJSON.convertSetToJsonArray(eventsDelivered));
			streamConfig.add("events_requested", streamConfigInput.getAsJsonArray("events_requested"));

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

			// store raw stream config
			streamsObj.add(streamId, streamConfig);

			// remove internal fields from stream config for output
			JsonObject streamConfigResult = copyConfigObjectWithoutInternalFields(streamConfig);

			resultObj.addProperty("stream_id", streamId);
			resultObj.add("result", streamConfigResult);
			resultObj.addProperty("status_code", 201);
			logSuccess("Handled stream creation request for stream_id=" + streamId,
				args("stream_id", streamId, "stream_input", streamConfigInput));
		} catch (Exception e) {
			resultObj.add("error", createErrorObj("bad_request", e.getMessage()));
			resultObj.addProperty("status_code", 400);
			log("Failed to handle stream creation request", args("error", resultObj.get("error")));
		}

		// TODO handle 403	if the Event Receiver is not allowed to create a stream
		// see: https://openid.github.io/sharedsignals/openid-sharedsignals-framework-1_0.html#section-8.1.1.1-11

		return env;
	}

	protected String getStreamAudience(Environment env) {
		return OIDFJSON.getString(env.getElementFromObject("config", "ssf.stream.audience"));
	}

	protected String generateStreamId() {
		return "stream_" + UUID.randomUUID();
	}

}
