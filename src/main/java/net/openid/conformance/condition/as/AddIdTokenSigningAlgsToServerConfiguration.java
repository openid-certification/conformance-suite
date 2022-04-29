package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddIdTokenSigningAlgsToServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server", strings = "signing_algorithm")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		String alg = env.getString("signing_algorithm");

		JsonArray data = new JsonArray();
		data.add(alg);

		JsonObject server = env.getObject("server");
		server.add("id_token_signing_alg_values_supported", data);

		logSuccess("Added 'id_token_signing_alg_values_supported' to server metadata as 'alg' from server jwks", args("value", data));

		return env;
	}
}
