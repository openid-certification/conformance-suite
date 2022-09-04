package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilEncryptRequestObject extends AbstractJWEEncryptString
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
		String clientSecret = null; // client_secret is not allowed in FAPI
		JsonObject serverJwks = env.getObject("server_jwks");

		String encryptedRequestObject = encrypt("server", requestObject, clientSecret, serverJwks, alg, enc,
			"request_object_encryption_alg", "request_object_encryption_enc");

		log("Encrypted the request object", args("request_object", encryptedRequestObject,
			"alg", alg,
			"enc", enc));
		env.putString("request_object", encryptedRequestObject);
		return env;
	}

}
