package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWEUtil;
import net.openid.conformance.util.JWKUtil;
import net.openid.conformance.testmodule.OIDFJSON;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.ArrayList;
import java.util.List;

public class VP1FinalEncryptVPResponse extends AbstractJWEEncryptString
{

	@Override
	@PreEnvironment(required = { CreateAuthorizationEndpointResponseParams.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.ENV_KEY })
	@PostEnvironment(required = "direct_post_request_form_parameters")
	public Environment evaluate(Environment env) {

		// read from the effective request parameters, which are populated for all request methods
		// (with url_query there is no request object, so authorization_request_object is absent)
		final JsonElement jwksEl;
		try {
			jwksEl = env.getElementFromObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "client_metadata.jwks");
		} catch (Exception e) {
			throw error("Couldn't read client_metadata.jwks from authorization request", e, args("authorization_request", env.getObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY)));
		}
		if (jwksEl == null) {
			throw error("An encrypted response was requested but client_metadata.jwks is not present in the received request.");
		}
		if (!jwksEl.isJsonObject()) {
			throw error("client_metadata.jwks must be a JSON object", args("client_jwks", jwksEl));
		}

		JsonObject clientJwks = jwksEl.getAsJsonObject();
		// use the alg from the first key a wallet could actually use - the set may deliberately
		// lead with unusable keys (the ignores-unusable-encryption-key test); this matches the
		// key selection in CreateVP1FinalVerifierIsoMdocRedirectSessionTranscriptEncrypted
		List<JWKUtil.SkippedJwk> skippedKeys = new ArrayList<>();
		JWK encKey = JWEUtil.selectFirstUsableEncKey(clientJwks, skippedKeys);
		for (JWKUtil.SkippedJwk skipped : skippedKeys) {
			log("Ignoring an unusable key in client_metadata.jwks, as a wallet would",
				args("key", skipped.keyJson(), "reason", skipped.reason()));
		}
		if (encKey == null) {
			throw error("No usable encryption key was found in client_metadata.jwks from the authorization request", args("client_jwks", clientJwks));
		}
		String alg = resolveResponseAlg(env, encKey, clientJwks);

		// and just use the first enc - if there's not one default to A128GCM as per OID4VP spec
		JsonElement encValuesSupported = env.getElementFromObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "client_metadata.encrypted_response_enc_values_supported");
		String enc;
		if (encValuesSupported != null) {
			enc = OIDFJSON.getString(encValuesSupported.getAsJsonArray().get(0));
		} else {
			enc = "A128GCM";
			log("encrypted_response_enc_values_supported is not present in client_metadata in the authorization request parameters - defaulting to " + enc + " as per OID4VP spec");
		}

		String response = env.getObject(CreateAuthorizationEndpointResponseParams.ENV_KEY).toString();

		// Neither OID4VP nor HAIP require any particular values for apu/apv, nor even require them to be present, so we should probably leave them out in some requests
		String apu = RandomStringUtils.secure().nextAlphanumeric(16); // there may not be a reason to restrict this to alphanumerics
		String apv = RandomStringUtils.secure().nextAlphanumeric(16);
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

	/**
	 * The JWE alg to encrypt the response with: the alg of the encryption key the verifier
	 * published, or, when the key does not carry one, the draft-era
	 * authorization_encrypted_response_alg client_metadata value if the verifier sent that
	 * instead. The missing alg is itself reported as a failure by
	 * VP1FinalValidateClientMetadataJwksForEncryptedResponse, so falling back here only lets the
	 * rest of the flow be exercised; it does not excuse the verifier.
	 */
	private String resolveResponseAlg(Environment env, JWK encKey, JsonObject clientJwks) {
		if (encKey.getAlgorithm() != null) {
			return encKey.getAlgorithm().getName();
		}
		JsonElement fallbackAlgEl = env.getElementFromObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "client_metadata.authorization_encrypted_response_alg");
		if (fallbackAlgEl == null) {
			throw error("Key in client_metadata in request does not contain alg field", args("client_jwks", clientJwks));
		}
		String alg = OIDFJSON.getString(fallbackAlgEl);
		// encrypt() reselects the key from the jwks by the key type the alg implies, so an alg for
		// a different key type would encrypt to a key other than the one selected here - whose
		// thumbprint the mdoc session transcript already commits to.
		KeyType algKeyType = JWEUtil.keyTypeForEncryptionAlg(JWEAlgorithm.parse(alg));
		if (algKeyType == null) {
			throw error("The encryption key in client_metadata.jwks has no alg value, and the client_metadata authorization_encrypted_response_alg value is not an RSA or ECDH-ES algorithm, so it cannot be used in its place",
				args("alg", alg, "client_jwks", clientJwks));
		}
		if (!encKey.getKeyType().equals(algKeyType)) {
			throw error("The encryption key in client_metadata.jwks has no alg value, and the client_metadata authorization_encrypted_response_alg value is for a different type of key than that key, so it cannot be used in its place",
				args("alg", alg, "alg_key_type", algKeyType.getValue(), "selected_key_type", encKey.getKeyType().getValue(), "client_jwks", clientJwks));
		}
		log("The encryption key in client_metadata.jwks does not contain an alg value (reported as a failure by a previous condition) - continuing using the value of the client_metadata authorization_encrypted_response_alg parameter, which is not defined in OID4VP 1.0 Final",
			args("alg", alg, "client_jwks", clientJwks));
		return alg;
	}

}
