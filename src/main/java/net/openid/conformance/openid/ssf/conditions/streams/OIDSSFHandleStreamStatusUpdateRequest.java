package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFStreamUtils.StreamStatusValue;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDSSFHandleStreamStatusUpdateRequest extends AbstractOIDSSFHandleReceiverRequest {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject resultObj = new JsonObject();
		env.putObject("ssf", "stream_op_result", resultObj);

		JsonElement streamStatusInputEl = env.getElementFromObject("ssf", "stream_status_input");
		if (streamStatusInputEl == null) {
			resultObj.add("error", createErrorObj("parsing_error", "Missing stream status input"));
			resultObj.addProperty("status_code", 400);
			throw error("Failed to handle stream status update request: Failed to parse stream status input", args("error", resultObj.get("error")));
		}
		JsonObject streamStatusInput = streamStatusInputEl.getAsJsonObject();

		String streamId = OIDFJSON.tryGetString(streamStatusInput.get("stream_id"));
		if (streamId == null) {
			resultObj.add("error", createErrorObj("bad_request", "Missing stream_id in request body"));
			resultObj.addProperty("status_code", 400);
			throw error("Failed to handle stream status update request: Missing stream_id in stream status update request body", args("error", resultObj.get("error")));
		}

		JsonObject streamsObj = getOrCreateStreamsObject(env);
		if (streamsObj.isEmpty()) {
			resultObj.add("error", createErrorObj("not_found", "Stream not found"));
			resultObj.addProperty("status_code", 404);
			throw error("Failed to handle stream status update request: No streams configured", args("stream_id", streamId, "error", resultObj.get("error")));
		}

		JsonElement streamConfigEl = OIDSSFStreamUtils.getStreamConfig(env, streamId);
		if (streamConfigEl == null) {
			resultObj.add("error", createErrorObj("not_found", "Stream not found"));
			resultObj.addProperty("status_code", 404);
			throw error("Failed to handle stream status update request: Stream not found", args("stream_id", streamId, "error", resultObj.get("error")));
		}

		JsonObject streamConfig = streamConfigEl.getAsJsonObject();

		StreamStatusValue status;
		try {
			status = StreamStatusValue.valueOf(OIDFJSON.tryGetString(streamStatusInput.get("status")));
		} catch (IllegalArgumentException e) {
			resultObj.add("error", createErrorObj("bad_request", "Invalid stream status input"));
			resultObj.addProperty("status_code", 400);
			throw error("Failed to handle stream status update request: Invalid input", args("stream_id", streamId, "error", resultObj.get("error"), "status", streamStatusInput.get("status")));
		}

		String reason = OIDFJSON.tryGetString(streamStatusInput.get("reason"));
		try {
			OIDSSFStreamUtils.updateStreamStatus(streamConfig, status, reason);
		} catch (IllegalArgumentException e) {
			resultObj.add("error", createErrorObj("bad_request", e.getMessage()));
			resultObj.addProperty("status_code", 400);
			throw error("Failed to handle stream status update request: Invalid status transition", args("stream_id", streamId, "error", resultObj.get("error")));
		}

		// store updated stream status
		streamsObj.add(streamId, streamConfig);

		JsonObject streamStatus = streamConfig.getAsJsonObject("_status");
		resultObj.add("result", streamStatus);

		resultObj.addProperty("stream_id", streamId);
		resultObj.addProperty("status_code", 200);
		logSuccess("Handled update stream status update request for stream_id=" + streamId, args("stream_id", streamId, "status_input", streamStatusInput));

		return env;
	}
}
