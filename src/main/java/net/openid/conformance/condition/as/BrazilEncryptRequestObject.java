package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class BrazilEncryptRequestObject extends AbstractJWEEncryptString
{

	@Override
	@PreEnvironment(strings = "request_object", required = "client")
	@PostEnvironment(strings = "request_object")
	public Environment evaluate(Environment env) {

		String requestObject = env.getString("request_object");
		// these are the only available options:
		// https://github.com/OpenBanking-Brasil/specs-seguranca/blob/main/open-banking-brasil-financial-api-1_ID1.md#encryption-algorithm-considerations
		String alg = "RSA-OAEP";
		String enc = "A256GCM";
		String clientSecret = null;
		//client jwks may be null
		JsonElement clientJwksElement = env.getObject("server_jwks");
		JsonObject clientJwks = null;
		if(clientJwksElement!=null) {
			clientJwks = clientJwksElement.getAsJsonObject();
		}

		String encryptedRequestObject = encrypt("server", requestObject, clientSecret, clientJwks, alg, enc,
			"id_token_encrypted_response_alg", "id_token_encrypted_response_enc");

		log("Encrypted the request object", args("request_object", encryptedRequestObject,
			"alg", alg,
			"enc", enc));
		env.putString("request_object", encryptedRequestObject);
		return env;
	}

}
