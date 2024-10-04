package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDSSFCheckVerificationEventSubjectId extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject claimsJsonObject = env.getElementFromObject("ssf", "verification.token.claims").getAsJsonObject();

		JsonObject subIdObject = claimsJsonObject.getAsJsonObject("sub_id");
		if (subIdObject == null) {
			logFailure("Missing sub_id object in claims", args("missing_key", "sub_id", "events_object", claimsJsonObject));
			return env;
		}

		String subjectFormat = OIDFJSON.getString(subIdObject.get("format"));
		if (!"opaque".equals(subjectFormat)) {
			logFailure("Invalid subject format", args("expected_format", "opaque", "actual_format", subjectFormat));
			return env;
		}

		String subjectId = OIDFJSON.getString(subIdObject.get("id"));
		String streamId = OIDFJSON.getString(env.getElementFromObject("ssf", "stream.stream_id"));
		if (!subjectId.equals(streamId)) {
			logFailure("Invalid subject id, expected id of the associated stream", args("expected", streamId, "actual", subjectId));
			return env;
		}

		logSuccess("Successfully verified subject ID", args("sub_id", subIdObject));

		return env;
	}
}
