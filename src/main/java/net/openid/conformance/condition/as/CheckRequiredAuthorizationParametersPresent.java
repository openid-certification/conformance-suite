package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;

public class CheckRequiredAuthorizationParametersPresent extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_endpoint_request"})
	public Environment evaluate(Environment env) {


		List<String> responses = new ArrayList<>();
		responses.add(env.getString("authorization_endpoint_request", "params.response_type"));
		responses.add(env.getString("authorization_endpoint_request", "params.client_id"));
		responses.add(env.getString("authorization_endpoint_request", "params.redirect_uri"));
		responses.add(env.getString("authorization_endpoint_request", "params.scope"));

		for (String singleResponse : responses) {
			if (Strings.isNullOrEmpty(singleResponse)) {
				throw error("Required parameter value(s) not present in the authorization endpoint request", args("Missing parameter", singleResponse));
			}
		}

		logSuccess("Required parameter values are found outside of the post body", args("parameters", responses));

		return env;
	}
}
