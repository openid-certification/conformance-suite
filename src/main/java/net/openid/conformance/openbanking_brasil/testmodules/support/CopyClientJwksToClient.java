package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CopyClientJwksToClient extends AbstractCondition {
	@Override
	@PreEnvironment(required = "client_jwks" )
	@PostEnvironment(required = "consent_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject clientJwks = env.getObject("client_jwks");
		if(clientJwks == null){
			throw  error("No client_jwks field was found");
		}

		JsonObject client = env.getObject("client");
		if(client == null){
			throw  error("No client field was found");
		}

		client.add("org_jwks", clientJwks);

		logSuccess("JWKS copied successfully", client);
		env.putObject("client", client);
		return env;
	}
}
