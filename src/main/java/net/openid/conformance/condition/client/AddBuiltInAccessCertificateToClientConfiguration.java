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
 * the same key. The built-in certificate is re-minted daily, so an x509_hash client identifier
 * derived from it changes daily too; a wallet that pins the client identifier needs a configured
 * 'jwks'.
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

		TestKeysAndCerts.MintedLeaf wrpac = TestKeysAndCerts.getWrpac();
		client.add("jwks", wrpac.jwksJson());
		JsonElement clientId = client.get("client_id");
		if (clientId != null && !clientId.isJsonNull()) {
			// an x509_san_dns client identifier must be a dNSName of the certificate, which the tester did not choose
			log("The 'Client ID' field in the 'Client' section of the test configuration is set but 'jwks' is not, so the"
					+ " suite signs with its built-in access certificate: with the x509_san_dns client identifier prefix the"
					+ " Client ID must be a dNSName of that certificate. Clear the Client ID to use the certificate's, or"
					+ " configure a 'jwks' whose certificate names your Client ID.",
				args("client_id", clientId, "access_certificate_subject", wrpac.getCert().getSubject().getName()));
		}
		logSuccess("No 'jwks' field in the 'Client' section of the test configuration; the suite signs requests with its"
				+ " built-in wallet-relying-party access certificate, issued by the access CA served at "
				+ TestKeysAndCerts.SuiteCa.ACCESS.getTrustAnchorUrl() + ". The certificate is re-minted daily, so an x509_hash"
				+ " client identifier derived from it changes daily; configure a 'jwks' if the wallet pins the client identifier.",
			args("access_certificate_subject", wrpac.getCert().getSubject().getName(),
				"access_ca_url", TestKeysAndCerts.SuiteCa.ACCESS.getTrustAnchorUrl()));
		return env;
	}
}
