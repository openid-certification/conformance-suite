package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EncryptIdToken extends AbstractJWEEncryptString
{

	@Override
	@PreEnvironment(strings = "id_token", required = "client")
	@PostEnvironment(strings = "id_token")
	public Environment evaluate(Environment env) {

		String idToken = env.getString("id_token");
		String alg = env.getString("client", "id_token_encrypted_response_alg");
		String enc = env.getString("client", "id_token_encrypted_response_enc");
		String clientSecret = env.getString("client", "client_secret");
		//client jwks may be null
		JsonElement clientJwksElement = env.getElementFromObject("client", "jwks");
		JsonObject clientJwks = null;
		if(clientJwksElement!=null) {
			clientJwks = clientJwksElement.getAsJsonObject();
		}

		String encryptedIdToken = encrypt("client", idToken, clientSecret, clientJwks, alg, enc,
			"id_token_encrypted_response_alg", "id_token_encrypted_response_enc");

		log("Encrypted the id token", args("id_token", encryptedIdToken,
			"id_token_encrypted_response_alg", alg,
			"id_token_encrypted_response_enc", enc));
		env.putString("id_token", encryptedIdToken);
		return env;
	}

}
