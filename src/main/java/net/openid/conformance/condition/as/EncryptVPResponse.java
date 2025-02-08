package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.util.Base64URL;
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
		JsonObject clientJwks = jwksEl.getAsJsonObject();

		String response = env.getObject(CreateAuthorizationEndpointResponseParams.ENV_KEY).toString();

		// As per ISO 18013-7 B.4.3.3.2 Authorization Response encryption
		String apu = env.getString("mdoc_generated_nonce");
		String apv = env.getString("nonce");
		Base64URL apub64 = apu != null ? Base64URL.encode(apu) : null;
		Base64URL apvb64 = apv != null ? Base64URL.encode(apv) : null;

		String encryptedResponse = encrypt("client", response, null, clientJwks, alg, enc,
			"authorization_encrypted_response_alg", "authorization_encrypted_response_enc",
			"json", apub64, apvb64);

		log("Encrypted the response", args("response", encryptedResponse,
			"authorization_encrypted_response_alg", alg,
			"authorization_encrypted_response_enc", enc,
			"apu", apu,
			"apu_b64", apub64,
			"apv", apv,
			"apv_b64", apvb64));

		JsonObject formParams = new JsonObject();
		formParams.addProperty("response", encryptedResponse);
		env.putObject("direct_post_request_form_parameters", formParams);

		return env;
	}

}
