package net.openid.conformance.condition.client;

import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.CborMap;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.DiagnosticOption;

import java.util.Base64;
import java.util.Set;

/**
 * Parses an mdoc credential received from a VCI credential issuance response.
 *
 * For VCI issuance, the credential is an IssuerSigned structure (not a DeviceResponse like in VP flow).
 * The IssuerSigned structure contains:
 * - nameSpaces: IssuerNamespaces containing the credential claims
 * - issuerAuth: COSE_Sign1 signature over the MSO (Mobile Security Object)
 *
 * This condition validates the CBOR structure and logs the contents.
 * Full cryptographic validation of the MSO signature would require additional conditions.
 */
public class ParseMdocCredentialFromVCIIssuance extends AbstractCondition {

	@Override
	@PreEnvironment(strings = { "credential" })
	@PostEnvironment(strings = { "mdoc_credential_cbor" })
	public Environment evaluate(Environment env) {
		String mdocBase64 = env.getString("credential");

		if (mdocBase64 == null || mdocBase64.isEmpty()) {
			throw error("credential string is null or empty");
		}

		// Check if this looks like a JWT (SD-JWT VC) instead of base64url-encoded CBOR
		if (mdocBase64.contains(".") && mdocBase64.split("\\.").length >= 2) {
			throw error("credential appears to be a JWT (SD-JWT VC) rather than base64url-encoded mdoc. " +
				"Check that the issuer is configured to return mso_mdoc format.",
				args("credential_preview", mdocBase64.substring(0, Math.min(100, mdocBase64.length()))));
		}

		byte[] bytes;
		try {
			bytes = new Base64URL(mdocBase64).decode();
		} catch (Exception e) {
			throw error("Failed to decode credential as base64url",
				e, args("credential_preview", mdocBase64.substring(0, Math.min(200, mdocBase64.length())),
					"credential_length", mdocBase64.length()));
		}

		if (bytes.length == 0) {
			throw error("Decoded credential is empty (0 bytes)",
				args("credential_preview", mdocBase64.substring(0, Math.min(200, mdocBase64.length()))));
		}

		// Store raw CBOR bytes for downstream signature validation
		env.putString("mdoc_credential_cbor", Base64.getEncoder().encodeToString(bytes));

		String diagnostics;
		try {
			diagnostics = Cbor.INSTANCE.toDiagnostics(bytes,
				Set.of(DiagnosticOption.PRETTY_PRINT, DiagnosticOption.EMBEDDED_CBOR));
		} catch (Exception e) {
			throw error("Failed to parse credential as CBOR. The credential may not be in mso_mdoc format.",
				e, args("credential_preview", mdocBase64.substring(0, Math.min(200, mdocBase64.length())),
					"credential_length", mdocBase64.length(),
					"decoded_bytes_length", bytes.length,
					"first_bytes_hex", bytesToHex(bytes, 32)));
		}

		// Parse as IssuerSigned structure
		DataItem dataItem;
		try {
			dataItem = Cbor.INSTANCE.decode(bytes);
		} catch (Exception e) {
			throw error("Failed to decode credential CBOR structure",
				e, args("cbor_diagnostic", diagnostics,
					"credential_length", mdocBase64.length(),
					"decoded_bytes_length", bytes.length));
		}

		if (!(dataItem instanceof CborMap)) {
			throw error("Expected mdoc credential to be a CBOR map (IssuerSigned structure)",
				args("cbor_diagnostic", diagnostics));
		}

		CborMap issuerSigned = (CborMap) dataItem;

		// Validate required fields
		DataItem nameSpaces = issuerSigned.get("nameSpaces");
		if (nameSpaces == null) {
			throw error("mdoc credential missing required 'nameSpaces' field",
				args("cbor_diagnostic", diagnostics));
		}

		DataItem issuerAuth = issuerSigned.get("issuerAuth");
		if (issuerAuth == null) {
			throw error("mdoc credential missing required 'issuerAuth' field (COSE_Sign1 signature)",
				args("cbor_diagnostic", diagnostics));
		}

		// Log success with the parsed structure
		logSuccess("Parsed mdoc credential (IssuerSigned) from VCI issuance response",
			args("cbor_diagnostic", diagnostics,
				"has_nameSpaces", nameSpaces != null,
				"has_issuerAuth", issuerAuth != null));

		return env;
	}

	/**
	 * Convert first N bytes to hex string for debugging
	 */
	private String bytesToHex(byte[] bytes, int maxBytes) {
		StringBuilder sb = new StringBuilder();
		int limit = Math.min(bytes.length, maxBytes);
		for (int i = 0; i < limit; i++) {
			sb.append(String.format("%02x", bytes[i]));
		}
		if (bytes.length > maxBytes) {
			sb.append("...");
		}
		return sb.toString();
	}
}
