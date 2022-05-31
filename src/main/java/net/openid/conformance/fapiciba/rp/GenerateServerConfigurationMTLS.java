package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.runner.TestDispatcher;
import net.openid.conformance.testmodule.Environment;

public class GenerateServerConfigurationMTLS extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server", strings = "base_url")
	public Environment evaluate(Environment env) {

		String baseUrl = env.getString("base_url");

		if (baseUrl.isEmpty()) {
			throw error("Base URL is empty");
		}

		// set off the URLs below with a slash, if needed
		if (!baseUrl.endsWith("/")) {
			baseUrl = baseUrl + "/";
		}

		String baseUrlMtls = baseUrl.replaceFirst(TestDispatcher.TEST_PATH, TestDispatcher.TEST_MTLS_PATH);
		JsonObject mtlsAliases = new JsonObject();
		mtlsAliases.addProperty("token_endpoint", baseUrlMtls + "token");
		mtlsAliases.addProperty("userinfo_endpoint", baseUrlMtls + "userinfo");
		mtlsAliases.addProperty("backchannel_authentication_endpoint", baseUrlMtls + "backchannel");

		JsonObject server = env.getObject("server");
		server.add("mtls_endpoint_aliases", mtlsAliases);

		logSuccess("Added mtls_endpoint_aliases", args("mtls_endpoint_aliases", mtlsAliases));

		return env;

	}

}
