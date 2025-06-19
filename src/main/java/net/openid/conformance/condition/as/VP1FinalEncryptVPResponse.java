package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class VP1FinalEncryptVPResponse extends AbstractJWEEncryptString
{

	@Override
	@PreEnvironment(required = CreateAuthorizationEndpointResponseParams.ENV_KEY)
	@PostEnvironment(required = "direct_post_request_form_parameters")
	public Environment evaluate(Environment env) {

		final JsonElement jwksEl;
		try {
			jwksEl = env.getElementFromObject("authorization_request_object", "claims.client_metadata.jwks");
		} catch (Exception e) {
			throw error("Couldn't read client_metadata.jwks from authorization request", e, args("authorization_request", env.getObject("authorization_request_object")));
		}
		if (jwksEl == null) {
			throw error("An encrypted response was requested but client_metadata.jwks is not present in the received request.");
		}
		if (!jwksEl.isJsonObject()) {
			throw error("client_metadata.jwks must be a JSON object", args("client_jwks", jwksEl));
		}

		JsonObject clientJwks = jwksEl.getAsJsonObject();
		// just use the alg from the first key for now
		JsonElement algEl;
		try {
			algEl = clientJwks.get("keys").getAsJsonArray().get(0).getAsJsonObject().get("alg");
		} catch (Exception e) {
			throw error("Couldn't read alg from first key in client_metadata.jwks from authorization request", e, args("authorization_request", env.getObject("authorization_request_object")));
		}
		if (algEl == null) {
			throw error("Key in client_metadata in request does not contain alg field", args("client_jwks", clientJwks));
		}
		String alg = OIDFJSON.getString(algEl);

		// and just use the first enc - if there's not one default to A128GCM as per OID4VP spec
		JsonElement encValuesSupported = env.getElementFromObject("authorization_request_object", "claims.client_metadata.encrypted_response_enc_values_supported");
		String enc;
		if (encValuesSupported != null) {
			enc = OIDFJSON.getString(encValuesSupported.getAsJsonArray().get(0));
		} else {
			enc = "A128GCM";
			log("encrypted_response_enc_values_supported is not present in client_metadata in the authorization request parameters - defaulting to " + enc + " as per OID4VP spec");
		}

		String response = env.getObject(CreateAuthorizationEndpointResponseParams.ENV_KEY).toString();

		// FIXME - OID4VP doesn't require these values to be used for apu/apv, so we should probably use different values
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
