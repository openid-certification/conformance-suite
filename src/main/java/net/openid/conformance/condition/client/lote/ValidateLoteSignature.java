package net.openid.conformance.condition.client.lote;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jose.util.X509CertUtils;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

/**
 * Checks the JWS envelope of the list of trusted entities: an asymmetric signature algorithm, the
 * signer's certificate in the x5c protected header (the compact JAdES signature of ETSI TS 119 602
 * Annex D.4 and its siblings carries it there), a signature that verifies under that certificate's
 * key, and an {@code x5t#S256} that names the same certificate when present. Whether the signer is
 * the expected scheme operator is a separate check ({@link ValidateLoteSignerCertificate},
 * {@link ValidateLoteSignerAgainstConfiguredCertificate}).
 */
public class ValidateLoteSignature extends AbstractLoteCondition {

	@Override
	@PreEnvironment(required = LOTE)
	public Environment evaluate(Environment env) {
		JWSObject jws = parseJws(env);
		JWSAlgorithm alg = jws.getHeader().getAlgorithm();
		if (alg == null || JWSAlgorithm.Family.HMAC_SHA.contains(alg) || "none".equals(alg.getName())) {
			throw error("The list of trusted entities is not signed with an asymmetric algorithm",
				args("alg", alg == null ? null : alg.getName()));
		}

		List<X509Certificate> chain = getSignerCertificateChain(jws);
		X509Certificate signer = chain.get(0);

		Base64URL x5tS256 = jws.getHeader().getX509CertSHA256Thumbprint();
		if (x5tS256 != null) {
			Base64URL computed = X509CertUtils.computeSHA256Thumbprint(signer);
			if (!x5tS256.equals(computed)) {
				throw error("The x5t#S256 in the JWS protected header does not match the first x5c certificate",
					args("x5t#S256", x5tS256.toString(), "computed", computed.toString(),
						"signer_certificate_subject", signer.getSubjectX500Principal().getName()));
			}
		}

		checkX5tO(jws, signer);

		boolean verified;
		try {
			verified = verifies(jws, signer);
		} catch (JOSEException e) {
			throw error("Verifying the signature of the list of trusted entities failed", e,
				args("alg", alg.getName(), "signer_certificate_subject", signer.getSubjectX500Principal().getName()));
		}
		if (!verified) {
			throw error("The signature of the list of trusted entities does not verify under the certificate in its x5c header",
				args("alg", alg.getName(), "signer_certificate_subject", signer.getSubjectX500Principal().getName()));
		}

		logSuccess("The signature of the list of trusted entities verifies under the certificate in its x5c header",
			args("alg", alg.getName(),
				"signer_certificate_subject", signer.getSubjectX500Principal().getName(),
				"chain_length", chain.size()));
		return env;
	}

	/** JAdES digAlg names of the digests the suite can compute, to their Java names. */
	private static final Map<String, String> X5T_O_DIGESTS = Map.of("S256", "SHA-256", "S384", "SHA-384", "S512", "SHA-512");

	/**
	 * Checks the JAdES {@code x5t#o} header (ETSI TS 119 182-1 clause 5.2.2.2), the signer certificate
	 * digest the Commission's lists carry instead of {@code x5t#S256}, against the first x5c certificate.
	 */
	private void checkX5tO(JWSObject jws, X509Certificate signer) {
		Object x5tO = jws.getHeader().getCustomParam("x5t#o");
		if (x5tO == null) {
			return;
		}
		if (!(x5tO instanceof Map<?, ?> digest) || !(digest.get("digAlg") instanceof String digAlg)
			|| !(digest.get("digVal") instanceof String digVal)) {
			throw error("The x5t#o in the JWS protected header is not an object with string digAlg and digVal members",
				args("x5t#o", String.valueOf(x5tO)));
		}
		String javaName = X5T_O_DIGESTS.get(digAlg);
		if (javaName == null) {
			log("The x5t#o in the JWS protected header uses a digest algorithm the suite does not check",
				args("digAlg", digAlg));
			return;
		}
		Base64URL computed;
		try {
			computed = Base64URL.encode(MessageDigest.getInstance(javaName).digest(signer.getEncoded()));
		} catch (NoSuchAlgorithmException | CertificateEncodingException e) {
			throw error("Computing the digest of the first x5c certificate failed", e, args("digAlg", digAlg));
		}
		if (!computed.toString().equals(digVal)) {
			throw error("The x5t#o in the JWS protected header does not match the first x5c certificate",
				args("digAlg", digAlg, "digVal", digVal, "computed", computed.toString(),
					"signer_certificate_subject", signer.getSubjectX500Principal().getName()));
		}
	}
}
