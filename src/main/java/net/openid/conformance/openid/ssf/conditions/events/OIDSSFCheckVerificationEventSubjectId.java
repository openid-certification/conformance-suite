package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDSSFCheckVerificationEventSubjectId extends AbstractCondition {

	@Override
	@PreEnvironment(required = "ssf")
	public Environment evaluate(Environment env) {

		JsonObject claimsJsonObject = env.getElementFromObject("ssf", "verification.token.claims").getAsJsonObject();

		JsonObject subIdObject = claimsJsonObject.getAsJsonObject("sub_id");
		if (subIdObject == null) {
			throw error("Missing sub_id object in verification token", args("token_claims", claimsJsonObject));
		}

		if (!subIdObject.has("format")) {
			throw error("Missing format in sub_id object in verification token", args("token_claims", claimsJsonObject));
		}

		String subjectFormat = OIDFJSON.getString(subIdObject.get("format"));
		if (!"opaque".equals(subjectFormat)) {
			throw error("Invalid subject format", args("expected_format", "opaque", "actual_format", subjectFormat));
		}

		if (!subIdObject.has("id")) {
			throw error("Missing id in sub_id object in verification token", args("token_claims", claimsJsonObject));
		}

		String subjectId = OIDFJSON.getString(subIdObject.get("id"));
		String streamId = OIDFJSON.getString(env.getElementFromObject("ssf", "stream.stream_id"));
		if (!subjectId.equals(streamId)) {
			throw error("Invalid subject id, expected id of the associated stream", args("expected", streamId, "actual", subjectId));
		}

		logSuccess("Successfully verified subject ID", args("sub_id", subIdObject));

		return env;
	}
}
