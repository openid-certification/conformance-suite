package net.openid.conformance.condition.client;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;

public class AustraliaConnectIdEnsureMtlsAliasesContainsRequiredEndpoints extends AbstractCondition {

	private static final List<String> REQUIRED_ENDPOINTS = ImmutableList.of("token_endpoint", "pushed_authorization_request_endpoint", "userinfo_endpoint");

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		JsonObject server = env.getObject("server");

		JsonElement mtlsEndpointAliasesEl = env.getElementFromObject("server", "mtls_endpoint_aliases");
		JsonObject mtlsEndpointAliases = null;

		if (mtlsEndpointAliasesEl != null) {
			if (!mtlsEndpointAliasesEl.isJsonObject()) {
				throw error("mtls_endpoint_aliases in the server configuration is not a JSON object", args("server", server));
			}

			mtlsEndpointAliases = (JsonObject) mtlsEndpointAliasesEl;
		}
		else {
			throw error("the server configuration does not contain the mtls_endpoint_aliases parameter", args("server", server));
		}

		List<String> missingEndpoints = new ArrayList<>();

		for (String endpoint: REQUIRED_ENDPOINTS) {
			if (! mtlsEndpointAliases.has(endpoint)) {
				missingEndpoints.add(endpoint);
			}
		}

		if (missingEndpoints.isEmpty()) {
			logSuccess("mtls_endpoint_aliases contains all the required endpoints", args("required", REQUIRED_ENDPOINTS, "mtls_endpoint_aliases", mtlsEndpointAliases));
			return env;
		}

		throw error("mtls_endpoint_aliases does not contain all the required endpoints",
			args("required", REQUIRED_ENDPOINTS, "mtls_endpoint_aliases", mtlsEndpointAliases, "missing", missingEndpoints));
	}

}
