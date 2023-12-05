package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateIdTokenFromTokenResponseEncryption extends AbstractVerifyJweEncryption {

	@Override
	@PreEnvironment(required = { "token_endpoint_response", "client_jwks" })
	public Environment evaluate(Environment env) {

		JsonObject clientJwks = env.getObject("client_jwks");

		JsonElement tokenElement = env.getElementFromObject("token_endpoint_response", "id_token");
		if (tokenElement == null || !tokenElement.isJsonPrimitive()) {
			throw error("Couldn't find id_token in token_endpoint_response");
		}

		String idToken = OIDFJSON.getString(tokenElement);

		if (verifyJweEncryption(idToken, clientJwks, "id_token")) {
			logSuccess("The client has a valid asymmetric key to decrypt the id_token");
		}
		else {
			logSuccess("The id_token is not encrypted using an asymmetric encryption algorithm");
		}

		return env;
	}

}
