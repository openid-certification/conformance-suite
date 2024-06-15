package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class GenerateServerConfigurationMTLS extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server", strings = "base_url")
	public Environment evaluate(Environment env) {

		String baseMtlsUrl = env.getString("base_mtls_url");

		if (baseMtlsUrl.isEmpty()) {
			throw error("Base MTLS URL is empty");
		}

		// set off the URLs below with a slash, if needed
		if (!baseMtlsUrl.endsWith("/")) {
			baseMtlsUrl = baseMtlsUrl + "/";
		}

		JsonObject mtlsAliases = new JsonObject();
		mtlsAliases.addProperty("token_endpoint", baseMtlsUrl + "token");
		mtlsAliases.addProperty("backchannel_authentication_endpoint", baseMtlsUrl + "backchannel");


		JsonObject server = env.getObject("server");
		server.add("mtls_endpoint_aliases", mtlsAliases);

		logSuccess("Added mtls_endpoint_aliases", args("mtls_endpoint_aliases", mtlsAliases));

		return env;

	}

}
