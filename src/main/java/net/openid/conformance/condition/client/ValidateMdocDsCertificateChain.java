package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractValidateX5cCertificateChain;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdocUtil;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DataItem;
import org.multipaz.crypto.X509Cert;
import org.multipaz.crypto.X509CertChain;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Validates the X.509 certificate chain in the x5chain header of an mdoc's issuerAuth
 * against the configured trust anchor, mirroring the SD-JWT VC x5c chain validation.
 *
 * Per ISO/IEC 18013-5 clause 9.3.1 the mdoc verifier performs certification path validation
 * of the document signer certificate up to a trusted IACA root certificate, and per
 * clause 9.1.2.4 the IACA root certificate itself must not be included in the x5chain.
 * When a trust anchor is configured ('Trust anchor PEM' in the 'Credential Issuer' section
 * of the test configuration) full PKIX path validation is performed; otherwise only the
 * reduced checks (leaf validity, leaf not self-signed, parent-signature walk) run.
 */
public class ValidateMdocDsCertificateChain extends AbstractValidateX5cCertificateChain {

	@Override
	@PreEnvironment(strings = { "mdoc_credential_cbor" })
	public Environment evaluate(Environment env) {
		String mdocCborBase64 = env.getString("mdoc_credential_cbor");
		if (mdocCborBase64 == null || mdocCborBase64.isEmpty()) {
			throw error("mdoc_credential_cbor is null or empty");
		}

		DataItem issuerSigned;
		try {
			issuerSigned = Cbor.INSTANCE.decode(Base64.getDecoder().decode(mdocCborBase64));
		} catch (Exception e) {
			throw error("Failed to decode IssuerSigned CBOR", e);
		}

		X509CertChain certChain;
		try {
			certChain = MdocUtil.extractX5chain(issuerSigned);
		} catch (MdocUtil.MdocParseException e) {
			throw error(e.getMessage(), e);
		}

		List<String> base64Certs = new ArrayList<>();
		for (X509Cert cert : certChain.getCertificates()) {
			var encoded = cert.getEncoded();
			base64Certs.add(Base64.getEncoder().encodeToString(encoded.toByteArray(0, encoded.getSize())));
		}
		List<X509Certificate> certs = parseX5cCertificatesFromStrings(base64Certs);

		String trustAnchorPem = env.getString("credential_trust_anchor_pem");
		X509Certificate trustAnchor = parseTrustAnchorPem(trustAnchorPem);
		validateX5cCertificateChain(certs, trustAnchor);

		logSuccess("Validated the mdoc x5chain document signer certificate chain",
			args("leaf_cert_subject", certs.get(0).getSubjectX500Principal().getName(),
				"chain_length", certs.size(),
				"pkix_validated_against_trust_anchor", trustAnchor != null));

		return env;
	}
}
