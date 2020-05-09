package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetClientIdTokenSignedResponseAlgToHS256 extends AbstractCondition
{

	@Override
	@PreEnvironment(required = { "client"})
	public Environment evaluate(Environment env) {
		JsonObject client = env.getObject("client");
		client.addProperty("id_token_signed_response_alg", "HS256");
		env.putObject("client", client);
		log("Set id_token_signed_response_alg to HS256 for the registered client", args("client", client));
		return env;
	}
}
