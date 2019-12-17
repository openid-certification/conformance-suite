package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractBearerAccessTokenFromParams extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	@PostEnvironment(strings = "incoming_access_token")
	public Environment evaluate(Environment env) {

		String incoming = env.getString("incoming_request", "body_form_params.access_token");

		if (!Strings.isNullOrEmpty(incoming)) {
			logSuccess("Found access token on incoming request", args("access_token", incoming));
			env.putString("incoming_access_token", incoming);
			return env;
		} else {
			throw error("Couldn't find access token in parameters");
		}

	}

}
