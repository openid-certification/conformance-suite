package net.openid.conformance.authzen.condition;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractSetAuthzenApiEndpoint extends AbstractCondition {

	public Environment setAuthzenApiEndpoint(Environment env, String endpointName, String endpointConfigPath) {
		String endpointUrl = env.getString("pdp", endpointConfigPath);
		if(Strings.isNullOrEmpty(endpointUrl)) { // use default if not set
			endpointUrl = env.getString("pdp", "policy_decision_point");
			if(!endpointUrl.endsWith("/")) {
				endpointUrl += "/";
			}
			endpointUrl += "access/v1/";

			switch (endpointConfigPath) {
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

}
