package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamUtils.StreamStatusValue;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDSSFHandleStreamStatusUpdate extends AbstractOIDSSFHandleReceiverRequest {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject resultObj = new JsonObject();
		env.putObject("ssf", "stream_op_result", resultObj);

		JsonObject streamStatusInput;
		try {
			streamStatusInput = env.getElementFromObject("incoming_request", "body_json").getAsJsonObject();
		} catch (Exception e) {
			resultObj.add("error", createErrorObj("parsing_error", e.getMessage()));
			resultObj.addProperty("status_code", 400);
			log("Failed to handle stream status update request: Failed to parse stream status input", args("error", resultObj.get("error")));
			return env;
		}

		String streamId = OIDFJSON.tryGetString(streamStatusInput.get("stream_id"));
		if (streamId == null) {
			resultObj.add("error", createErrorObj("bad_request", "Missing stream_id in request body"));
			resultObj.addProperty("status_code", 400);
			log("Failed to handle stream status update request: Missing stream_in in stream status update request body", args("error", resultObj.get("error")));
			return env;
		}

		JsonObject streamsObj = getOrCreateStreamsObject(env);
		if (streamsObj.isEmpty()) {
			resultObj.add("error", createErrorObj("not_found", "Stream not found"));
			resultObj.addProperty("status_code", 404);
			log("Failed to handle stream status update request: No streams configured", args("stream_id", streamId, "error", resultObj.get("error")));
			return env;
		}

		JsonElement streamConfigEl = env.getElementFromObject("ssf", "streams." + streamId);
		if (streamConfigEl == null) {
			resultObj.add("error", createErrorObj("not_found", "Stream not found"));
			log("Failed to handle stream status update request: Stream not found", args("stream_id", streamId, "error", resultObj.get("error")));
			resultObj.addProperty("status_code", 404);
			return env;
		}

		JsonObject streamConfig = streamConfigEl.getAsJsonObject();

		StreamStatusValue status;
		try {
			status = StreamStatusValue.valueOf(OIDFJSON.tryGetString(streamStatusInput.get("status")));
		} catch (IllegalArgumentException e) {
			resultObj.add("error", createErrorObj("bad_request", "Invalid stream status input"));
			log("Failed to handle stream status update request: Invalid input", args("stream_id", streamId, "error", resultObj.get("error"), "status", streamStatusInput.get("status")));
			resultObj.addProperty("status_code", 400);
			return env;
		}

		String reason = OIDFJSON.tryGetString(streamStatusInput.get("reason"));
		try {
			OIDSSFStreamUtils.updateStreamStatus(streamConfig, status, reason);
		} catch (IllegalArgumentException e) {
			resultObj.add("error", createErrorObj("bad_request", e.getMessage()));
			log("Failed to handle stream status update request: Invalid status transition", args("stream_id", streamId, "error", resultObj.get("error")));
			resultObj.addProperty("status_code", 400);
			return env;
		}

		// store updated stream status
		streamsObj.add(streamId, streamConfig);

		JsonObject streamStatus = streamConfig.getAsJsonObject("_status");
		resultObj.add("result", streamStatus);

		resultObj.addProperty("status_code", 200);
		log("Handled update stream status update request", args("stream_id", streamId, "status", streamStatus));

		return env;
	}
}
