package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilAddLoggedUserNameToConsentRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "consent_endpoint_request")
	@PostEnvironment(required = "consent_endpoint_request")
	public Environment evaluate(Environment env) {
		String name = env.getString("brazil_logged_user_name");
		if (name == null) {
			logSuccess("No logged-in user name configured; consent request unchanged");
			return env;
		}
		env.getObject("consent_endpoint_request").getAsJsonObject("data").getAsJsonObject("loggedUser")
			.addProperty("name", name);
		logSuccess("Added logged-in user name to consent request", args("name", name));
		return env;
	}
}
