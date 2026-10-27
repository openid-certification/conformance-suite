package net.openid.conformance.authzen.condition;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public abstract class AbstractSetAuthzenApiEndpoint extends AbstractCondition {

	public Environment setAuthzenApiEndpoint(Environment env, String endpointName, String endpointConfigPath) {
		String endpointUrl = stringOrNull(env, endpointConfigPath);
		if(Strings.isNullOrEmpty(endpointUrl)) { // use default if not set
			endpointUrl = stringOrNull(env, "policy_decision_point");
			if(Strings.isNullOrEmpty(endpointUrl)) {
				throw error("'PDP Identifier' field is missing from the 'AuthZEN' section in the test configuration");
			}
			if(!endpointUrl.endsWith("/")) {
				endpointUrl += "/";
			}
			endpointUrl += "access/v1/";

			switch (endpointConfigPath) {
				case "access_evaluation_endpoint":
					endpointUrl += "evaluation";
					break;
				case "access_evaluations_endpoint":
					endpointUrl += "evaluations";
					break;
				case "search_subject_endpoint":
					endpointUrl += "search/subject";
					break;
				case "search_resource_endpoint":
					endpointUrl += "search/resource";
					break;
				case "search_action_endpoint":
					endpointUrl += "search/action";
					break;
				default:
					throw error ("Unknown API endpoint", args("Endpoint name", endpointName, "path", endpointConfigPath));
			}
		}
		env.putString("authzen_api_endpoint", endpointUrl);
		logSuccess("Successfully set API endpoint", args(endpointName, endpointUrl));
		return env;
	}

	/**
	 * A JSON null is a value not entered, as for an empty string; any other non-string value is
	 * an error rather than an exception out of the environment.
	 */
	private String stringOrNull(Environment env, String path) {
		JsonElement value = env.getElementFromObject("pdp", path);
		if (value == null || value.isJsonNull()) {
			return null;
		}
		if (!OIDFJSON.isString(value)) {
			throw error("The PDP's " + path + " is not a string", args(path, value));
		}
		return OIDFJSON.getString(value);
	}

}
