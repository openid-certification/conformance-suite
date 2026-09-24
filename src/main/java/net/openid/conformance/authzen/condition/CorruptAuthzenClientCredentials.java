package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Overwrites the client_secret and api_key entries in the test environment with a
 * deliberately invalid value, so that the auth-attaching condition still runs but
 * the PDP receives bogus credentials. Used by 401 negative tests where the PDP
 * is expected to reject the request. Fails if neither credential is present, so a
 * 401 test cannot silently pass for the wrong reason.
 */
public class CorruptAuthzenClientCredentials extends AbstractCondition {

	public static final String INVALID_CREDENTIAL_VALUE = "invalid-bogus-credential";

	@Override
	@PreEnvironment(required = "client")
	@PostEnvironment(required = "client")
	public Environment evaluate(Environment env) {
		JsonObject client = env.getObject("client");
		boolean corruptedClientSecret = false;
		boolean corruptedApiKey = false;
		if (client.has("client_secret")) {
			client.addProperty("client_secret", INVALID_CREDENTIAL_VALUE);
			corruptedClientSecret = true;
		}
		if (client.has("api_key")) {
			client.addProperty("api_key", INVALID_CREDENTIAL_VALUE);
			corruptedApiKey = true;
		}
		if (!corruptedClientSecret && !corruptedApiKey) {
			throw error("No PDP authentication credentials available to corrupt — neither client.client_secret nor client.api_key is set; the 401 negative test cannot prove the PDP rejected bad credentials");
		}
		env.putObject("client", client);
		logSuccess("Replaced PDP authentication credentials with an invalid value",
			args("corrupted_client_secret", corruptedClientSecret, "corrupted_api_key", corruptedApiKey));
		return env;
	}
}
