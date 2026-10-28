package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.X509CertUtils;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.TestKeysAndCerts;

import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.List;

/**
 * Makes every certificate the request is signed with untrusted, keeping everything else about it: the
 * signing key of the 'jwks' field in the 'Client' section of the test configuration, and of the 'jwks'
 * field in the 'Second client' section when present (the second signature of a multi-signed request), is
 * replaced by a key whose certificate is a twin of the configured one (same subject and extensions, see
 * {@link TestKeysAndCerts#mintUntrustedTwin}) issued by a CA that no trust anchor or list of trusted
 * entities names. Without a client 'jwks' the client signs with {@link TestKeysAndCerts#getUntrustedWrpac()},
 * the twin of the suite's built-in access certificate. Other keys in the JWK Sets, such as an encryption
 * key, are kept. Runs before the client configuration is read into the environment, so x509_hash client
 * identifiers are derived from the twins.
 */
public class AddUntrustedAccessCertificateToClientConfiguration extends AbstractGetSigningKey {

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
		if (jwks == null || jwks.isJsonNull()) {
			TestKeysAndCerts.MintedLeaf untrusted = TestKeysAndCerts.getUntrustedWrpac();
			client.add("jwks", untrusted.jwksJson());
			logSuccess("No 'jwks' field in the 'Client' section of the test configuration; the suite signs requests with a"
					+ " certificate that has the subject, subject alternative name and extensions of its built-in access"
					+ " certificate but chains to a CA that is minted at runtime and that no published trust anchor or list of"
					+ " trusted entities contains. The wallet must reject the request.",
				args("access_certificate_subject", untrusted.getCert().getSubject().getName(),
					"untrusted_ca_subject", TestKeysAndCerts.getUntrustedAccessCa().getCert().getSubject().getName(),
					"untrusted_ca_certificate", TestKeysAndCerts.getUntrustedAccessCa().getCertPem()));
		} else {
			replaceSigningKeyWithUntrustedTwin(jwks, "'jwks' field in the 'Client' section");
		}

		JsonElement client2Jwks = env.getElementFromObject("config", "client2.jwks");
		if (client2Jwks != null && !client2Jwks.isJsonNull()) {
			replaceSigningKeyWithUntrustedTwin(client2Jwks, "'jwks' field in the 'Second client' section");
		}
		return env;
	}

	private void replaceSigningKeyWithUntrustedTwin(JsonElement jwks, String fieldDescription) {
		if (!jwks.isJsonObject() || !jwks.getAsJsonObject().has("keys") || !jwks.getAsJsonObject().get("keys").isJsonArray()) {
			throw error("The " + fieldDescription + " of the test configuration is not a JSON Web Key Set", args("jwks", jwks));
		}
		JsonArray keys = jwks.getAsJsonObject().getAsJsonArray("keys");
		JWK signingJwk = getSigningKey(fieldDescription, jwks.getAsJsonObject());

		List<Base64> x5c = signingJwk.getX509CertChain();
		if (x5c == null || x5c.isEmpty()) {
			throw error("The signing key in the " + fieldDescription + " of the test configuration has no 'x5c', so the"
				+ " suite has no certificate to make an untrusted copy of", args("jwks", jwks));
		}
		X509Certificate original = X509CertUtils.parse(x5c.get(0).decode());
		if (original == null) {
			throw error("The first 'x5c' entry of the signing key in the " + fieldDescription + " of the test configuration"
				+ " is not an X.509 certificate", args("x5c", x5c.get(0).toString()));
		}

		TestKeysAndCerts.MintedLeaf twin = TestKeysAndCerts.mintUntrustedTwin(original);
		ECKey.Builder twinJwk = new ECKey.Builder(twin.getJwk()).keyUse(signingJwk.getKeyUse());
		if (signingJwk.getKeyID() != null) {
			twinJwk.keyID(signingJwk.getKeyID());
		}
		JsonObject twinJson = JsonParser.parseString(twinJwk.build().toJSONString()).getAsJsonObject();

		for (int i = 0; i < keys.size(); i++) {
			if (isSameKey(keys.get(i), signingJwk)) {
				keys.set(i, twinJson);
				break;
			}
		}

		logSuccess("Replaced the signing key in the " + fieldDescription + " of the test configuration with one whose"
				+ " certificate has the same subject and extensions as the configured certificate but chains to a CA that is"
				+ " minted at runtime and that no published trust anchor or list of trusted entities contains. The wallet"
				+ " must reject the request.",
			args("configured_certificate_subject", original.getSubjectX500Principal().getName(),
				"configured_certificate_issuer", original.getIssuerX500Principal().getName(),
				"untrusted_certificate", twin.getJwk().getX509CertChain().get(0).toString(),
				"untrusted_ca_subject", TestKeysAndCerts.getUntrustedAccessCa().getCert().getSubject().getName(),
				"untrusted_ca_certificate", TestKeysAndCerts.getUntrustedAccessCa().getCertPem()));
	}

	private static boolean isSameKey(JsonElement key, JWK signingJwk) {
		try {
			return JWK.parse(key.toString()).equals(signingJwk);
		} catch (ParseException e) {
			return false;
		}
	}
}
