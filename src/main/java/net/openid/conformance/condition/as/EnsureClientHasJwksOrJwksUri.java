package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureClientHasJwksOrJwksUri extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "client"})
	public Environment evaluate(Environment env) {

		JsonObject client = env.getObject("client");

		if (!client.has("jwks") && !client.has("jwks_uri")){

			throw error("Client must have either jwks or jwks_uri set. This is typically required " +
				"when client auth is private_key_jwt " +
				//" or client auth is self_signed_tls_client_auth " +
				"or request_object_signing_alg is set to PS*, RS*, ES*, EdDSA" +
				"or id_token_encrypted_response_alg, userinfo_encrypted_response_alg " +
				//TODO include the following line?
				", introspection_encrypted_response_alg, or authorization_encrypted_response_alg " +
				" match RSA*, ECDH*.", args("client", client));

		}
		logSuccess("Client has jwks or jwks_uri", args("client", client));
		return env;
	}

}
