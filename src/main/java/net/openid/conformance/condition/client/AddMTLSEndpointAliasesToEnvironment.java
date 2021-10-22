package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.HashSet;
import java.util.Set;

/**
 * This copies any server endpoints into the root of the environment, overriding it with any entry found in
 * mtls_endpoint_aliases.
 *
 * It is assumed that, when a test needs to use mtls, it will use the value in the root of the
 * environment.
 */
public class AddMTLSEndpointAliasesToEnvironment extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonObject server = env.getObject("server");

		JsonElement mtlsEndpointAliases = env.getElementFromObject("server", "mtls_endpoint_aliases");
		JsonObject mtlsEndpointAliasesObj = null;

		Set<String> keys = new HashSet<>();
		keys.addAll(server.keySet());
		if (mtlsEndpointAliases != null) {
			if (!mtlsEndpointAliases.isJsonObject()) {
				throw error("mtls_endpoint_aliases in the server configuration is not a JSON object", args("server", server));
			}

			mtlsEndpointAliasesObj = (JsonObject) mtlsEndpointAliases;
			keys.addAll(mtlsEndpointAliasesObj.keySet());
		}

		for (String k : keys) {

			if (k.endsWith("_endpoint")) {

				JsonElement jsonElement = null;

				if (mtlsEndpointAliasesObj != null) {
					jsonElement = mtlsEndpointAliasesObj.get(k);
				}

				if (jsonElement != null) {
					env.putString(k, OIDFJSON.getString(jsonElement));
				} else {
					env.putString(k, OIDFJSON.getString(server.get(k)));
				}

			}
		}

		logSuccess("Added mtls_endpoint_aliases to environment");

		return env;
	}
}
