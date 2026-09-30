package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;

/**
 * Same as {@link AddVP1FinalIsoMdocClientMetadataToAuthorizationRequest} but lists every
 * algorithm the suite can verify, in both issuerauth_alg_values and deviceauth_alg_values.
 * This tests that wallets accept a verifier that states the algorithms it supports for both
 * structures (OID4VP 1.0 Final Appendix B.2.2).
 */
public class AddVP1FinalIsoMdocClientMetadataWithAllSupportedAlgsToAuthorizationRequest extends AddVP1FinalIsoMdocClientMetadataToAuthorizationRequest {

	/**
	 * The COSE identifiers the mdoc parsing resolves and verifies as the alg of an IssuerAuth or
	 * DeviceSignature header: ES256, ESP256, ES384, ES512 and EdDSA. The four algorithms other
	 * than ESP256 are the ones ISO/IEC 18013-5 9.1.2.4 allows, and cover every curve it pairs
	 * them with. The remaining fully-specified identifiers of RFC 9864 are left out because the
	 * multipaz version in use does not resolve them to the registered algorithms.
	 */
	public static final List<Integer> VERIFIABLE_SIGNATURE_ALGORITHMS = List.of(-7, -9, -35, -36, -8);

	/**
	 * Appendix B.2.2 Table 2: a DeviceMac using HMAC 256/256 with the MAC key established via
	 * ECDH over P-256. It is the only DeviceMac the suite can verify, and only when the response
	 * is encrypted to a P-256 key: see the note on the reader key in {@link ParseCredentialAsMdoc}.
	 */
	public static final int DEVICE_MAC_ECDH_P256 = -65537;

	@Override
	@PreEnvironment(required = { "authorization_endpoint_request", "client_public_jwks" }, strings = "response_mode")
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}

	@Override
	protected JsonObject createMsoMdocFormatParameters(Environment env) {
		JsonArray deviceAuthAlgValues = signatureAlgorithms();
		if (responseIsEncryptedToP256Key(env)) {
			deviceAuthAlgValues.add(DEVICE_MAC_ECDH_P256);
		}

		JsonObject parameters = new JsonObject();
		parameters.add("issuerauth_alg_values", signatureAlgorithms());
		parameters.add("deviceauth_alg_values", deviceAuthAlgValues);
		return parameters;
	}

	private static JsonArray signatureAlgorithms() {
		JsonArray values = new JsonArray();
		VERIFIABLE_SIGNATURE_ALGORITHMS.forEach(values::add);
		return values;
	}

	private static boolean responseIsEncryptedToP256Key(Environment env) {
		String responseMode = env.getString("response_mode");
		if (!responseMode.equals("direct_post.jwt") && !responseMode.equals("dc_api.jwt")) {
			return false;
		}
		// the key AddVP1FinalEncryptionParametersToClientMetadata offers for encrypting the response
		for (JsonElement jwkEl : env.getObject("client_public_jwks").getAsJsonArray("keys")) {
			JsonObject jwk = jwkEl.getAsJsonObject();
			if (jwk.has("use") && OIDFJSON.getString(jwk.get("use")).equals("enc")) {
				return jwk.has("crv") && OIDFJSON.getString(jwk.get("crv")).equals("P-256");
			}
		}
		return false;
	}
}
