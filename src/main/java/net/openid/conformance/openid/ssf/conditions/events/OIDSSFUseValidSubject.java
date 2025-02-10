package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class OIDSSFUseValidSubject extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject subjectId = getValidSubjectId(env);
		if (subjectId == null) {
			throw error("Missing valid SubjectId");
		}

		env.putObject("ssf","stream.subject", subjectId);

		logSuccess("Using valid Subject ID", args("sub_id", subjectId));

		return env;
	}

	public JsonObject getValidSubjectId(Environment env) {
		JsonElement subjectId = env.getElementFromObject("config", "ssf.subjects.valid");
		if (subjectId == null) {
			return null;
		}
		return subjectId.getAsJsonObject();
	}

	public JsonObject getInvalidSubjectId(Environment env) {
		JsonElement subjectId = env.getElementFromObject("config", "ssf.subjects.invalid");
		if (subjectId == null) {
			return null;
		}
		return subjectId.getAsJsonObject();
	}
}
