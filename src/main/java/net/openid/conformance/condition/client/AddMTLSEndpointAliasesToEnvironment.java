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

		JsonElement mtlsEndpointAliasesEl = env.getElementFromObject("server", "mtls_endpoint_aliases");
		JsonObject mtlsEndpointAliases = null;

		Set<String> allKeys = new HashSet<>();
		allKeys.addAll(server.keySet());
		if (mtlsEndpointAliasesEl != null) {
			if (!mtlsEndpointAliasesEl.isJsonObject()) {
				throw error("mtls_endpoint_aliases in the server configuration is not a JSON object", args("server", server));
			}

			mtlsEndpointAliases = (JsonObject) mtlsEndpointAliasesEl;
			allKeys.addAll(mtlsEndpointAliases.keySet());
		}

		for (String k : allKeys) {

			if (k.endsWith("_endpoint")) {

				JsonElement jsonElement = null;

				if (mtlsEndpointAliases != null) {
					jsonElement = mtlsEndpointAliases.get(k);
				}

				if (jsonElement != null) {
					env.putString(k, OIDFJSON.getString(jsonElement));
				} else {
					env.putString(k, OIDFJSON.getString(server.get(k)));
				}

			}
		}

		logSuccess("Added mtls_endpoint_aliases to environment");

		if (mtlsEndpointAliases != null) {
			if (mtlsEndpointAliases.has("authorization_endpoint")) {
				throw error("authorization_endpoint is incorrectly listed in mtls_endpoint_aliases - as per RFC8705 section 5 only endpoints the client makes a direct call are listed here.", mtlsEndpointAliases);
			}

			for (String k : mtlsEndpointAliases.keySet()) {
				if (!k.endsWith("_endpoint")) {
					throw error("unexpected value '" + k + "' found in mtls_endpoint_aliases. Only endpoints the client makes a direct call are listed here", mtlsEndpointAliases);
				}

			}
			Set<String> keysOnlyInMtlsEndpointAliases = new HashSet<>();
			keysOnlyInMtlsEndpointAliases.addAll(mtlsEndpointAliases.keySet());
			keysOnlyInMtlsEndpointAliases.removeAll(server.keySet());
			if (!keysOnlyInMtlsEndpointAliases.isEmpty()) {
				throw error("Some endpoints are found only in mtls_endpoint_aliases. Endpoints should only be present in mtls_endpoint_aliases if an endpoint has versions that both do and do not require mtls.",
					args("mtls_endpoint_aliases", mtlsEndpointAliases, "only_in_mtls", keysOnlyInMtlsEndpointAliases));
			}
		}

		return env;
	}
}
