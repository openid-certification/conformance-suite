package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.TestKeysAndCerts;

/**
 * Fills the client's 'jwks' in the test configuration with the suite's built-in wallet-relying-party
 * access certificate and key ({@link TestKeysAndCerts#getWrpac()}) when the tester supplied none, so
 * that OpenID4VP wallet tests with an x509 client identifier prefix sign their requests with a
 * certificate the wallet can chain to a published trust anchor. A configured 'jwks' is left alone.
 * Runs before the client configuration is read into the environment, so every later consumer sees
 * the same key. The certificate's dNSName is the host of the module's base URL, the value the
 * x509_san_dns client identifier takes.
 */
public class AddBuiltInAccessCertificateToClientConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {
		JsonElement clientEl = env.getElementFromObject("config", "client");
		JsonObject client;
		if (clientEl == null || !clientEl.isJsonObject()) {
			client = new JsonObject();
			env.getObject("config").add("client", client);
		} else {
			client = clientEl.getAsJsonObject();
		}

		JsonElement jwks = client.get("jwks");
		if (jwks != null && !jwks.isJsonNull()) {
			log("The 'jwks' field in the 'Client' section of the test configuration is set; the suite signs requests with it",
				args("client_jwks", jwks));
			return env;
		}

		// the x509_san_dns client identifier is the host the wallet reaches this test module at, which an
		// external URL override may make differ from the deployment's base URL: mint the certificate for it
		String externalUrl = env.getString("external_url_override");
		String host = hostOf(externalUrl == null || externalUrl.isBlank() ? env.getString("base_url") : externalUrl);
		TestKeysAndCerts.MintedLeaf wrpac = host == null || host.equals(TestKeysAndCerts.getWrpacDnsName())
			? TestKeysAndCerts.getWrpac() : TestKeysAndCerts.mintWrpac(host);
		client.add("jwks", wrpac.jwksJson());
		logSuccess("No 'jwks' field in the 'Client' section of the test configuration; the suite signs requests with its"
				+ " built-in wallet-relying-party access certificate, issued by the access CA served at "
				+ TestKeysAndCerts.SuiteCa.ACCESS.getTrustAnchorUrl(),
			args("access_certificate_subject", wrpac.getCert().getSubject().getName(),
				"access_ca_url", TestKeysAndCerts.SuiteCa.ACCESS.getTrustAnchorUrl()));
		return env;
	}

	private static String hostOf(String url) {
		if (url == null) {
			return null;
		}
		try {
			return new java.net.URI(url).getHost();
		} catch (java.net.URISyntaxException e) {
			return null;
		}
	}
}
