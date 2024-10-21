package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EncryptVPResponse extends AbstractJWEEncryptString
{

	@Override
	@PreEnvironment(strings = "jarm_response", required = "client")
	@PostEnvironment(strings = "jarm_response")
	public Environment evaluate(Environment env) {

		// animo key FIXME hardcoded
//		String jwks = """
//			 {
//				"use": "enc",
//				"kty": "EC",
//				"crv": "P-256",
//				"x": "I4lTY1MXPc-HEA8UxDuwOzo94NExsImKuLxEuhkAx5s",
//				"y": "8WJEeMYcA9qpFO-oA72ys0KlA-v9w3-MdoSdGpTVF04",
//				"kid": "zDnaeSpgmT3YCEFvdmzXFEXiJaTcCAwoe97wrYjJpVHLNZs78"
//			  }
//			""";
		String response = env.getString("jarm_response");
		String alg = env.getString("client", "authorization_encrypted_response_alg");
		String enc = env.getString("client", "authorization_encrypted_response_enc");
		String clientSecret = env.getString("client", "client_secret");
		//client jwks may be null
		JsonElement clientJwksElement = env.getElementFromObject("client", "jwks");
		JsonObject clientJwks = null;
		if(clientJwksElement!=null) {
			clientJwks = clientJwksElement.getAsJsonObject();
		}

		String encryptedResponse = encrypt("client", response, clientSecret, clientJwks, alg, enc,
			"authorization_encrypted_response_alg", "authorization_encrypted_response_enc");

		log("Encrypted the JARM response", args("response", encryptedResponse,
			"authorization_encrypted_response_alg", alg,
			"authorization_encrypted_response_enc", enc));
		env.putString("jarm_response", encryptedResponse);
		return env;
	}

}
