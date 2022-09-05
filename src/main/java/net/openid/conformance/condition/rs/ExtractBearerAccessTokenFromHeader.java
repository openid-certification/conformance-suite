package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractBearerAccessTokenFromHeader extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	@PostEnvironment(strings = "incoming_access_token")
	public Environment evaluate(Environment env) {

		String auth = env.getString("incoming_request", "headers.authorization");

		if (!Strings.isNullOrEmpty(auth)) {
			if (auth.toLowerCase().startsWith("bearer") && auth.length() > "bearer ".length()) {
				String incoming = auth.substring("bearer ".length());
				if (!Strings.isNullOrEmpty(incoming)) {
					logSuccess("Found access token on incoming request", args("access_token", incoming));
					env.putString("incoming_access_token", incoming);
					return env;
				} else {
					throw error("Couldn't find access token in header");
				}
			} else {
				throw error("Couldn't find bearer token in authorization header");
			}
		} else {
			throw error("Couldn't find authorization header");
		}

	}

}
