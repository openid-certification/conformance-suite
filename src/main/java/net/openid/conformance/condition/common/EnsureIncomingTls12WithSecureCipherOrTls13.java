package net.openid.conformance.condition.common;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class EnsureIncomingTls12WithSecureCipherOrTls13 extends AbstractCondition {

	// as per SSL_PROTOCOL in https://httpd.apache.org/docs/current/mod/mod_ssl.html
	private static final String TLS_12 = "TLSv1.2";
	private static final String TLS_13 = "TLSv1.3";

	@Override
	@PreEnvironment(required = "client_request")
	public Environment evaluate(Environment env) {

		String protocol = env.getString("client_request", "headers.x-ssl-protocol");

		if (Strings.isNullOrEmpty(protocol)) {
			throw error("TLS Protocol not found; this header should have been set by the apache proxy");
		}

		if (protocol.equals(TLS_12)) {
			String cipher = env.getString("client_request", "headers.x-ssl-cipher");
			List<String> recommended = getRecomendedCiphers();

			if (!recommended.contains(cipher)) {
				// "actual" here uses the openssl names instead of the standard iana ones, which is annoying but
				// apache can only give us the openssl ones: https://httpd.apache.org/docs/current/mod/mod_ssl.html
				// and there doesn't seem to be an easy way to map back: https://stackoverflow.com/questions/63491644/openssl-1-1-get-a-cipher-suite-by-the-iana-id
				// https://testssl.sh/openssl-iana.mapping.html
				// openssl ciphers -V outputs the hex codes and openssl names, in theory we could get the hex code then lookup in org.bouncycastle.crypto.tls.CipherSuite to convert to the standard name
				throw error("TLS 1.2 in use and cipher is not one recommended by BCP195", args("expected", recommended, "actual", cipher));
			}
			logSuccess("TLS 1.2 in use and cipher is one recommended by BCP195", args("recommended", recommended, "actual", cipher));
			return env;
		} else if (protocol.equals(TLS_13)) {
			logSuccess("Found TLS 1.3 connection");
			return env;
		} else {
			throw error("TLS version is neither 1.2 nor 1.3", args( "actual", protocol));
		}

	}

	protected List<String> getRecomendedCiphers() {
		// List of recommended cyphers from BCP195; note that apache uses the OpenSSL cipher name
		// unlike the constants found in the CipherSuite enum used by DisallowInsecureCipher which
		// uses the IANA name.
		//
		// See https://ciphersuite.info/cs/ for mappings.
		return ImmutableList.of(
			"DHE-RSA-AES128-GCM-SHA256",
			"ECDHE-RSA-AES128-GCM-SHA256",
			"DHE-RSA-AES256-GCM-SHA384",
			"ECDHE-RSA-AES256-GCM-SHA384");

	}

}
