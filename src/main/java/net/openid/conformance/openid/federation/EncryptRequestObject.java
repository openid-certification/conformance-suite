package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.AbstractJWEEncryptString;
import net.openid.conformance.testmodule.Environment;

public class EncryptRequestObject extends AbstractJWEEncryptString
{

	@Override
	@PreEnvironment(strings = "request_object", required = "client")
	@PostEnvironment(strings = "request_object")
	public Environment evaluate(Environment env) {

		String requestObject = env.getString("request_object");
		String alg = "RSA-OAEP";
		String enc = "A256GCM";
		String clientSecret = null;
		JsonObject serverJwks = env.getObject("server_jwks");

		String encryptedRequestObject = encrypt("server", requestObject, clientSecret, serverJwks, alg, enc,
			"request_object_encryption_alg", "request_object_encryption_enc");

		env.putString("request_object", encryptedRequestObject);

		logSuccess("Encrypted the request object", args("request_object", encryptedRequestObject, "alg", alg, "enc", enc));
		return env;
	}

}
