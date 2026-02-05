package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CreateSdJwtCredential extends AbstractCreateSdJwtCredential {

	public CreateSdJwtCredential() {
		super();
	}

	public CreateSdJwtCredential(Map<String, Object> additionalClaims) {
		super(additionalClaims);
	}

	@Override
	@PostEnvironment(required = "credential_issuance")
	public Environment evaluate(Environment env) {

		List<Object> publicJWKs = resolveJwks(env);

		JsonArray credentials = new JsonArray();
		for (Object publicJWK : publicJWKs) {
			String sdJwt = createSdJwt(env, publicJWK, null);
			JsonObject credentialObj = new JsonObject();
			credentialObj.addProperty("credential", sdJwt);
			credentials.add(credentialObj);
		}

		JsonObject credentialIssuance = new JsonObject();
		credentialIssuance.add("credentials", credentials);
		env.putObject("credential_issuance", credentialIssuance);

		log("Created EU ARF 1.8 PID credential(s) in SD-JWT VC format",
			args("credentials", credentials, "credential_count", credentials.size()));

		return env;

	}

	/**
	 * Resolves all device public keys from the proof.
	 * Per VCI spec F.1 and F.3, the issuer SHOULD issue a Credential for each
	 * cryptographic public key specified in the attested_keys claim or for each
	 * key in the jwt proofs array.
	 *
	 * @return List of JWKs (may contain a single null if no cryptographic binding is required)
	 */
	protected List<Object> resolveJwks(Environment env) {

		// Check if the credential configuration requires cryptographic binding
		JsonObject credentialConfiguration = env.getObject("credential_configuration");
		if (credentialConfiguration != null && !credentialConfiguration.has("cryptographic_binding_methods_supported")) {
			// No cryptographic binding required, no cnf claim needed
			log("Credential configuration does not require cryptographic binding, skipping cnf claim");
			List<Object> result = new ArrayList<>();
			result.add(null);
			return result;
		}

		String proofType = env.getString("proof_type");

		List<Object> publicJWKs = new ArrayList<>();
		if ("jwt".equals(proofType)) {
			// Check if we have multiple proof JWTs (proof_jwts array)
			JsonObject proofJwtsWrapper = env.getObject("proof_jwts");
			if (proofJwtsWrapper != null && proofJwtsWrapper.has("items")) {
				JsonArray proofJwtsArray = proofJwtsWrapper.getAsJsonArray("items");
				for (JsonElement proofJwtEl : proofJwtsArray) {
					JsonObject proofJwt = proofJwtEl.getAsJsonObject();
					JsonElement publicJWK = proofJwt.has("header") ?
						proofJwt.getAsJsonObject("header").get("jwk") : null;
					if (publicJWK == null) {
						throw error("Couldn't find public JWK in proof_jwt header.jwk",
							args("proof_type", proofType, "proof_jwt", proofJwt));
					}
					publicJWKs.add(publicJWK);
				}
				log("Found " + publicJWKs.size() + " JWK(s) from jwt proofs",
					args("key_count", publicJWKs.size()));
			} else {
				// Fallback to single proof_jwt for backward compatibility
				JsonElement publicJWK = env.getElementFromObject("proof_jwt", "header.jwk");
				if (publicJWK == null) {
					throw error("Couldn't find public JWK in proof_jwt header.jwk for proof type: " + proofType,
						args("proof_type", proofType));
				}
				publicJWKs.add(publicJWK);
				log("Found JWK in jwt proof", args("jwk", publicJWK));
			}
		} else if ("attestation".equals(proofType)) {
			JsonElement proofAttestation = env.getElementFromObject("proof_attestation", "claims.attested_keys");
			if (proofAttestation == null || !proofAttestation.isJsonArray()) {
				throw error("Couldn't find attested_keys in proof_attestation claims for proof type: " + proofType,
					args("proof_type", proofType));
			}
			var jwksKeys = proofAttestation.getAsJsonArray();
			if (jwksKeys.isEmpty()) {
				throw error("attested_keys of Attestation must not be empty");
			}
			// Add all keys from attested_keys - per spec we should issue a credential for each
			for (JsonElement key : jwksKeys) {
				publicJWKs.add(key);
			}
			log("Found " + publicJWKs.size() + " JWK(s) in attested_keys",
				args("attested_keys", jwksKeys, "key_count", publicJWKs.size()));
		} else {
			throw error("Cannot determine JWK from unsupported proof type: " + proofType, args("proof_type", proofType));
		}

		return publicJWKs;
	}

}
