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

		String rawStreamInput = env.getString("ssf", "stream_input_raw");

		JsonElement streamConfigInputEl = env.getElementFromObject("ssf", "stream_input");
		if (streamConfigInputEl == null) {
			resultObj.add("error", createErrorObj("bad_request", "Missing stream config "));
			resultObj.addProperty("status_code", 400);
			throw error("Failed to handle stream replacement request: Failed to parse SSF stream config input", args("error", resultObj.get("error"), "unparsed_stream_config", rawStreamInput));
		}

		JsonObject streamConfigInput = streamConfigInputEl.getAsJsonObject();
		/*
		 * The stream_id and the full set of Receiver-Supplied properties MUST be present in the PUT body, not only those specifically intended to be changed.
		 */
		if (!streamConfigInput.has("stream_id")) {
			resultObj.add("error", createErrorObj("bad_request", "Missing stream_id in request body"));
			resultObj.addProperty("status_code", 400);
			throw error("Failed to handle stream replacement request: Missing stream_id in request body", args("error", resultObj.get("error")));
		}

		JsonElement streamIdEl = streamConfigInput.get("stream_id");
		if (streamIdEl.isJsonNull()) {
			resultObj.add("error", createErrorObj("not_found", "Stream not found"));
			resultObj.addProperty("status_code", 404);
			throw error("Failed to handle stream replacement request: Stream not found", args("stream_id", streamIdEl));
		}

		String streamId = OIDFJSON.tryGetString(streamIdEl);

		JsonObject streamsObj = getOrCreateStreamsObject(env);
		if (streamsObj.isEmpty()) {
			resultObj.add("error", createErrorObj("not_found", "Stream not found"));
			resultObj.addProperty("status_code", 404);
			throw error("Failed to handle stream replacement request: No streams configured", args("error", resultObj.get("error")));
		}

		JsonElement streamConfigEl = OIDSSFStreamUtils.getStreamConfig(env, streamId);
		if (streamConfigEl == null) {
			resultObj.add("error", createErrorObj("not_found", "Stream not found"));
			resultObj.addProperty("status_code", 404);
			throw error("Failed to handle stream replacement request: Stream not found", args("stream_id", streamId));
		}

		JsonObject streamConfig = streamConfigEl.getAsJsonObject();

		/*
		 * 8.1.1.4: "Event Receivers MAY read the configuration first, modify the JSON,
		 * then PUT it back" — so transmitter-supplied properties MAY be echoed, but per
		 * 8.1.1.3 they MUST match the expected value; on mismatch respond with 400.
		 */
		Set<String> mismatchedKeys = computeMismatchedTransmitterSuppliedProperties(streamConfigInput, streamConfig);
		if (!mismatchedKeys.isEmpty()) {
			resultObj.add("error", createErrorObj("bad_request", "Transmitter-supplied properties in request body do not match the current stream configuration"));
			resultObj.addProperty("status_code", 400);
			throw error("Failed to handle stream replacement request: Transmitter-supplied properties do not match the expected values (SSF 1.0 8.1.1.3)", args("error", resultObj.get("error"), "mismatched_keys", mismatchedKeys));
		}

		try {
			// SSF 1.0 8.1.1.4: malformed receiver-supplied values (events_requested,
			// delivery, description) yield a 400, never a 500.
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
		} catch (Exception e) {
			resultObj.add("error", createErrorObj("bad_request", e.getMessage()));
			resultObj.addProperty("status_code", 400);
			throw error("Failed to handle stream replacement request", args("error", resultObj.get("error")));
		}
	}
}
