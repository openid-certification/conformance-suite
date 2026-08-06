package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonElement;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.X509CertUtils;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.X509CertificateUtil;

import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.List;

public class VCIEnsureCredentialSigningCertificateIsNotSelfSigned extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {

		JsonElement credentialSigningJwkEl = env.getElementFromObject("config", "credential.signing_jwk");
		if (credentialSigningJwkEl == null) {
			throw error("'Signing JWK' field is missing from the 'Credential Issuer' section in the test configuration");
		}
		String credentialSigningJwkString = credentialSigningJwkEl.toString();

		JWK credentialSigningJwk;
		try {
			credentialSigningJwk = JWK.parse(credentialSigningJwkString);
		} catch (ParseException e) {
			throw error("Failed to parse the 'Signing JWK' field in the 'Credential Issuer' section of the test configuration: " + e.getMessage(),
				args("signing_jwk", credentialSigningJwkString));
		}

		List<Base64> x5c = credentialSigningJwk.getX509CertChain();
		if (x5c == null || x5c.isEmpty()) {
			throw error("'Signing JWK' field in the 'Credential Issuer' section of the test configuration must contain the signing certificate chain in its 'x5c' claim",
				args("signing_jwk", credentialSigningJwkString));
		}

		String encodedCert = x5c.get(0).toString();
		X509Certificate credentialSigningCert;
		try {
			credentialSigningCert = X509CertUtils.parse(java.util.Base64.getDecoder().decode(encodedCert));
		} catch (IllegalArgumentException e) {
			credentialSigningCert = null;
		}
		if (credentialSigningCert == null) {
			throw error("The first certificate in the 'x5c' claim of the 'Signing JWK' field in the 'Credential Issuer' section of the test configuration could not be parsed as an X.509 certificate",
				args("cert_0_from_x5c", encodedCert));
		}

		// Per HAIP section 6.1.1: Credential signing certificate must NOT be self-signed
		if (X509CertificateUtil.isSelfSigned(credentialSigningCert)) {
			throw error("The first certificate in the 'x5c' claim of the 'Signing JWK' field in the 'Credential Issuer' section of the test configuration is self-signed; HAIP section 6.1.1 requires that the credential signing certificate is not self-signed",
				args("cert_0_from_x5c", encodedCert));
		}

		logSuccess("Credential signing cert is not a self-signed cert",
			args("cert_0_from_x5c", encodedCert));

		return env;
	}
}
