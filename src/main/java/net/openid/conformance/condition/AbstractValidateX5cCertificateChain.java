package net.openid.conformance.condition;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.proc.JWSVerifierFactory;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.client.lote.LoteSlot;
import net.openid.conformance.condition.client.lote.LoteTrustAnchors;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.X509CertificateUtil;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.List;

/**
 * Abstract base condition for validating x5c certificate chains.
 *
 * Provides shared helper methods for:
 * - Parsing x5c certificate lists from Base64 DER strings or Nimbus Base64 objects
 * - Validating the chain: validity dates, self-signed checks, chain signature walking,
 *   trust anchor exclusion
 * - Verifying a JWT signature using the leaf certificate from the chain
 *
 * Per RFC 7515 section 4.1.6, the leaf certificate (containing the key used to sign)
 * MUST be the first certificate. Subsequent certificates each certify the previous one.
 * Per HAIP, the trust anchor MUST NOT be included in the chain and the leaf MUST NOT
 * be self-signed.
 */
public abstract class AbstractValidateX5cCertificateChain extends AbstractCondition {

	/**
	 * Parse x5c certificates from a list of Base64-encoded DER strings
	 * (as found in JSON-sourced x5c arrays).
	 */
	protected List<X509Certificate> parseX5cCertificatesFromStrings(List<String> base64DerCerts) {
		try {
			return X509CertificateUtil.parseX5cCertificatesFromStrings(base64DerCerts);
		} catch (X509CertificateUtil.X5cCertificateChainException e) {
			throw error(e.getMessage());
		}
	}

	/**
	 * Parse x5c certificates from a list of Nimbus Base64 objects
	 * (as returned by JWSHeader.getX509CertChain()).
	 */
	protected List<X509Certificate> parseX5cCertificatesFromNimbusBase64(List<com.nimbusds.jose.util.Base64> base64Certs) {
		try {
			return X509CertificateUtil.parseX5cCertificatesFromNimbusBase64(base64Certs);
		} catch (X509CertificateUtil.X5cCertificateChainException e) {
			throw error(e.getMessage());
		}
	}

	/**
	 * Validate an x5c certificate chain against a set of trust anchors (a configured PEM, or the
	 * anchors of the list of trusted entities covering the purpose, see
	 * {@link LoteTrustAnchors#resolve}). With anchors this is full RFC 5280 PKIX path validation
	 * (intermediate validity, BasicConstraints CA:true and KeyUsage keyCertSign on intermediates,
	 * name chaining, critical extensions; CRL/OCSP revocation checking is disabled). An empty set
	 * means the legacy walk (parent-signature walk only), unless a list of trusted entities was
	 * configured for the purpose: one that lists no anchor for it trusts nothing, so the chain is
	 * rejected. In both modes the chain must be non-empty, the leaf within its validity dates and
	 * not self-signed, and no anchor may appear in the chain.
	 *
	 * @return the anchor the path ended at, or null for the legacy walk
	 */
	protected X509Certificate validateX5cCertificateChainAgainst(List<X509Certificate> certs, LoteTrustAnchors.TrustAnchorSet anchors) {
		if (anchors.trustsNothing()) {
			throw error("The configured list of trusted entities lists no trust anchor for this certificate chain, so the chain"
					+ " is not trusted. An empty list, or one listing only services of other types, trusts nobody.",
				args("lists_of_trusted_entities", anchors.configuredLists(),
					"leaf_cert_subject", certs.isEmpty() ? null : certs.get(0).getSubjectX500Principal().getName()));
		}
		try {
			return X509CertificateUtil.validateX5cCertificateChain(certs, anchors.certs());
		} catch (X509CertificateUtil.X5cCertificateChainException e) {
			if (anchors.isEmpty()) {
				throw error(e.getMessage());
			}
			throw error(e.getMessage(), args("trust_anchors_tried", anchors.sourcesJson()));
		}
	}

	/**
	 * The trust anchors for a purpose: the configured PEM when set, otherwise the anchors of the given
	 * lists of trusted entities; a misconfigured PEM surfaces as a test failure.
	 */
	protected LoteTrustAnchors.TrustAnchorSet resolveTrustAnchors(Environment env, String configuredPem, String configuredLabel,
			String serviceTypeSuffix, LoteSlot... slots) {
		try {
			return LoteTrustAnchors.resolve(env, configuredPem, configuredLabel, serviceTypeSuffix, slots);
		} catch (X509CertificateUtil.X5cCertificateChainException e) {
			throw error(e.getMessage());
		}
	}

	/**
	 * Verify a JWT signature using the leaf certificate's public key from the x5c chain.
	 *
	 * @param jwtString the raw JWT string
	 * @param certs the parsed x5c certificate chain (leaf first)
	 */
	protected void verifyJwtSignatureWithX5cLeafCert(String jwtString, List<X509Certificate> certs) {
		X509Certificate leafCert = certs.get(0);
		PublicKey publicKey = leafCert.getPublicKey();

		try {
			SignedJWT jwt = SignedJWT.parse(jwtString);
			JWSVerifierFactory factory = new DefaultJWSVerifierFactory();
			JWSVerifier verifier = factory.createJWSVerifier(jwt.getHeader(), publicKey);

			if (!jwt.verify(verifier)) {
				throw error("JWT signature verification failed using the leaf certificate from x5c",
					args("leaf_cert_subject", leafCert.getSubjectX500Principal().getName()));
			}
		} catch (ParseException e) {
			throw error("Failed to parse JWT for signature verification", e,
				args("jwt", jwtString));
		} catch (JOSEException e) {
			throw error("Error verifying JWT signature using x5c leaf certificate", e,
				args("leaf_cert_subject", leafCert.getSubjectX500Principal().getName()));
		}
	}
}
