package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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
			throw new IllegalArgumentException("Invalid subject");
		}

		JsonPrimitive verified = streamSubjectInput.getAsJsonPrimitive("verified");
		OIDSSFStreamUtils.addStreamSubject(streamConfig, subjectObj, verified != null ? OIDFJSON.getBoolean(verified) : null);

		resultObj.addProperty("stream_id", OIDFJSON.tryGetString(streamConfig.get("stream_id")));
		resultObj.addProperty("status_code", 200);
	}
}
