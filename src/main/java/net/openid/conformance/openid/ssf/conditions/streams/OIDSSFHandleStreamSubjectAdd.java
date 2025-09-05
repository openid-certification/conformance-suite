package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class OIDSSFHandleStreamSubjectAdd extends AbstractOIDSSFHandleStreamSubjectChange {

	@Override
	protected String getChangeType() {
		return "add";
	}

	@Override
	protected void changeSubjects(JsonObject streamConfig, JsonObject streamSubjectInput, JsonObject resultObj) {
		JsonObject subjectObj = streamSubjectInput.getAsJsonObject("subject");

		if (OIDSSFStreamUtils.getInvalidSubjectExample().equals(subjectObj)) {
			throw new IllegalArgumentException("Invalid subject");
		}

		JsonPrimitive verified = streamSubjectInput.getAsJsonPrimitive("verified");
		OIDSSFStreamUtils.addStreamSubject(streamConfig, subjectObj, verified != null ? verified.getAsBoolean() : null);

//		JsonObject streamSubjects = streamConfig.getAsJsonObject("_subjects");
//
//		List<JsonObject> resultSubjects = new ArrayList<>();
//		JsonArray subjects = streamSubjects.getAsJsonArray("subjects");
//		for (var subject : subjects) {
//			// remove internal _verified field from subject
//			resultSubjects.add(copyConfigObjectWithoutInternalFields(subject.getAsJsonObject()));
//		}

		resultObj.addProperty("status_code", 200);
	}
}
