package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.JWK;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CheckIfSecondClientIdInX509CertSanDns extends AbstractGetSigningKey {

	@Override
	@PreEnvironment(required = {"client2_jwks"}, strings = "orig_client2_id")
	public Environment evaluate(Environment env) {
		JsonObject jwks = env.getObject("client2_jwks");
		JWK jwk = getSigningKey("client2", jwks);
		List<X509Certificate> certChain = jwk.getParsedX509CertChain();
		if (certChain == null) {
			throw error("x509_san_dns has been selected as the second client authentication method, but the first key in client2_jwks does not contain an 'x5c' header");
		}
		X509Certificate certificate = certChain.get(0);

		Collection<List<?>> sanList;
		try {
			sanList = certificate.getSubjectAlternativeNames();
		} catch (CertificateParsingException e) {
			throw error("Parsing subject alternative names from x5c certificate failed", e, args("cert", certificate.toString()));
		}
		if (sanList == null) {
			throw error("x5c certificate does not contain a SAN (subject alternative names) entry");
		}
		List<String> dnsNames = new ArrayList<>();
		sanList.forEach((entry) -> {
			Object type = entry.get(0);
			if (!(type instanceof Integer typeInteger)) {
				throw error("Non-integer type found in certificate SAN", args("cert", certificate.toString()));
			}
			final int x509SanNameDns = 2;
			if (typeInteger == x509SanNameDns) {
				Object value = entry.get(1);
				if (!(value instanceof String)) {
					throw error("Non-string value found in certificate SAN dnsName", args("cert", certificate.toString()));
				}
				dnsNames.add((String) value);
			}
		});

		String clientId = env.getString("orig_client2_id");

		if (!dnsNames.contains(clientId)) {
			throw error("The second x509_san_dns client_id is not present in the x5c certificate SAN in client2_jwks. This must cause the wallet to reject the request due to the client_id used not being authorized.",
				args("client2_jwks", jwks,
					"san", sanList.toString(),
					"cert", certificate.toString(),
					"dns_names_from_cert", dnsNames,
					"client2_id", clientId
				));
		}

		logSuccess("The second x509_san_dns client_id is present in the x5c certificate SAN in client2_jwks",
			args("client2_jwks", jwks,
				"san", sanList.toString(),
				"cert", certificate.toString(),
				"dns_names_from_cert", dnsNames,
				"client2_id", clientId
			));

		return env;
	}

}
