package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class CreateSdJwtPresentationSubmission extends AbstractCondition {

	@Override
	@PostEnvironment(required = "presentation_submission")
	public Environment evaluate(Environment env) {

		// This is a very basic implementation that just assumes the first input descriptor is one that matches the
		// sd_jwt credential the suite returns
		if (env.getElementFromObject("authorization_request_object", "claims.presentation_definition") == null) {
			throw error("presentation_definition is not in presentation request");
		}
		String id = env.getString("authorization_request_object", "claims.presentation_definition.id");
		JsonElement descArray = env.getElementFromObject("authorization_request_object", "claims.presentation_definition.input_descriptors");
		JsonObject foo = descArray.getAsJsonArray().get(0).getAsJsonObject();

		String descriptKey = OIDFJSON.getString(foo.get("id"));

		String ps = """
			{
				"id": "vFB9qd4_0P-7fWRBBKHZx",
				"definition_id": "%s",
				"descriptor_map": [
					{
						"id": "%s",
						"format": "dc+sd-jwt",
						"path": "$"
					}
				]
			}""".formatted(id, descriptKey);
		JsonObject jsonRoot = JsonParser.parseString(ps).getAsJsonObject();
		env.putObject("presentation_submission", jsonRoot);

		logSuccess("Created SD-JWT presentation_submission", args("presentation_submission", jsonRoot));

		return env;

	}

}
