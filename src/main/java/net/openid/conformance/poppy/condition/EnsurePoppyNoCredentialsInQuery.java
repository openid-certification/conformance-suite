package net.openid.conformance.poppy.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;

public class EnsurePoppyNoCredentialsInQuery extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	public Environment evaluate(Environment env) {
		JsonObject query = env.getElementFromObject("incoming_request", "query_string_params") instanceof JsonObject o ? o : new JsonObject();
		List<String> found = new ArrayList<>();
		for (String name : query.keySet()) {
			if ("access_token".equals(name) || "poppy_session".equals(name)) {
				found.add(name);
			}
		}
		if (!found.isEmpty()) {
			throw error("The request carries credentials in the query string. Section 5.6 requires the access token and the Session JWT to be sent in headers only.",
				args("query_string_params", query, "credential_parameters", found));
		}
		logSuccess("The request carries no credentials in the query string");
		return env;
	}
}
