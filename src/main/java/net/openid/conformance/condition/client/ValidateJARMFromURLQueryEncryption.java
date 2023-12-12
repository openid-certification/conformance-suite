package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateJARMFromURLQueryEncryption extends AbstractVerifyJweEncryption {

	@Override
	@PreEnvironment(required = { "callback_query_params", "client_jwks" })
	public Environment evaluate(Environment env) {

		JsonObject clientJwks = env.getObject("client_jwks");

		JsonElement responseElement = env.getElementFromObject("callback_query_params", "response");
		if (responseElement == null || !responseElement.isJsonPrimitive()) {
			throw error("Couldn't find response in callback_query_params");
		}

		String response = OIDFJSON.getString(responseElement);

		if (verifyJweEncryption(response, clientJwks, "response")) {
			logSuccess("The client has a valid asymmetric key to decrypt the respose");
		}
		else {
			logSuccess("The response is not encrypted using an asymmetric encryption algorithm");
		}

		return env;
	}

}
