package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddOnlyRS256ToIdTokenSigningAlgValuesSupported extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonArray data = new JsonArray();
		data.add("RS256");

		JsonObject server = env.getObject("server");
		server.add("id_token_signing_alg_values_supported", data);

		logSuccess("Set id_token_signing_alg_values_supported to RS256 only");

		return env;
	}
}
