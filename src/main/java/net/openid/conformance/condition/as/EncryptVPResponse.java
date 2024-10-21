package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EncryptVPResponse extends AbstractJWEEncryptString
{

	@Override
	@PreEnvironment(required = CreateAuthorizationEndpointResponseParams.ENV_KEY)
	@PostEnvironment(required = "direct_post_request_form_parameters")
	public Environment evaluate(Environment env) {

		final JsonElement jwksEl = env.getElementFromObject("authorization_request_object", "claims.client_metadata.jwks");
		if (jwksEl == null) {
			throw error("An encrypted response was requested by client_metadata.jwks is not present in the received request.");
		}

		String alg = env.getString("authorization_request_object", "claims.client_metadata.authorization_encrypted_response_alg");
		String enc = env.getString("authorization_request_object", "claims.client_metadata.authorization_encrypted_response_enc");
		String clientSecret = env.getString("client", "client_secret");
		JsonObject clientJwks = jwksEl.getAsJsonObject();

		String response = env.getObject(CreateAuthorizationEndpointResponseParams.ENV_KEY).toString();

		String encryptedResponse = encrypt("client", response, clientSecret, clientJwks, alg, enc,
			"authorization_encrypted_response_alg", "authorization_encrypted_response_enc");

		log("Encrypted the response", args("response", encryptedResponse,
			"authorization_encrypted_response_alg", alg,
			"authorization_encrypted_response_enc", enc));

		JsonObject formParams = new JsonObject();
		formParams.addProperty("response", encryptedResponse);
		env.putObject("direct_post_request_form_parameters", formParams);

		return env;
	}

}
