package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateInitiateLoginUriInRegistrationResponse extends AbstractCondition {

	public static final String INITIATE_LOGIN_URI = "initiate_login_uri";

	@Override
	@PreEnvironment(required = { "client" }, strings = INITIATE_LOGIN_URI)
	public Environment evaluate(Environment env) {

		String returnedUri = env.getString("client", INITIATE_LOGIN_URI);
		String initiateLoginUri = env.getString(INITIATE_LOGIN_URI);

		if (returnedUri == null) {
			throw error(INITIATE_LOGIN_URI + " missing from client registration response.");
		}

		if (!returnedUri.equals(initiateLoginUri)) {
			throw error(INITIATE_LOGIN_URI + " in client registration response does not match the value in the request.",
				args("requested", initiateLoginUri,
					"actual", returnedUri));
		}

		logSuccess(INITIATE_LOGIN_URI + " in registration response is correct.",
			args("actual", returnedUri));

		return env;

	}

}
