package net.openid.conformance.condition.as.par;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AddPushedAuthorizationRequestEndpointToServerConfig extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonObject server = env.getObject("server");

		String tokenEndpoint = OIDFJSON.getString(server.get("token_endpoint"));
		String parEndpoint = tokenEndpoint.replaceFirst("(token)$", "par");
		if(server.has("mtls_endpoint_aliases")) {
			JsonObject mtlsAliases = server.get("mtls_endpoint_aliases").getAsJsonObject();
			String mtlsTokenEndpoint = OIDFJSON.getString(mtlsAliases.get("token_endpoint"));
			String mtlsParEndpoint = mtlsTokenEndpoint.replaceFirst("(token)$", "par");
			mtlsAliases.addProperty("pushed_authorization_request_endpoint", mtlsParEndpoint);
		}
		server.addProperty("pushed_authorization_request_endpoint", parEndpoint);

		log("Added pushed_authorization_request_endpoint to server configuration", args("endpoint", parEndpoint));

		return env;
	}
}
