package net.openid.conformance.vci10issuer.condition;

import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractValidateX5cCertificateChain;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.lote.LoteTrustAnchors;
import net.openid.conformance.condition.client.lote.TrustPurpose;
import net.openid.conformance.testmodule.Environment;

import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.List;

/**
 * Validates the x5c chain of the registration certificate in {@code issuer_info} against the
 * configured registration certificate trust source (the 'Registration Certificate Trust Anchor' or
 * the registration certificate providers of the WRPRC providers LoTE), and its signature with the
 * leaf: the certificate is signed by a provider of registration certificates.
 */
public class VCIValidateIssuerInfoRegistrationCertificateSignature extends AbstractValidateX5cCertificateChain {

	@Override
	@PreEnvironment(required = VCIExtractIssuerInfoRegistrationCertificate.ENV_KEY)
	public Environment evaluate(Environment env) {
		String value = env.getString(VCIExtractIssuerInfoRegistrationCertificate.ENV_KEY, "value");

		SignedJWT jwt;
		try {
			jwt = SignedJWT.parse(value);
		} catch (ParseException e) {
			throw error("Unable to parse the registration certificate in 'issuer_info'", e, args("registration_certificate", value));
		}

		List<Base64> x5c = jwt.getHeader().getX509CertChain();
		if (x5c == null || x5c.isEmpty()) {
			throw error("The header of the registration certificate in 'issuer_info' has no 'x5c', so its signature cannot be"
				+ " verified", args("header", jwt.getHeader().toJSONObject()));
		}
		List<X509Certificate> certs = parseX5cCertificatesFromNimbusBase64(x5c);

		LoteTrustAnchors.TrustAnchorSet anchors = resolveTrustAnchors(env, TrustPurpose.REGISTRATION_CERTIFICATE);
		X509Certificate matched = validateX5cCertificateChainAgainst(certs, anchors);

		verifyJwtSignatureWithX5cLeafCert(value, certs);

		logSuccess("Validated the registration certificate's x5c certificate chain and signature",
			args("leaf_cert_subject", certs.get(0).getSubjectX500Principal().getName(),
				"chain_length", certs.size(),
				"trust_anchors", anchors.sourcesJson(),
				"matched_trust_anchor", anchors.sourceOf(matched)));
		return env;
	}
}
