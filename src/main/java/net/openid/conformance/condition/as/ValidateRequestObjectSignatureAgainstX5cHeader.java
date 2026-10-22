package net.openid.conformance.condition.as;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractValidateX5cCertificateChain;
import net.openid.conformance.condition.client.lote.LoteTrustAnchors;
import net.openid.conformance.condition.client.lote.TrustPurpose;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.List;

public class ValidateRequestObjectSignatureAgainstX5cHeader extends AbstractValidateX5cCertificateChain {

	@Override
	@PreEnvironment(required = { "authorization_request_object" })
	@PostEnvironment(strings = "request_object_signing_alg")
	public Environment evaluate(Environment env) {

		String requestObject = env.getString("authorization_request_object", "value");

		try {
			SignedJWT jwt = SignedJWT.parse(requestObject);
			List<Base64> x5c = jwt.getHeader().getX509CertChain();
			if (x5c == null || x5c.isEmpty()) {
				throw error("Request object JWT does not contain an x5c header",
					args("header", jwt.getHeader().toJSONObject()));
			}

			List<X509Certificate> certs = parseX5cCertificatesFromNimbusBase64(x5c);

			// the configured 'Request Object Trust Anchor' plus the access certificate providers listed in the configured LoTE
			LoteTrustAnchors.TrustAnchorSet anchors = resolveTrustAnchors(env, TrustPurpose.REQUEST_OBJECT);
			X509Certificate matched = validateX5cCertificateChainAgainst(certs, anchors);

			// Check request_object_signing_alg if configured
			var client = env.getObject("client");
			if (client != null && client.has("request_object_signing_alg")) {
				String expectedAlg = OIDFJSON.getString(client.get("request_object_signing_alg"));
				JWSAlgorithm actualAlg = jwt.getHeader().getAlgorithm();
				if (!actualAlg.getName().equals(expectedAlg)) {
					throw error("Algorithm in JWT header does not match client request_object_signing_alg",
						args("actual", actualAlg.getName(), "expected", expectedAlg));
				}
			}

			verifyJwtSignatureWithX5cLeafCert(requestObject, certs);

			String alg = jwt.getHeader().getAlgorithm().getName();
			env.putString("request_object_signing_alg", alg);
			logSuccess("Request object x5c chain validated and signature verified",
				args("request_object_signing_alg", alg,
					"leaf_cert_subject", certs.get(0).getSubjectX500Principal().getName(),
					"chain_length", certs.size(),
					"trust_anchors", anchors.sourcesJson(),
					"matched_trust_anchor", anchors.sourceOf(matched)));
			return env;

		} catch (ParseException e) {
			throw error("Error parsing request object JWT", e);
		}
	}
}
