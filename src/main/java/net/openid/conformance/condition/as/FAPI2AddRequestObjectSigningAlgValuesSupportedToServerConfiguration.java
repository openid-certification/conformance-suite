package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPI2AddRequestObjectSigningAlgValuesSupportedToServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server", strings = "signing_algorithm")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonArray algs = new JsonArray();
		algs.add("PS256");
		algs.add("ES256");
		algs.add("EdDSA");

		JsonObject server = env.getObject("server");
		server.add("request_object_signing_alg_values_supported", algs);

		logSuccess("Added 'request_object_signing_alg_values_supported' to server metadata", args("value", algs));

		return env;
	}
}
