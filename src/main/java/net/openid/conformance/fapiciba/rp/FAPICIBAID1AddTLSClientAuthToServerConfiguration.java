package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPICIBAID1AddTLSClientAuthToServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonArray data = new JsonArray();
		data.add("tls_client_auth");

		JsonObject server = env.getObject("server");
		server.add("backchannel_endpoint_auth_methods_supported", data);

		logSuccess("Added tls_client_auth for backchannel_endpoint_auth_methods_supported");

		return env;
	}
}
