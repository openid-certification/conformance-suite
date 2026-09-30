package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDSSFHandleStreamSubjectAdd extends AbstractOIDSSFHandleStreamSubjectChange {

	@Override
	protected String getChangeType() {
		return "add";
	}

	@Override
	protected void changeSubjects(JsonObject streamConfig, JsonObject streamSubjectInput, JsonObject resultObj) {
		JsonObject subjectObj = streamSubjectInput.getAsJsonObject("subject");

		if (OIDSSFStreamUtils.getInvalidSubjectExample().equals(subjectObj)) {
			log("Rejecting attempt to add invalid subject", args("subject", subjectObj));
			throw new IllegalArgumentException("Invalid subject");
		}

		JsonElement verifiedEl = streamSubjectInput.get("verified");
		if (verifiedEl != null && !verifiedEl.isJsonNull() && !(verifiedEl.isJsonPrimitive() && verifiedEl.getAsJsonPrimitive().isBoolean())) {
			resultObj.add("error", createErrorObj("bad_request", "'verified' must be a boolean"));
			resultObj.addProperty("status_code", 400);
			throw error("Failed to handle stream subject add request: 'verified' is not a boolean", args("error", resultObj.get("error"), "verified", verifiedEl));
		}
		Boolean verified = verifiedEl == null || verifiedEl.isJsonNull() ? null : OIDFJSON.getBoolean(verifiedEl);
		OIDSSFStreamUtils.addStreamSubject(streamConfig, subjectObj, verified);

		resultObj.addProperty("stream_id", OIDFJSON.tryGetString(streamConfig.get("stream_id")));
		resultObj.addProperty("status_code", 200);
	}
}
