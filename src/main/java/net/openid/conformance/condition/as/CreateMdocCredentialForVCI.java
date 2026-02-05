package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DiagnosticOption;
import org.multipaz.testapp.VciMdocUtils;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

/**
 * Creates a mdoc credential (IssuerSigned structure) for VCI issuance.
 * This is used when the conformance suite acts as a credential issuer
 * and needs to create a mdoc credential in response to a credential request.
 */
public class CreateMdocCredentialForVCI extends AbstractCondition {

	@Override
	@PostEnvironment(required = "credential_issuance")
	public Environment evaluate(Environment env) {

		// Get all device public keys from the proof
		// Per VCI spec F.1 and F.3, we should issue a credential for each key
		List<String> publicJwkJsonList = resolveJwks(env);

		String docType = env.getString("credential_configuration", "doctype");
		if (docType == null || docType.isBlank()) {
			throw error("doctype is missing for credential configuration", args("credential_configuration", env.getObject("credential_configuration")));
		}

		// Optionally get custom issuer signing key from configuration
		JsonElement credentialSigningJwkEl = env.getElementFromObject("config", "credential.signing_jwk");
		String issuerSigningJwk = credentialSigningJwkEl != null ? credentialSigningJwkEl.toString() : null;

		JsonArray credentials = new JsonArray();
		for (String publicJwkJson : publicJwkJsonList) {
			// Create the mdoc credential for this key
			String mdocB64url = VciMdocUtils.createMdocCredential(publicJwkJson, docType, issuerSigningJwk);

			JsonObject credentialObj = new JsonObject();
			credentialObj.addProperty("credential", mdocB64url);
			credentials.add(credentialObj);

			// Log the created credential with CBOR diagnostics
			byte[] mdocBytes = Base64.getUrlDecoder().decode(mdocB64url);
			String diagnostics = Cbor.INSTANCE.toDiagnostics(mdocBytes,
				Set.of(DiagnosticOption.PRETTY_PRINT, DiagnosticOption.EMBEDDED_CBOR));

			log("Created mdoc credential (IssuerSigned) for VCI",
				args("mdoc_b64url", mdocB64url,
					"doctype", docType,
					"cbor_diagnostic", diagnostics));
		}

		JsonObject credentialIssuance = new JsonObject();
		credentialIssuance.add("credentials", credentials);
		env.putObject("credential_issuance", credentialIssuance);

		log("Created " + credentials.size() + " mdoc credential(s) for VCI",
			args("credentials", credentials, "credential_count", credentials.size()));

		return env;
	}

	/**
	 * Resolves all device public keys from the proof.
	 * Supports both 'jwt' and 'attestation' proof types.
	 * Per VCI spec F.1 and F.3, the issuer SHOULD issue a Credential for each
	 * cryptographic public key specified in the attested_keys claim or for each
	 * key in the jwt proofs array.
	 *
	 * @return List of JWK JSON strings (may contain a single null if no cryptographic binding is required)
	 */
	protected List<String> resolveJwks(Environment env) {
		// Check if the credential configuration requires cryptographic binding
		JsonObject credentialConfiguration = env.getObject("credential_configuration");
		if (credentialConfiguration != null && !credentialConfiguration.has("cryptographic_binding_methods_supported")) {
			// No cryptographic binding required, no device key needed
			log("Credential configuration does not require cryptographic binding, skipping device key binding");
			List<String> result = new ArrayList<>();
			result.add(null);
			return result;
		}

		String proofType = env.getString("proof_type");

		List<String> publicJWKs = new ArrayList<>();
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
					publicJWKs.add(publicJWK.toString());
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
				publicJWKs.add(publicJWK.toString());
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
				publicJWKs.add(key.toString());
			}
			log("Found " + publicJWKs.size() + " JWK(s) in attested_keys",
				args("attested_keys", jwksKeys, "key_count", publicJWKs.size()));
		} else {
			throw error("Cannot determine JWK from unsupported proof type: " + proofType,
				args("proof_type", proofType));
		}

		return publicJWKs;
	}
}
