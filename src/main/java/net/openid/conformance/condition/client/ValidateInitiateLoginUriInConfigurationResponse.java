package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateInitiateLoginUriInConfigurationResponse extends AbstractCondition {

	public static final String INITIATE_LOGIN_URI = "initiate_login_uri";

	@Override
	@PreEnvironment(required = "registration_client_endpoint_response", strings = INITIATE_LOGIN_URI)
	public Environment evaluate(Environment env) {

		String returnedUri = env.getString("registration_client_endpoint_response", "body_json."+INITIATE_LOGIN_URI);
		String initiateLoginUri = env.getString(INITIATE_LOGIN_URI);

		if (returnedUri == null) {
			throw error(INITIATE_LOGIN_URI + " missing from client configuration response.");
		}

		if (!returnedUri.equals(initiateLoginUri)) {
			throw error(INITIATE_LOGIN_URI + " in client configuration response does not match the value the client registered.",
				args("requested", initiateLoginUri,
					"actual", returnedUri));
		}

		logSuccess(INITIATE_LOGIN_URI + " in configuration response is correct.",
			args("actual", returnedUri));

		return env;

	}

}
