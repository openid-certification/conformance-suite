package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureClientHasJwksOrJwksUri extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "client"})
	public Environment evaluate(Environment env) {

		JsonObject client = env.getObject("client");

		if (!client.has("jwks") && !client.has("jwks_uri")) {

			throw error("Client must have either jwks or jwks_uri set. This is typically required " +
				"when client authentication type is private_key_jwt " +
				" or self_signed_tls_client_auth, " +
				"or when an asymmetric algorithm is used for request_object_signing_alg, " +
				"id_token_encrypted_response_alg or userinfo_encrypted_response_alg." , args("client", client));

		}
		logSuccess("Client has jwks or jwks_uri", args("client", client));
		return env;
	}

}
