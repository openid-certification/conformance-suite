package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdocUtil;
import net.openid.conformance.util.X509CertificateUtil;
import org.multipaz.cbor.Cbor;
import org.multipaz.crypto.X509Cert;
import org.multipaz.crypto.X509CertChain;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Validates the certification path of the MSO revocation list's x5chain as per ISO/IEC 18013-5
 * 12.3.6.2: when the MSO's status reference carries the optional Certificate element, that
 * certificate is the trust point for the chain; otherwise "the top-level certificate in the
 * x5chain element shall be signed by the certificate used to sign the certificate in the
 * x5chain element of the MSO" — in the context of an mDL, the IACA certificate.
 *
 * Without the Certificate element the same-issuer requirement is checked structurally (issuer
 * name and Authority Key Identifier of the revocation list chain's top certificate must match
 * those of the MSO chain's top certificate), and the chain is PKIX-validated against the
 * 'Credential Trust Anchor' from the test configuration when one is configured. A configured
 * VICAL supersedes the trust anchor (as for the credential chain itself): the VICAL-based
 * trust decision for the credential chain combined with the same-issuer check ties the
 * revocation list to the same IACA, so only the trust-anchor-independent checks run here.
 */
public class ValidateStatusListTokenCwtCertificateChain extends AbstractStatusListCwtCondition {

	@Override
	@PreEnvironment(strings = { ENV_STATUS_LIST_TOKEN, "mdoc_credential_cbor" })
	public Environment evaluate(Environment env) {

		ParsedStatusListCwt parsed = parseStatusListCwt(env);
		X509CertChain statusChain = getProtectedX5chain(parsed.coseSign1());
		if (statusChain == null || statusChain.getCertificates().isEmpty()) {
			throw error("The MSO revocation list token does not contain an x5chain in its protected header, so its certification path cannot be validated");
		}

		List<X509Certificate> statusJavaChain = toJavaCerts(statusChain);
		X509Cert statusTop = statusChain.getCertificates().get(statusChain.getCertificates().size() - 1);

		String referenceCertB64 = env.getString(ENV_STATUS_REFERENCE_CERTIFICATE);
		if (referenceCertB64 != null) {
			// 12.3.6.2: the Certificate element of the status reference is the trust point
			X509Certificate trustPoint = parseJavaCert(Base64.getDecoder().decode(referenceCertB64),
				"the Certificate element of the MSO's status reference");
			try {
				X509CertificateUtil.validateX5cCertificateChain(statusJavaChain, trustPoint);
			} catch (X509CertificateUtil.X5cCertificateChainException e) {
				throw error("The MSO revocation list's x5chain does not validate against the Certificate element of the MSO's status reference",
					args("error", e.getMessage(),
						"trust_point_subject", trustPoint.getSubjectX500Principal().getName()));
			}
			logSuccess("The MSO revocation list's x5chain validates against the Certificate element of the MSO's status reference",
				args("trust_point_subject", trustPoint.getSubjectX500Principal().getName(),
					"chain_length", statusJavaChain.size()));
			return env;
		}

		// no Certificate element: the top-level certificate must be signed by the same CA that
		// signed the MSO's signer certificate (the IACA); check the issuer identity structurally
		X509Cert msoTop = msoChainTop(env);
		String msoIssuer = msoTop.getIssuer().getName();
		String statusIssuer = statusTop.getIssuer().getName();
		if (!statusIssuer.equals(msoIssuer)) {
			throw error("The issuer of the MSO revocation list's top-level certificate does not match the issuer of the MSO's signer certificate; without a Certificate element in the status reference, both must be signed by the same CA (the IACA)",
				args("revocation_list_chain_issuer", statusIssuer,
					"mso_chain_issuer", msoIssuer));
		}
		byte[] statusAki = statusTop.getAuthorityKeyIdentifier();
		byte[] msoAki = msoTop.getAuthorityKeyIdentifier();
		if (statusAki != null && msoAki != null && !Arrays.equals(statusAki, msoAki)) {
			throw error("The Authority Key Identifier of the MSO revocation list's top-level certificate does not match that of the MSO's signer certificate; without a Certificate element in the status reference, both must be signed by the same CA (the IACA)",
				args("revocation_list_chain_aki", hex(statusAki),
					"mso_chain_aki", hex(msoAki)));
		}

		String trustAnchorPem = env.getString("credential_trust_anchor_pem");
		boolean vicalConfigured = env.containsObject("vical");
		X509Certificate trustAnchor = null;
		if (!vicalConfigured && trustAnchorPem != null) {
			trustAnchor = parseJavaCert(pemToDer(trustAnchorPem),
				"the 'Credential Trust Anchor' field in the 'Credential Issuer' section of the test configuration");
		}
		try {
			// with a trust anchor this is full PKIX path validation; without one (VICAL
			// configured, or no anchor outside HAIP) only the chain-internal checks run
			X509CertificateUtil.validateX5cCertificateChain(statusJavaChain, trustAnchor);
		} catch (X509CertificateUtil.X5cCertificateChainException e) {
			throw error("The MSO revocation list's x5chain does not validate"
					+ (trustAnchor == null ? "" : " against the 'Credential Trust Anchor' from the test configuration"),
				args("error", e.getMessage()));
		}

		logSuccess("The MSO revocation list's x5chain is signed by the same CA as the MSO's signer certificate"
				+ (trustAnchor != null ? " and validates against the 'Credential Trust Anchor'"
					: vicalConfigured ? "; the IACA itself is checked against the VICAL by the credential chain validation"
					: "; no trust anchor is configured, so only chain-internal checks were performed"),
			args("issuer", statusIssuer, "chain_length", statusJavaChain.size()));

		return env;
	}

	private X509Cert msoChainTop(Environment env) {
		try {
			byte[] mdoc = Base64.getDecoder().decode(env.getString("mdoc_credential_cbor"));
			X509CertChain msoChain = MdocUtil.extractX5chain(Cbor.INSTANCE.decode(mdoc));
			return msoChain.getCertificates().get(msoChain.getCertificates().size() - 1);
		} catch (Exception e) {
			throw error("Failed to extract the x5chain from the mdoc issuerAuth", e);
		}
	}

	private List<X509Certificate> toJavaCerts(X509CertChain chain) {
		List<X509Certificate> certs = new ArrayList<>();
		for (X509Cert cert : chain.getCertificates()) {
			certs.add(parseJavaCert(cert.getEncoded().toByteArray(0, cert.getEncoded().getSize()), "the MSO revocation list's x5chain"));
		}
		return certs;
	}

	private X509Certificate parseJavaCert(byte[] der, String description) {
		try {
			return (X509Certificate) CertificateFactory.getInstance("X.509")
				.generateCertificate(new ByteArrayInputStream(der));
		} catch (Exception e) {
			throw error("Failed to parse a certificate from " + description, e);
		}
	}

	private byte[] pemToDer(String pem) {
		String base64 = pem.replaceAll("-----(BEGIN|END) CERTIFICATE-----", "").replaceAll("\\s", "");
		return Base64.getDecoder().decode(base64);
	}

	private String hex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
}
