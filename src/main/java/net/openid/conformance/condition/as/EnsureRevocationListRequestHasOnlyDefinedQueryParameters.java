package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks that the verifier sent no unexpected query parameters when it fetched the revocation
 * list. draft-ietf-oauth-status-list defines exactly one, 'time' for the historical resolution
 * of section 8.4; anything else is not defined by the specification and is most likely a bug in
 * the verifier, such as a misspelling of 'time'.
 */
public class EnsureRevocationListRequestHasOnlyDefinedQueryParameters extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	public Environment evaluate(Environment env) {

		JsonObject query = (JsonObject) env.getElementFromObject(
			"incoming_request", "query_string_params");

		List<String> unexpected = new ArrayList<>();
		if (query != null) {
			for (String name : query.keySet()) {
				if (!"time".equals(name)) {
					unexpected.add(name);
				}
			}
		}

		if (!unexpected.isEmpty()) {
			throw error("The verifier sent query parameters the Token Status List specification does not define when it fetched the revocation list",
				args("unexpected_query_parameters", unexpected, "query_string_params", query));
		}

		logSuccess("The verifier sent no query parameters the Token Status List specification does not define",
			args("query_string_params", query));
		return env;
	}
}
