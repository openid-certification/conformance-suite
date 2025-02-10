package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;

public class ExtractAuthorizationEndpointResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "original_authorization_endpoint_response")
	@PostEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {

		JsonObject response = env.getObject("original_authorization_endpoint_response");
		env.putObject("authorization_endpoint_response", response);

		// we have to cope with x-www-form-urlencoded arriving as direct_post here, but the rest of the code copes with
		// JSON (arriving in the body of a JWE) as well - so we need to coalesce any items that should be JSON into
		// JSON
		for (String s : List.of("presentation_submission", "vp_token")) {
			if (response.has(s)) {
				String value = OIDFJSON.getString(response.get(s));
				try {
					JsonElement json = JsonParser.parseString(value);
					response.add(s, json);
				} catch (JsonSyntaxException e) {
					// leave it as a string then
				}

			}
		}

		logSuccess("Extracted authorization response", response);

		return env;
	}

}
