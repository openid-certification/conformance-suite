package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.openqa.selenium.InvalidArgumentException;

public abstract class AbstractOIDSSFHandleStreamSubjectChange extends AbstractOIDSSFHandleReceiverRequest {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject resultObj = new JsonObject();
		env.putObject("ssf", "stream_op_result", resultObj);

		JsonObject streamSubjectInput;
		try {
			streamSubjectInput = env.getElementFromObject("incoming_request", "body_json").getAsJsonObject();
		} catch (Exception e) {
			resultObj.add("error", createErrorObj("parsing_error", e.getMessage()));
			resultObj.addProperty("status_code", 400);
			log("Failed to handle stream subject " + getChangeType() + " request: Failed to parse stream status input", args("error", resultObj.get("error")));
			return env;
		}

		String streamId = OIDFJSON.tryGetString(streamSubjectInput.get("stream_id"));
		if (streamId == null) {
			resultObj.add("error", createErrorObj("bad_request", "Missing stream_id in request body"));
			resultObj.addProperty("status_code", 400);
			log("Failed to handle stream subject " + getChangeType() + " request: Missing stream_in in stream status update request body", args("error", resultObj.get("error")));
			return env;
		}

		JsonObject streamsObj = getOrCreateStreamsObject(env);
		if (streamsObj.isEmpty()) {
			resultObj.add("error", createErrorObj("not_found", "Stream not found"));
			resultObj.addProperty("status_code", 404);
			log("Failed to handle stream subject " + getChangeType() + " request: No streams configured", args("stream_id", streamId, "error", resultObj.get("error")));
			return env;
		}

		JsonElement streamConfigEl = OIDSSFStreamUtils.getStreamConfig(env, streamId);
		if (streamConfigEl == null) {
			resultObj.add("error", createErrorObj("not_found", "Stream not found"));
			log("Failed to handle stream subject " + getChangeType() + " request: Stream not found", args("stream_id", streamId, "error", resultObj.get("error")));
			resultObj.addProperty("status_code", 404);
			return env;
		}

		JsonObject streamConfig = streamConfigEl.getAsJsonObject();

		try {
			changeSubjects(streamConfig, streamSubjectInput, resultObj);
		} catch (InvalidArgumentException e) {
			resultObj.add("error", createErrorObj("invalid_subject", e.getMessage()));
			log("Failed to handle stream subject " + getChangeType() + " request: Invalid subject", args("stream_id", streamId, "error", resultObj.get("error")));
			resultObj.addProperty("status_code", 403);
			return env;
		}

		// store updated stream subjects
		streamsObj.add(streamId, streamConfig);

		logSuccess("Handled stream subject " + getChangeType() + " request for stream_id=" + streamId, args("stream_id", streamId,
			"subject_input", streamSubjectInput,  "change_type", getChangeType()));

		return env;
	}

	protected abstract String getChangeType();

	protected abstract void changeSubjects(JsonObject streamConfig, JsonObject streamSubjectInput, JsonObject resultObj);
}
