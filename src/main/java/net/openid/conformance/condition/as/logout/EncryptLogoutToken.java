package net.openid.conformance.condition.as.logout;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.AbstractJWEEncryptString;
import net.openid.conformance.testmodule.Environment;

public class EncryptLogoutToken extends AbstractJWEEncryptString {

	@Override
	@PreEnvironment(strings = "logout_token", required = "client")
	@PostEnvironment(strings = "logout_token")
	public Environment evaluate(Environment env) {

		String token = env.getString("logout_token");
		String alg = env.getString("client", "id_token_encrypted_response_alg");
		String enc = env.getString("client", "id_token_encrypted_response_enc");
		String clientSecret = env.getString("client", "client_secret");
		//client jwks may be null
		JsonElement clientJwksElement = env.getElementFromObject("client", "jwks");
		JsonObject clientJwks = null;
		if(clientJwksElement!=null) {
			clientJwks = clientJwksElement.getAsJsonObject();
		}

		String encryptedToken = encrypt("client", token, clientSecret, clientJwks, alg, enc,
			"id_token_encrypted_response_alg", "id_token_encrypted_response_enc");

		logSuccess("Encrypted the logout token", args("logout_token", encryptedToken,
			"id_token_encrypted_response_alg", alg,
			"id_token_encrypted_response_enc", enc));
		env.putString("logout_token", encryptedToken);
		return env;
	}

}
