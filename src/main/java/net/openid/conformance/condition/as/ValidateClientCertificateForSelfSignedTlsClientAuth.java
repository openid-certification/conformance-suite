package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWKUtil;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.ParseException;

public class ValidateClientCertificateForSelfSignedTlsClientAuth extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"client_certificate", "client"})
	public Environment evaluate(Environment env) {

		JsonObject certInfo = env.getObject("client_certificate");
		JsonObject client = env.getObject("client");
		X509Certificate expectedCertificate;
		String certPem = OIDFJSON.getString(certInfo.get("pem"));
		try {
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			expectedCertificate = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certPem.getBytes()));
		} catch (CertificateException ex) {
			throw error("Invalid certificate", ex, args("certificate_pem", certPem));
		}
		JsonObject clientJwks = client.get("jwks").getAsJsonObject();
		try {
			JWKSet jwkSet = JWKUtil.parseJWKSet(clientJwks.toString());
			for(JWK jwk : jwkSet.getKeys()) {
				//Also note that Section 4.7 of
				//   [RFC7517] requires that the key in the first certificate of the "x5c"
				//   parameter match the public key represented by those other members of
				//   the JWK.
				if(jwk.getParsedX509CertChain()!=null) {
					X509Certificate cert = jwk.getParsedX509CertChain().get(0);
					if(expectedCertificate.equals(cert)) {
						logSuccess("Valid client certificate found in request",
									args("certificate", certInfo));
						return env;
					}
				}
			}
		} catch (ParseException e) {
			throw error("Failed to parse client jwks", e);
		}
		throw error("Could not find a certificate in client jwks matching the one in the request",
					args("jwks", clientJwks, "certificate_in_request", certInfo));
	}


}
