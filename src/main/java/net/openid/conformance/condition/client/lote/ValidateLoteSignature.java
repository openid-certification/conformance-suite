package net.openid.conformance.condition.client.lote;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jose.util.X509CertUtils;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.security.cert.X509Certificate;
import java.util.List;

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
}
