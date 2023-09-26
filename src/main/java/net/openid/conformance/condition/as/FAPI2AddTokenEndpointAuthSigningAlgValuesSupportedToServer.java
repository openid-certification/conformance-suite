package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPI2AddTokenEndpointAuthSigningAlgValuesSupportedToServer extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonArray algs = new JsonArray();
		algs.add("PS256");
		algs.add("ES256");
		algs.add("EdDSA");

		JsonObject server = env.getObject("server");
		server.add("token_endpoint_auth_signing_alg_values_supported", algs);

		logSuccess("Set token_endpoint_auth_signing_alg_values_supported", args("values", algs));

		return env;
	}
}
