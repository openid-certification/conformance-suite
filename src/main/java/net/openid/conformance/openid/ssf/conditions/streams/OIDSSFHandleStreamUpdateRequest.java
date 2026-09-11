package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Set;

import static net.openid.conformance.openid.ssf.SsfConstants.DELIVERY_METHOD_POLL_RFC_8936_URI;

public class OIDSSFHandleStreamUpdateRequest extends AbstractOIDSSFHandleReceiverRequest {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject resultObj = new JsonObject();
		env.putObject("ssf", "stream_op_result", resultObj);

		JsonElement streamConfigInputEl = env.getElementFromObject("ssf", "stream_input");
		if (streamConfigInputEl == null) {
			resultObj.add("error", createErrorObj("bad_request", "Missing stream config "));
			resultObj.addProperty("status_code", 400);
			throw error("Failed to handle stream update request: Failed to parse stream input", args("error", resultObj.get("error")));
		}

		JsonObject streamConfigInput = streamConfigInputEl.getAsJsonObject();

		/*
		 * 8.1.1.3. Updating a Stream's Configuration
		 * The stream_id property MUST be present in the request.
		 */
		if (!streamConfigInput.has("stream_id")) {
			resultObj.add("error", createErrorObj("bad_request", "Missing stream_id in request body"));
			resultObj.addProperty("status_code", 400);
			throw error("Failed to handle stream update request: Missing stream_id in request body", args("error", resultObj.get("error")));
		}

		JsonElement streamIdEl = streamConfigInput.get("stream_id");
		/*
		 * 404	if there is no Event Stream with the given "stream_id" for this Event Receiver
		 */
		if (streamIdEl.isJsonNull()) {
			resultObj.add("error", createErrorObj("not_found", "Stream not found"));
			resultObj.addProperty("status_code", 404);
			throw error("Failed to handle stream update request: Stream not found", args("error", resultObj.get("error")));
		}

		String streamId = OIDFJSON.tryGetString(streamIdEl);
		JsonObject streamsObj = getOrCreateStreamsObject(env);
		if (streamsObj.isEmpty()) {
			resultObj.add("error", createErrorObj("not_found", "Stream not found"));
			resultObj.addProperty("status_code", 404);
			throw error("Failed to handle stream update request: No streams configured", args("error", resultObj.get("error")));
		}

		JsonElement streamConfigEl = OIDSSFStreamUtils.getStreamConfig(env, streamId);
		if (streamConfigEl == null) {
			// SSF 1.0 8.1.1.3, Table 4: 404 is the transmitter's regular answer for a stream_id it
			// does not know, as for a read or delete; a stale id from an earlier run is not graded.
			resultObj.add("error", createErrorObj("not_found", "Stream not found"));
			resultObj.addProperty("status_code", 404);
			log("Handled stream update request: no stream with the given stream_id, answered 404", args("stream_id", streamId));
			return env;
		}

		JsonObject streamConfig = streamConfigEl.getAsJsonObject();

		/*
		 * 8.1.1.3: "Transmitter-Supplied properties besides the stream_id MAY be present,
		 * but they MUST match the expected value. If there is a mismatch, the Transmitter
		 * MUST respond with a 400 error."
		 */
		Set<String> mismatchedKeys = computeMismatchedTransmitterSuppliedProperties(streamConfigInput, streamConfig);
		if (!mismatchedKeys.isEmpty()) {
			resultObj.add("error", createErrorObj("bad_request", "Transmitter-supplied properties in request body do not match the current stream configuration"));
			resultObj.addProperty("status_code", 400);
			throw error("Failed to handle stream update request: Transmitter-supplied properties do not match the expected values", args("error", resultObj.get("error"), "mismatched_keys", mismatchedKeys));
		}

		try {
			// SSF 1.0 8.1.1.3: malformed receiver-supplied values (events_requested,
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

			if (streamConfigInput.has("delivery")) {
				JsonObject delivery = streamConfigInput.getAsJsonObject("delivery");
				if (delivery == null) {
					// If delivery is not set, we use POLL delivery method as fallback
					// see https://openid.github.io/sharedsignals/openid-sharedsignals-framework-1_0.html#section-8.1.1.1-5
					delivery = new JsonObject();
					delivery.addProperty("method", DELIVERY_METHOD_POLL_RFC_8936_URI);
				}

				// if delivery is configured and set to POLL we generate a poll delivery
				String deliveryMethod = OIDFJSON.getString(delivery.get("method"));
				ensureDeliveryMethodSupported(env, deliveryMethod);
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
			}

			streamsObj.add(streamId, streamConfig);

			JsonObject streamConfigResult = copyConfigObjectWithoutInternalFields(streamConfig);

			resultObj.addProperty("stream_id", streamId);
			resultObj.add("result", streamConfigResult);
			resultObj.addProperty("status_code", 200);
			logSuccess("Handled stream update request: Updated stream for stream_id=" + streamId, args("stream_id", streamId, "stream_input", streamConfigInput));

			return env;
		} catch (Exception e) {
			resultObj.add("error", createErrorObj("bad_request", e.getMessage()));
			resultObj.addProperty("status_code", 400);
			throw error("Failed to handle stream update request", args("error", resultObj.get("error")));
		}
	}
}
