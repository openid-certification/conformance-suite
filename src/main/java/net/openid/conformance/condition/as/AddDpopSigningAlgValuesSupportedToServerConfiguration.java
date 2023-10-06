package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddDpopSigningAlgValuesSupportedToServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonArray data = new JsonArray();
		data.add("PS256");
		data.add("ES256");
		data.add("EdDSA");

		JsonObject server = env.getObject("server");
		server.add("dpop_signing_alg_values_supported", data);

		logSuccess("Set dpop_signing_alg_values_supported", args("values", data));

		return env;
	}
}
