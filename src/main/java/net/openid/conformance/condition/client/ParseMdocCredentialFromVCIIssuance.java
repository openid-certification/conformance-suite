package net.openid.conformance.condition.client;

import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.CborMap;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.DiagnosticOption;

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
	public Environment evaluate(Environment env) {
		String mdocBase64 = env.getString("credential");

		byte[] bytes = new Base64URL(mdocBase64).decode();

		String diagnostics = Cbor.INSTANCE.toDiagnostics(bytes,
			Set.of(DiagnosticOption.PRETTY_PRINT, DiagnosticOption.EMBEDDED_CBOR));

		// Parse as IssuerSigned structure
		DataItem dataItem = Cbor.INSTANCE.decode(bytes);

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
}
