package net.openid.conformance.condition.client;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.X509CertUtils;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.security.cert.X509Certificate;
import java.util.List;

public class SetClient2IdToX509Hash extends AbstractGetSigningKey {

	@Override
	@PreEnvironment(required = "client2_jwks")
	@PostEnvironment(strings = "client2_id")
	public Environment evaluate(Environment env) {
		JWK signingJwk = getSigningKey("signing", env.getObject("client2_jwks"));

		List<Base64> x5c = signingJwk.getX509CertChain();
		if (x5c == null || x5c.isEmpty()) {
			throw error("An x509 certificate is needed for the second client identity in multi-signed requests, " +
				"but the signing key in the 'Second Client JWKS' in the test configuration doesn't have an x5c entry",
				args("client2_jwks", env.getObject("client2_jwks")));
		}
		Base64 certBase64 = x5c.get(0);
		X509Certificate cert = X509CertUtils.parse(certBase64.decode());
		String thumbprint = X509CertUtils.computeSHA256Thumbprint(cert).toString();
		String client2Id = "x509_hash:" + thumbprint;

		env.putString("client2_id", client2Id);

		logSuccess("Set second client_id to x509 hash",
			args("client2_id", client2Id,
				"x5c_certificate", certBase64.toString()));

		return env;
	}

}
