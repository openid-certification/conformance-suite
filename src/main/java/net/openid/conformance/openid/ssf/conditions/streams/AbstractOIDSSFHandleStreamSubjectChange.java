package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.openqa.selenium.InvalidArgumentException;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractOIDSSFHandleStreamSubjectChange extends AbstractOIDSSFHandleReceiverRequest {

	public static class OIDSSFHandleStreamSubjectAdd extends AbstractOIDSSFHandleStreamSubjectChange {

		@Override
		protected String getChangeType() {
			return "add";
		}

		@Override
		protected void changeSubjects(JsonObject streamConfig, JsonObject streamSubjectInput) {
			JsonObject subjectObj = streamSubjectInput.getAsJsonObject("subject");


			if (OIDSSFStreamUtils.getInvalidSubjectExample().equals(subjectObj)) {
				throw new IllegalArgumentException("Invalid subject");
			}

			JsonPrimitive verified = streamSubjectInput.getAsJsonPrimitive("verified");
			OIDSSFStreamUtils.addStreamSubject(streamConfig, subjectObj, verified != null ? verified.getAsBoolean() : null);
		}
	}

	public static class OIDSSFHandleStreamSubjectRemove extends AbstractOIDSSFHandleStreamSubjectChange {

		@Override
		protected String getChangeType() {
			return "remove";
		}

		@Override
		protected void changeSubjects(JsonObject streamConfig, JsonObject streamSubjectInput) {
			JsonObject subjectObj = streamSubjectInput.getAsJsonObject("subject");
			OIDSSFStreamUtils.removeStreamSubject(streamConfig, subjectObj);
		}
	}

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

		JsonElement streamConfigEl = env.getElementFromObject("ssf", "streams." + streamId);
		if (streamConfigEl == null) {
			resultObj.add("error", createErrorObj("not_found", "Stream not found"));
			log("Failed to handle stream subject " + getChangeType() + " request: Stream not found", args("stream_id", streamId, "error", resultObj.get("error")));
			resultObj.addProperty("status_code", 404);
			return env;
		}

		JsonObject streamConfig = streamConfigEl.getAsJsonObject();

		try {
			changeSubjects(streamConfig, streamSubjectInput);
		} catch (InvalidArgumentException e) {
			resultObj.add("error", createErrorObj("invalid_subject", e.getMessage()));
			log("Failed to handle stream subject " + getChangeType() + " request: Invalid subject", args("stream_id", streamId, "error", resultObj.get("error")));
			resultObj.addProperty("status_code", 403);
			return env;
		}

		// store updated stream subjects
		streamsObj.add(streamId, streamConfig);

		JsonObject streamSubjects = streamConfig.getAsJsonObject("_subjects");

		List<JsonObject> resultSubjects = new ArrayList<>();
		JsonArray subjects = streamSubjects.getAsJsonArray("subjects");
		for (var subject : subjects) {
			// remove internal _verified field from subject
			resultSubjects.add(copyConfigObjectWithoutInternalFields(subject.getAsJsonObject()));
		}

		resultObj.addProperty("status_code", 200);
		log("Handled stream subject " + getChangeType() + " request for stream_id=" + streamId, args("stream_id", streamId, "stream_subjects", resultSubjects));

		return env;
	}

	protected abstract String getChangeType();

	protected abstract void changeSubjects(JsonObject streamConfig, JsonObject streamSubjectInput);
}
