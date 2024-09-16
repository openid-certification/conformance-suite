package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.JWK;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CheckIfClientIdInX509CertSanDns extends AbstractGetSigningKey {

	@Override
	@PreEnvironment(required = {"config", "client_jwks"})
	public Environment evaluate(Environment env) {
		JsonObject jwks = env.getObject("client_jwks");
		JWK jwk = getSigningKey("client", jwks);
		List<X509Certificate> certChain = jwk.getParsedX509CertChain();
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
			// As per javadoc for getSubjectAlternativeNames:
			// Each entry is a List whose first entry is an Integer (the name type, 0-8) and whose second entry is a String or a byte array (the name, in string or ASN.1 DER encoded form, respectively).
			Object type = entry.get(0);
			if (!(type instanceof Integer typeInteger)) {
				throw error("Non-integer type found in certificate SAN", args("cert", certificate.toString()));
			}
			final int X509_SAN_NAME_DNS = 2;
			if (typeInteger == X509_SAN_NAME_DNS) {
				Object value = entry.get(1);
				if (!(value instanceof String)) {
					throw error("Non-string value found in certificate SAN dnsName", args("cert", certificate.toString()));
				}
				dnsNames.add((String)value);
			}
		});

		String client_id = env.getString("config", "client.client_id");
		if (Strings.isNullOrEmpty(client_id)) {
			throw error("No client_id found in configuration");
		}

		if (!dnsNames.contains(client_id)) {
			throw error("x509_san_dns client_id is not present in the x5c certificate SAN in the client_jwks x5c parameter. This must cause the wallet to reject the request due to the client_id used not being authorized.",
				args("client_jwks", jwks,
					"san", sanList.toString(),
					"cert", certificate.toString(),
					"dns_names_from_cert", dnsNames,
					"client_id", client_id
				));
		}

		logSuccess("x509_san_dns client_id is present in the x5c certificate SAN in the client_jwks x5c parameter",
			args("client_jwks", jwks,
				"san", sanList.toString(),
				"cert", certificate.toString(),
				"dns_names_from_cert", dnsNames,
				"client_id", client_id
			));

		return env;
	}

}
