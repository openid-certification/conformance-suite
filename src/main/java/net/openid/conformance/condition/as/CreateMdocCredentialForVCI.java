package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DiagnosticOption;
import org.multipaz.testapp.VciMdocUtils;

import java.util.Base64;
import java.util.Set;

/**
 * Creates an mdoc credential (IssuerSigned structure) for VCI issuance.
 * This is used when the conformance suite acts as a credential issuer
 * and needs to create an mdoc credential in response to a credential request.
 */
public class CreateMdocCredentialForVCI extends AbstractCondition {

	private static final String DEFAULT_DOCTYPE = "eu.europa.ec.eudi.pid.1";

	@Override
	@PostEnvironment(strings = "credential")
	public Environment evaluate(Environment env) {

		// Get the device public key from the proof (same pattern as CreateSdJwtCredential)
		String publicJwkJson = resolveJwk(env);

		// Get doctype from credential configuration (set by VCIResolveRequestedCredentialConfigurationFromRequest), or use default
		String docType = env.getString("credential_configuration", "doctype");
		if (docType == null || docType.isBlank()) {
			docType = DEFAULT_DOCTYPE;
		}

		// Optionally get custom issuer signing key from configuration
		JsonElement credentialSigningJwkEl = env.getElementFromObject("config", "credential.signing_jwk");
		String issuerSigningJwk = credentialSigningJwkEl != null ? credentialSigningJwkEl.toString() : null;

		// Create the mdoc credential
		String mdocB64url = VciMdocUtils.createMdocCredential(publicJwkJson, docType, issuerSigningJwk);

		env.putString("credential", mdocB64url);

		// Log the created credential with CBOR diagnostics
		byte[] mdocBytes = Base64.getUrlDecoder().decode(mdocB64url);
		String diagnostics = Cbor.INSTANCE.toDiagnostics(mdocBytes,
			Set.of(DiagnosticOption.PRETTY_PRINT, DiagnosticOption.EMBEDDED_CBOR));

		log("Created mdoc credential (IssuerSigned) for VCI",
			args("mdoc_b64url", mdocB64url,
				"doctype", docType,
				"cbor_diagnostic", diagnostics));

		return env;
	}

	/**
	 * Resolves the device public key from the proof.
	 * Supports both 'jwt' and 'attestation' proof types.
	 * Returns null if the credential configuration doesn't require cryptographic binding.
	 */
	protected String resolveJwk(Environment env) {
		// Check if the credential configuration requires cryptographic binding
		JsonObject credentialConfiguration = env.getObject("credential_configuration");
		if (credentialConfiguration != null && !credentialConfiguration.has("cryptographic_binding_methods_supported")) {
			// No cryptographic binding required, no device key needed
			log("Credential configuration does not require cryptographic binding, skipping device key binding");
			return null;
		}

		String proofType = env.getString("proof_type");

		JsonElement publicJWK;
		if ("jwt".equals(proofType)) {
			publicJWK = env.getElementFromObject("proof_jwt", "header.jwk");
			if (publicJWK == null) {
				throw error("Couldn't find public JWK in proof_jwt header.jwk for proof type: " + proofType,
					args("proof_type", proofType));
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
			if (jwksKeys.size() > 1) {
				log("Found more than one JWK in attested keys, using the first key in the list",
					args("attested_keys", jwksKeys, "first_jwk", jwksKeys.get(0)));
			} else {
				log("Found one JWK in attested keys",
					args("attested_keys", jwksKeys, "first_jwk", jwksKeys.get(0)));
			}
			publicJWK = jwksKeys.get(0);
		} else {
			throw error("Cannot determine JWK from unsupported proof type: " + proofType,
				args("proof_type", proofType));
		}

		return publicJWK.toString();
	}
}
