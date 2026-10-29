package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.TestKeysAndCerts;

/**
 * Fills the second client's 'jwks' in the test configuration with the suite's second built-in
 * wallet-relying-party access certificate and key ({@link TestKeysAndCerts#getSecondWrpac()}) when the
 * tester supplied none, so that a multi-signed OpenID4VP request with the x509_hash client identifier
 * prefix has a second signer the wallet can chain to the suite's published access CA. That is the CA
 * of the first signer too when it is the built-in access certificate; a tester-supplied first signer
 * keeps its own CA, which OpenID4VP Appendix A.3.2.2 allows, as each signature may belong to a
 * different trust framework. The certificate has its own key and subject, so its x509_hash client
 * identifier differs from the built-in first signer's. A configured 'jwks' is left alone. Runs before
 * the second client's keys are read into the environment. The built-in certificate is re-minted daily,
 * so the second x509_hash client identifier changes daily too.
 */
public class AddBuiltInSecondAccessCertificateToClient2Configuration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {
		JsonElement client2El = env.getElementFromObject("config", "client2");
		JsonObject client2;
		if (client2El == null || client2El.isJsonNull()) {
			client2 = new JsonObject();
			env.getObject("config").add("client2", client2);
		} else if (client2El.isJsonObject()) {
			client2 = client2El.getAsJsonObject();
		} else {
			throw error("The 'Second client' section of the test configuration is not a JSON object",
				args("client2", client2El));
		}

		// the same rule WarnIfBuiltInSecondAccessCertificateIsEphemeral applies, so the warning and the fill agree
		if (AbstractWarnIfBuiltInCertificateAuthorityIsEphemeral.isConfigured(env, "client2.jwks")) {
			JsonElement jwks = client2.get("jwks");
			log("The 'jwks' field in the 'Second client' section of the test configuration is set; the suite makes the"
					+ " second signature of multi-signed requests with it",
				args("client2_jwks", jwks));
			return env;
		}

		TestKeysAndCerts.MintedLeaf secondWrpac = TestKeysAndCerts.getSecondWrpac();
		client2.add("jwks", secondWrpac.jwksJson());
		logSuccess("No 'jwks' field in the 'Second client' section of the test configuration; the suite makes the second"
				+ " signature of multi-signed requests with its second built-in wallet-relying-party access certificate, issued"
				+ " by the access CA served at " + TestKeysAndCerts.SuiteCa.ACCESS.getTrustAnchorUrl() + ". The certificate is"
				+ " re-minted daily, so the second x509_hash client identifier derived from it changes daily; configure a"
				+ " 'jwks' in the 'Second client' section if the wallet pins the client identifier.",
			args("access_certificate_subject", secondWrpac.getCert().getSubject().getName(),
				"access_ca_url", TestKeysAndCerts.SuiteCa.ACCESS.getTrustAnchorUrl()));
		return env;
	}
}
