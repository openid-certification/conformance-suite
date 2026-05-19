package net.openid.conformance.condition.as.clientattestation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractValidateX5cCertificateChain;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Validates the client attestation JWT signature using the public key from the x5c header.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth">OAuth 2.0 Attestation-Based Client Authentication</a>
 */
public class ValidateClientAttestationSignature extends AbstractValidateX5cCertificateChain {

	@Override
	@PreEnvironment(required = "client_attestation_object", strings = "client_attestation")
	public Environment evaluate(Environment env) {

		String clientAttestation = env.getString("client_attestation");

		JsonElement x5cEl = env.getElementFromObject("client_attestation_object", "header.x5c");
		if (x5cEl == null || !x5cEl.isJsonArray() || x5cEl.getAsJsonArray().isEmpty()) {
			throw error("Client attestation does not have x5c header or x5c is empty",
				args("client_attestation", clientAttestation));
		}

		List<String> x5c = OIDFJSON.convertJsonArrayToList(x5cEl.getAsJsonArray());
		List<X509Certificate> certs = parseX5cCertificatesFromStrings(x5c);

		verifyJwtSignatureWithX5cLeafCert(clientAttestation, certs);

		logSuccess("Client attestation JWT signature verified successfully using x5c public key",
			args("client_attestation", clientAttestation,
				"leaf_cert_subject", certs.get(0).getSubjectX500Principal().getName()));

		return env;
	}
}
