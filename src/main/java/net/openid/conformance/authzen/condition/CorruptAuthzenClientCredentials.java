package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

/**
 * Overwrites the client_secret and api_key entries in the test environment with a
 * deliberately invalid value, so that the auth-attaching condition still runs but
 * the PDP receives bogus credentials. Used by 401 negative tests where the PDP
 * is expected to reject the request.
 */
public class CorruptAuthzenClientCredentials extends AbstractCondition {

	public static final String INVALID_CREDENTIAL_VALUE = "invalid-bogus-credential";

	@Override
	public Environment evaluate(Environment env) {
		JsonObject client = env.getObject("client");
		if (client != null && client.has("client_secret")) {
			client.addProperty("client_secret", INVALID_CREDENTIAL_VALUE);
			env.putObject("client", client);
		}
		JsonObject pdp = env.getObject("pdp");
		if (pdp != null && pdp.has("api_key")) {
			pdp.addProperty("api_key", INVALID_CREDENTIAL_VALUE);
			env.putObject("pdp", pdp);
		}
		logSuccess("Replaced PDP authentication credentials with an invalid value");
		return env;
	}
}
