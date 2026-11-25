package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Adds the directory client secret from the test configuration to {@code directory_client}, for a
 * directory that authenticates the suite with a client secret.
 */
public class ExtractDirectoryClientSecret extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "config", "directory_client" })
	@PostEnvironment(required = "directory_client")
	public Environment evaluate(Environment env) {
		String clientSecret = env.getString("config", "directory.client_secret");
		if (Strings.isNullOrEmpty(clientSecret)) {
			throw error("'Directory client secret' field is missing from the 'Directory' section in the test configuration");
		}

		JsonObject directoryClient = env.getObject("directory_client");
		directoryClient.addProperty("client_secret", clientSecret);

		logSuccess("Added the directory client secret from the test configuration to the directory client",
			args("client_id", env.getString("directory_client", "client_id")));

		return env;
	}

}
