package net.openid.conformance.condition.client.lote;

import com.nimbusds.jose.JWSObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.X509CertificateUtil;

import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Checks that the list of trusted entities is signed by the scheme operator the tester expects:
 * the certificate in the JWS x5c header must be the configured 'LoTE signing certificate', or
 * chain to it. With no certificate configured there is nothing to compare against and the condition
 * passes; {@link WarnIfLoteSigningCertificateNotConfigured} is what reports that.
 */
public class ValidateLoteSignerAgainstConfiguredCertificate extends AbstractLoteCondition {

	@Override
	@PreEnvironment(required = { LOTE, "config" })
	public Environment evaluate(Environment env) {
		// an unconfigured certificate is WarnIfLoteSigningCertificateNotConfigured's to report
		String configuredPem = LoteSlot.configuredSigningCertificatePem(env);
		if (configuredPem == null) {
			log("No '" + LoteSlot.SIGNING_CERTIFICATE_FIELD_LABEL + "' is configured; the list's signer is not checked against one");
			return env;
		}
		X509Certificate configured;
		try {
			configured = X509CertificateUtil.parseTrustAnchorPem(configuredPem);
		} catch (X509CertificateUtil.X5cCertificateChainException e) {
			throw error("The '" + LoteSlot.SIGNING_CERTIFICATE_FIELD_LABEL + "' field in the '"
				+ LoteSlot.SIGNING_CERTIFICATE_SECTION_LABEL + "' section of the test configuration is not a PEM certificate: "
				+ e.getMessage());
		}

		JWSObject jws = parseJws(env);
		List<X509Certificate> chain = getSignerCertificateChain(jws);
		X509Certificate signer = chain.get(0);

		if (signer.equals(configured)) {
			logSuccess("The list of trusted entities is signed with the configured LoTE signing certificate",
				args("signer_certificate_subject", signer.getSubjectX500Principal().getName()));
			return env;
		}

		try {
			checkSigner(chain, configured);
		} catch (X509CertificateUtil.X5cCertificateChainException e) {
			throw error("The list of trusted entities is signed with a certificate that is neither the configured LoTE signing"
					+ " certificate nor chains to it: " + e.getMessage(),
				args("signer_certificate_subject", signer.getSubjectX500Principal().getName(),
					"configured_certificate_subject", configured.getSubjectX500Principal().getName()));
		}
		logSuccess("The list of trusted entities is signed with a certificate that chains to the configured LoTE signing certificate",
			args("signer_certificate_subject", signer.getSubjectX500Principal().getName(),
				"configured_certificate_subject", configured.getSubjectX500Principal().getName()));
		return env;
	}

	/**
	 * Checks that the list's signer, the first certificate of {@code chain}, is {@code configured} or
	 * chains to it. A JAdES signature may carry the full chain, so {@code configured} may itself be in
	 * {@code chain}; it is the anchor here, so it is taken out before the path is validated.
	 *
	 * @throws X509CertificateUtil.X5cCertificateChainException when the signer neither is nor chains to it
	 */
	static void checkSigner(List<X509Certificate> chain, X509Certificate configured) throws X509CertificateUtil.X5cCertificateChainException {
		if (chain.get(0).equals(configured)) {
			return;
		}
		X509CertificateUtil.validateX5cCertificateChain(chain.stream().filter(cert -> !cert.equals(configured)).toList(), configured);
	}
}
