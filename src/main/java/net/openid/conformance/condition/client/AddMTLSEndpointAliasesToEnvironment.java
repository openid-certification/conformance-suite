package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AddMTLSEndpointAliasesToEnvironment extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonObject server = env.getObject("server");

		JsonElement mtlsEndpointAliases = env.getElementFromObject("server", "mtls_endpoint_aliases");

		if (mtlsEndpointAliases == null || !mtlsEndpointAliases.isJsonObject()) {
			throw error("The mtls_endpoint_aliases is not present in the server configuration", args("server", server));
		}

		JsonObject mtlsEndpointAliasesObj = (JsonObject) mtlsEndpointAliases;

		server.keySet().forEach(k -> {

			if (k.endsWith("_endpoint")) {

				JsonElement jsonElement = mtlsEndpointAliasesObj.get(k);

				if (jsonElement != null) {
					env.putString(k, OIDFJSON.getString(jsonElement));
				} else {
					env.putString(k, OIDFJSON.getString(server.get(k)));
				}

			}
		});

		logSuccess("Added mtls_endpoint_aliases to environment", args("mtls_endpoint_aliases", mtlsEndpointAliasesObj));

		return env;
	}
}
