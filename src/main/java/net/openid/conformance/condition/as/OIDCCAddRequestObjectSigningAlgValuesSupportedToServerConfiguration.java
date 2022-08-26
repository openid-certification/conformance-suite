package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class OIDCCAddRequestObjectSigningAlgValuesSupportedToServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		JsonObject server = env.getObject("server");

		JsonArray signingAlgValuesSupported = new JsonArray();
		signingAlgValuesSupported.add("none");
		signingAlgValuesSupported.add("RS256");
		signingAlgValuesSupported.add("PS256");
		signingAlgValuesSupported.add("ES256");
		signingAlgValuesSupported.add("EdDSA");

		server.add("request_object_signing_alg_values_supported", signingAlgValuesSupported);

		log("Added request_object_signing_alg_values_supported to server configuration", args("server", server));

		return env;
	}

}
