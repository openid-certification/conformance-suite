package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.BaseUrlUtil;
import net.openid.conformance.util.OAuthUriUtil;

public class AddDynamicRegistrationEndpointsToServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server", strings = { "base_url", "base_mtls_url" })
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		String baseUrl = OAuthUriUtil.stripTrailingSlash(BaseUrlUtil.resolveEffectiveBaseUrl(env));
		String baseMtlsUrl = OAuthUriUtil.stripTrailingSlash(env.getString("base_mtls_url"));
		if (baseUrl.isEmpty() || baseMtlsUrl.isEmpty()) {
			throw error("Base URL and mTLS base URL are required to advertise dynamic registration");
		}

		JsonObject server = env.getObject("server");
		JsonObject mtlsAliases = server.has("mtls_endpoint_aliases")
			? server.getAsJsonObject("mtls_endpoint_aliases")
			: new JsonObject();
		server.addProperty("registration_endpoint", baseUrl + "/register");
		mtlsAliases.addProperty("registration_endpoint", baseMtlsUrl + "/register");
		server.add("mtls_endpoint_aliases", mtlsAliases);

		logSuccess("Added dynamic registration endpoints to the CIBA server configuration",
			args("registration_endpoint", server.get("registration_endpoint"),
				"mtls_registration_endpoint", mtlsAliases.get("registration_endpoint")));
		return env;
	}
}
