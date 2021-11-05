package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import org.springframework.beans.factory.annotation.Value;

public class SetDirectoryInfo extends AbstractCondition {

    private final String BRAZIL_DIRECTORY_DISCOVERY_URL = "https://auth.sandbox.directory.openbankingbrasil.org.br/.well-known/openid-configuration";
	private final String BRAZIL_DIRECTORY_API_BASE = "https://matls-api.sandbox.directory.openbankingbrasil.org.br/";

	@Override
	public Environment evaluate(Environment env) {

		var config = env.getObject("config");
		JsonObject directoryObj = new JsonObject();
		directoryObj.addProperty("discoveryUrl", BRAZIL_DIRECTORY_DISCOVERY_URL);
		directoryObj.addProperty("client_id", env.getString("config", "directory.client_id"));
		directoryObj.addProperty("apibase", BRAZIL_DIRECTORY_API_BASE);
		directoryObj.addProperty("keystore", "https://keystore.sandbox.directory.openbankingbrasil.org.br/");
		config.add("directory", directoryObj);

		//log("Env:\n" + env.toString());

		return env;
	}
}
