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
 * chain to it. Without this check a list is trusted on its own say-so, which is why
 * {@link WarnIfLoteSigningCertificateNotConfigured} warns when the field is empty.
 */
public class ValidateLoteSignerAgainstConfiguredCertificate extends AbstractLoteCondition {

	@Override
	@PreEnvironment(required = { LOTE, "config" })
	public Environment evaluate(Environment env) {
		String configuredPem = env.getString("config", LoteSlot.SIGNING_CERTIFICATE_CONFIG_KEY);
		if (configuredPem == null || configuredPem.isBlank()) {
			throw error("The '" + LoteSlot.SIGNING_CERTIFICATE_FIELD_LABEL + "' field in the '"
				+ LoteSlot.SIGNING_CERTIFICATE_SECTION_LABEL + "' section of the test configuration is empty");
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
			X509CertificateUtil.validateX5cCertificateChain(chain, configured);
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
}
