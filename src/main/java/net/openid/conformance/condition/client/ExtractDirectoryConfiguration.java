package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractDirectoryConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = { "directory_client", "directory_config" })
	public Environment evaluate(Environment env) {
		String discoveryUrl = env.getString("config", "directory.discoveryUrl");
		if (Strings.isNullOrEmpty(discoveryUrl)) {
			throw error("directory.discoveryUrl missing from test configuration");
		}

		String clientId = env.getString("config", "directory.client_id");
		if (Strings.isNullOrEmpty(clientId)) {
			throw error("directory.client_id missing from test configuration");
		}

		JsonObject server = new JsonObject();
		server.addProperty("discoveryUrl", discoveryUrl);
		JsonObject directoryConfig = new JsonObject();
		directoryConfig.add("server", server);
		env.putObject("directory_config", directoryConfig);

		JsonObject directoryClient = new JsonObject();
		directoryClient.addProperty("client_id", clientId);
		env.putObject("directory_client", directoryClient);

		logSuccess("Extracted directory configuration parameters",
			args("directory_config", directoryConfig,
				"directory_client", directoryClient));

		return env;
	}

}
