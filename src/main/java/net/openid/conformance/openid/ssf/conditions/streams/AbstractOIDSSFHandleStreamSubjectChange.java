package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public abstract class AbstractOIDSSFHandleStreamSubjectChange extends AbstractOIDSSFHandleReceiverRequest {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject resultObj = new JsonObject();
		resultObj.addProperty("change_type", getChangeType());
		env.putObject("ssf", "stream_op_result", resultObj);

		JsonObject streamSubjectInput;
		try {
			streamSubjectInput = env.getElementFromObject("incoming_request", "body_json").getAsJsonObject();
		} catch (Exception e) {
			resultObj.add("error", createErrorObj("parsing_error", e.getMessage()));
			resultObj.addProperty("status_code", 400);
			throw error("Failed to handle stream subject " + getChangeType() + " request: Failed to parse input", args("error", resultObj.get("error")));
		}

		if (!streamSubjectInput.has("stream_id")) {
			resultObj.add("error", createErrorObj("bad_request", "Missing stream_id in request body"));
			resultObj.addProperty("status_code", 400);
			throw error("Failed to handle stream subject " + getChangeType() + " request: Missing stream_id in request body", args("error", resultObj.get("error")));
		}

		JsonElement streamIdEl = streamSubjectInput.get("stream_id");
		if (streamIdEl.isJsonNull()) {
			resultObj.add("error", createErrorObj("not_found", "Stream not found"));
			resultObj.addProperty("status_code", 404);
			throw error("Failed to handle stream subject " + getChangeType() + " request: Stream not found", args("stream_id", streamIdEl, "error", resultObj.get("error")));
		}

		if (!streamSubjectInput.has("subject")) {
			resultObj.add("error", createErrorObj("bad_request", "Missing subject in request body"));
			resultObj.addProperty("status_code", 400);
			throw error("Failed to handle stream subject " + getChangeType() + " request: Missing subject in request body", args("error", resultObj.get("error")));
		}

		JsonElement subjectEl = streamSubjectInput.get("subject");
		if (!subjectEl.isJsonObject()) {
			resultObj.add("error", createErrorObj("bad_request", "subject must be an object"));
			resultObj.addProperty("status_code", 400);
			throw error("Failed to handle stream subject " + getChangeType() + " request: Invalid subject in request body", args("error", resultObj.get("error"), "subject", subjectEl));
		}

		String streamId = OIDFJSON.tryGetString(streamIdEl);
		JsonObject streamsObj = getOrCreateStreamsObject(env);
		if (streamsObj.isEmpty()) {
			resultObj.add("error", createErrorObj("not_found", "Stream not found"));
			resultObj.addProperty("status_code", 404);
			throw error("Failed to handle stream subject " + getChangeType() + " request: No streams configured", args("stream_id", streamId, "error", resultObj.get("error")));
		}

		JsonElement streamConfigEl = OIDSSFStreamUtils.getStreamConfig(env, streamId);
		if (streamConfigEl == null) {
			resultObj.add("error", createErrorObj("not_found", "Stream not found"));
			resultObj.addProperty("status_code", 404);
			throw error("Failed to handle stream subject " + getChangeType() + " request: Stream not found", args("stream_id", streamId, "error", resultObj.get("error")));
		}

		JsonObject streamConfig = streamConfigEl.getAsJsonObject();

		try {
			changeSubjects(streamConfig, streamSubjectInput, resultObj);
		} catch (IllegalArgumentException e) {
			resultObj.add("error", createErrorObj("invalid_subject", e.getMessage()));
			resultObj.addProperty("status_code", 403);
			throw error("Failed to handle stream subject " + getChangeType() + " request: Invalid subject", args("stream_id", streamId, "error", resultObj.get("error")));
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
