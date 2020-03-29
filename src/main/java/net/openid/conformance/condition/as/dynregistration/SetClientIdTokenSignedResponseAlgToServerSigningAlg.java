package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetClientIdTokenSignedResponseAlgToServerSigningAlg extends AbstractCondition
{

	@Override
	@PreEnvironment(required = { "client"}, strings = "signing_algorithm")
	public Environment evaluate(Environment env) {
		JsonObject client = env.getObject("client");
		String signingAlg = env.getString("signing_algorithm");
		client.addProperty("id_token_signed_response_alg", signingAlg);
		env.putObject("client", client);
		log("Set id_token_signed_response_alg for the registered client",
			args("id_token_signed_response_alg", signingAlg));
		return env;
	}
}
