package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDSSFHandleStreamVerificationRequest extends AbstractOIDSSFHandleReceiverRequest{

	@Override
	public Environment evaluate(Environment env) {

		JsonObject resultObj = new JsonObject();
		env.putObject("ssf", "stream_op_result", resultObj);

		JsonObject verificationInput;
		try {
			verificationInput = env.getElementFromObject("incoming_request", "body_json").getAsJsonObject();
		} catch (Exception e) {
			resultObj.add("error", createErrorObj("parsing_error", e.getMessage()));
			resultObj.addProperty("status_code", 400);
			throw error("Failed to handle stream update request: Failed to parse stream input", args("error", resultObj.get("error")));
		}

		if (!verificationInput.has("stream_id")) {
			resultObj.add("error", createErrorObj("bad_request", "Missing stream_id in request body"));
			resultObj.addProperty("status_code", 400);
			throw error("Failed to handle stream update request: Missing stream_id in request body", args("error", resultObj.get("error")));
		}

		JsonElement streamIdEl = verificationInput.get("stream_id");
		if (streamIdEl.isJsonNull()) {
			resultObj.add("error", createErrorObj("bad_request", "Missing stream_id in request body"));
			resultObj.addProperty("status_code", 400);
			throw error("Failed to handle stream update request: Missing stream_id in request body", args("error", resultObj.get("error")));
		}

		String streamId = OIDFJSON.tryGetString(streamIdEl);
		JsonObject streamsObj = getOrCreateStreamsObject(env);
		if (streamsObj.isEmpty()) {
			resultObj.add("error", createErrorObj("not_found", "Streams not found"));
			resultObj.addProperty("status_code", 404);
			throw error("Failed to handle stream update request: No streams configured", args("error", resultObj.get("error")));
		}

		JsonElement streamConfigEl = OIDSSFStreamUtils.getStreamConfig(env, streamId);
		if (streamConfigEl == null) {
			resultObj.add("error", createErrorObj("not_found", "Streams not found"));
			resultObj.addProperty("status_code", 404);
			throw error("Failed to handle stream update request: Stream not found", args("stream_id", streamId, "error", resultObj.get("error")));
		}

		JsonObject streamConfig = streamConfigEl.getAsJsonObject();

		String verificationState = OIDFJSON.tryGetString(verificationInput.get("state"));
		streamConfig.addProperty("_verification_state", verificationState);

		streamsObj.add(streamId, streamConfig);

		resultObj.add("stream", streamConfig);

		resultObj.addProperty("stream_id", streamId);
		resultObj.addProperty("status_code", 204);
		logSuccess("Handled stream verification request for stream_id=" + streamId, args("stream_id", streamId, "verification_state", verificationState, "verification_request", verificationInput));

		return env;
	}
}
