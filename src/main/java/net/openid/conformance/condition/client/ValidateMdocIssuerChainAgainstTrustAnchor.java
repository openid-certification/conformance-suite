package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractValidateX5cCertificateChain;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdocUtil;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DataItem;
import org.multipaz.crypto.X509Cert;
import org.multipaz.crypto.X509CertChain;
import org.multipaz.crypto.X509CertJvmKt;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Validates the x5chain of the mdoc credential's issuerAuth against the configured
 * 'Credential Trust Anchor' (the IACA root certificate) using RFC 5280 PKIX path validation,
 * per the ISO/IEC 18013-5 clause 9.3.1 inspection procedure. This is the fallback mdoc issuer
 * trust check for when no VICAL is configured; a configured VICAL takes precedence and this
 * condition then does nothing.
 */
public class ValidateMdocIssuerChainAgainstTrustAnchor extends AbstractValidateX5cCertificateChain {

	@Override
	@PreEnvironment(strings = { "mdoc_credential_cbor", "credential_trust_anchor_pem" })
	public Environment evaluate(Environment env) {

		if (env.getObject("vical") != null) {
			log("A VICAL is configured, so it is used for mdoc issuer trust instead of the credential trust anchor");
			return env;
		}

		DataItem issuerSigned;
		try {
			issuerSigned = Cbor.INSTANCE.decode(Base64.getDecoder().decode(env.getString("mdoc_credential_cbor")));
		} catch (Exception e) {
			throw error("Failed to parse the mdoc IssuerSigned structure", e);
		}

		X509CertChain certChain;
		try {
			certChain = MdocUtil.getIssuerAuthCertChain(issuerSigned);
		} catch (Exception e) {
			throw error("Failed to extract the x5chain from the mdoc issuerAuth", e);
		}

		List<X509Certificate> javaCerts = new ArrayList<>();
		try {
			for (X509Cert cert : certChain.getCertificates()) {
				javaCerts.add(X509CertJvmKt.getJavaX509Certificate(cert));
			}
		} catch (Exception e) {
			throw error("Failed to parse a certificate from the mdoc issuerAuth x5chain as X.509", e);
		}

		X509Certificate trustAnchor = parseTrustAnchorPem(env.getString("credential_trust_anchor_pem"));

		validateX5cCertificateChain(javaCerts, trustAnchor);

		logSuccess("The mdoc issuer certificate chain validates to the configured credential trust anchor",
			args("issuer_certificate_subject", javaCerts.get(0).getSubjectX500Principal().getName(),
				"trust_anchor_subject", trustAnchor.getSubjectX500Principal().getName(),
				"chain_length", javaCerts.size()));

		return env;
	}
}
