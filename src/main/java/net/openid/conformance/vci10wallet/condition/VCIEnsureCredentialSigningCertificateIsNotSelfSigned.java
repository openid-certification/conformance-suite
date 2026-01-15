package net.openid.conformance.vci10wallet.condition;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.X509CertUtils;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.List;

public class VCIEnsureCredentialSigningCertificateIsNotSelfSigned extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String credentialSigningJwkString = env.getString("vci", "credential_signing_jwk");

		JWK credentialSigningJwk;
		try {
			credentialSigningJwk = JWK.parse(credentialSigningJwkString);
		} catch (ParseException e) {
			throw error("Failed to create JWK from Credential Signing JWK: " + e.getMessage(), args("credential_signing_jwk", credentialSigningJwkString));
		}

		List<Base64> x5c = credentialSigningJwk.getX509CertChain();
		String encodedCert = x5c.get(0).toString();
		byte[] der = java.util.Base64.getDecoder().decode(encodedCert);
		X509Certificate credentialSigningCert = X509CertUtils.parse(der);

		try {
			credentialSigningCert.verify(credentialSigningCert.getPublicKey());
			throw error("Credential signing cert must not be a self-signed cert",
				args("cert_0_from_x5c", encodedCert));
		} catch (Exception e) {
			logSuccess("Credential signing cert is not a self-signed cert",
				args("cert_0_from_x5c", encodedCert));
		}

		return env;
	}
}
