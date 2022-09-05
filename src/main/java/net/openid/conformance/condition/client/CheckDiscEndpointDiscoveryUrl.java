package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.net.MalformedURLException;
import java.net.URL;

public class CheckDiscEndpointDiscoveryUrl extends AbstractCondition {

	private final String requiredProtocol = "https";

	private final String environmentBaseObject = "config";
	private final String environmentVariable = "server.discoveryUrl";

	private final String errorMessageNotJsonPrimitive = "Specified value is not a Json primitive";
	private final String errorMessageInvalidURL = "Invalid URL. Unable to parse.";
	private final String errorMessageNotRequiredProtocol = "Expected " + requiredProtocol + " protocol for " + environmentVariable;

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {

		JsonElement configUrl = env.getElementFromObject(environmentBaseObject, environmentVariable);
		if (configUrl == null) {
			throw error("Unable to find Discovery URL", args("No discoveryUrl", env.getObject("config")));
		}

		if (!configUrl.isJsonPrimitive()) {
			throw error(errorMessageNotJsonPrimitive, args("Failure", configUrl));
		} else {
			try {
				String discoveryUrl = OIDFJSON.getString(configUrl);

				if (!discoveryUrl.endsWith("/.well-known/openid-configuration")) {
					throw error("discoveryUrl is missing '/.well-known/openid-configuration'", args("actual", discoveryUrl));
				}

				URL extractedUrl = new URL(discoveryUrl);
				if (!extractedUrl.getProtocol().equals(requiredProtocol)) {
					throw error(errorMessageNotRequiredProtocol, args("actual", extractedUrl.getProtocol(), "expected", requiredProtocol));
				}

				logSuccess("discoveryUrl", args("actual", configUrl));

			} catch (MalformedURLException invalidURL) {
				throw error(errorMessageInvalidURL, args("Failure", configUrl));
			}
		}
		return env;
	}
}
