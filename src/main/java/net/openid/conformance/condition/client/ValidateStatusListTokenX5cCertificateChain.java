package net.openid.conformance.condition.client;

import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.X509CertUtils;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractValidateX5cCertificateChain;
import net.openid.conformance.condition.Profile;
import net.openid.conformance.testmodule.Environment;

import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.List;

/**
 * Validates the x5c certificate chain and signature of the Token Status List JWT
 * per HAIP section 6.1.
 * <ul>
 *   <li>x5c header MUST be present</li>
 *   <li>The chain MUST validate (dates, signatures, leaf not self-signed,
 *       trust anchor excluded)</li>
 *   <li>The Status List Token signature MUST verify using the leaf certificate</li>
 * </ul>
 *
 * Skips when no status list fetch was performed (credential had no status claim).
 */
public class ValidateStatusListTokenX5cCertificateChain extends AbstractValidateX5cCertificateChain {

	@Override
	public Environment evaluate(Environment env) {

		if (!env.containsObject("status_list_token")) {
			log("No parsed status list token in environment, skipping x5c validation");
			return env;
		}

		String statusListTokenJwtString = env.getString("status_list_token", "value");
		if (statusListTokenJwtString == null) {
			throw error("status_list_token has no raw JWT value");
		}

		SignedJWT statusListTokenJwt;
		try {
			statusListTokenJwt = SignedJWT.parse(statusListTokenJwtString);
		} catch (ParseException e) {
			throw error("Unable to parse status list token", e);
		}

		List<Base64> x5cChain = statusListTokenJwt.getHeader().getX509CertChain();
		if (x5cChain == null || x5cChain.isEmpty()) {
			throw error("Status List Token MUST contain an x5c JOSE header in HAIP",
				args("header", statusListTokenJwt.getHeader().toJSONObject()));
		}

		List<X509Certificate> certs = parseX5cCertificatesFromNimbusBase64(x5cChain);

		String trustAnchorPem = env.getString("status_list_trust_anchor_pem");
		X509Certificate trustAnchor = trustAnchorPem != null ? X509CertUtils.parse(trustAnchorPem) : null;
		validateX5cCertificateChain(certs, trustAnchor, Profile.isHaip(env));

		verifyJwtSignatureWithX5cLeafCert(statusListTokenJwtString, certs);

		logSuccess("Validated status list token x5c certificate chain and signature",
			args("leaf_cert_subject", certs.get(0).getSubjectX500Principal().getName(),
				"chain_length", certs.size()));
		return env;
	}
}
