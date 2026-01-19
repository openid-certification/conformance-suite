package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;

public class CreateSdJwtCredential extends AbstractCreateSdJwtCredential {

	public CreateSdJwtCredential() {
		super();
	}

	public CreateSdJwtCredential(Map<String, Object> additionalClaims) {
		super(additionalClaims);
	}

	@Override
	@PostEnvironment(strings = "credential")
	public Environment evaluate(Environment env) {


		Object publicJWK = resolveJwk(env);

		String sdJwt = createSdJwt(env, publicJWK, null);

		env.putString("credential", sdJwt);

		log("Created an EU ARF 1.8 PID in SD-JWT VC format", args("sdjwt", sdJwt));

		return env;

	}

	protected Object resolveJwk(Environment env) {

		// Check if the credential configuration requires cryptographic binding
		JsonObject credentialConfiguration = env.getObject("credential_configuration");
		if (credentialConfiguration != null && !credentialConfiguration.has("cryptographic_binding_methods_supported")) {
			// No cryptographic binding required, no cnf claim needed
			log("Credential configuration does not require cryptographic binding, skipping cnf claim");
			return null;
		}

		String proofType = env.getString("proof_type");

		Object publicJWK;
		if ("jwt".equals(proofType)) {
			publicJWK = env.getElementFromObject("proof_jwt", "header.jwk");
			if (publicJWK == null) {
				throw error("Couldn't find public JWK in proof_jwt header.jwk for proof type: " + proofType, args("proof_type", proofType));
			}
		} else if ("attestation".equals(proofType)) {
			JsonElement proofAttestation = env.getElementFromObject("proof_attestation", "claims.attested_keys");
			if (!proofAttestation.isJsonArray()) {
				throw error("Couldn't find attested_keys in proof_attestation claims for proof type: " + proofType, args("proof_type", proofType));
			}
			var jwksKeys = proofAttestation.getAsJsonArray();
			if (jwksKeys.isEmpty()) {
				throw error("attested_keys of Attestation must not be empty");
			}
			if (jwksKeys.size() > 1) {
				log("Found more than one JWKs in attested keys, using the first key in the list", args("attested_keys", jwksKeys, "first_jwk", jwksKeys.get(0)));
			} else {
				log("Found one JWK in attested keys", args("attested_keys", jwksKeys, "first_jwk", jwksKeys.get(0)));
			}
			publicJWK = jwksKeys.get(0);
		} else {
			throw error("Cannot determine JWK from unsupported proof type: " + proofType, args("proof_type", proofType));
		}

		return publicJWK;
	}

}
