package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.net.MalformedURLException;
import java.net.URL;

public class FAPIBrazilCheckDirectoryDiscoveryUrl extends AbstractCondition {
	private final String BRAZIL_DIRECTORY_DISCOVERY_URL = "https://auth.sandbox.directory.openbankingbrasil.org.br/.well-known/openid-configuration";

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {

		String discoveryUrl = getStringFromEnvironment(env, "config", "directory.discoveryUrl");

		if (!discoveryUrl.equals(BRAZIL_DIRECTORY_DISCOVERY_URL)) {
			throw error("Testing for Brazil certification must be done using the Brazil directory. If you do not have access to the directory an example client is available in the conformance suite instructions.",
				args("directory_discovery", discoveryUrl,
					"expected", BRAZIL_DIRECTORY_DISCOVERY_URL));
		}

		logSuccess("Directory Discovery URL matches the Brazil directory.", args("actual", discoveryUrl));

		return env;

	}

}
