package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDSSFHandleStreamSubjectRemove extends AbstractOIDSSFHandleStreamSubjectChange {

	@Override
	protected String getChangeType() {
		return "remove";
	}

	@Override
	protected void changeSubjects(JsonObject streamConfig, JsonObject streamSubjectInput, JsonObject resultObj) {
		JsonObject subjectObj = streamSubjectInput.getAsJsonObject("subject");
		OIDSSFStreamUtils.removeStreamSubject(streamConfig, subjectObj);

		resultObj.addProperty("stream_id", OIDFJSON.tryGetString(streamConfig.get("stream_id")));
		resultObj.addProperty("status_code", 204);
	}
}
