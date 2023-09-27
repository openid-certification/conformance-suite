package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ChangeTokenEndpointInServerConfigurationToMtls extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server", strings={"base_url", "base_mtls_url"})
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		JsonObject server = env.getObject("server");
		String baseUrl = env.getString("base_url");
		String baseMtlsUrl = env.getString("base_mtls_url");

		String tokenEndpoint = OIDFJSON.getString(server.get("token_endpoint"));
		if(tokenEndpoint.startsWith(baseUrl)){
			//grab the path from base url part and prefix with mtls path
			tokenEndpoint = baseMtlsUrl + tokenEndpoint.substring(baseUrl.length());
		}
		server.addProperty("token_endpoint", tokenEndpoint);
		log("Replaced token_endpoint with the MTLS one");
		return env;
	}
}
