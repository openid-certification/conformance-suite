package net.openid.conformance.condition.client.lote;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.security.cert.X509Certificate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pre-flight check for the OpenID4VP wallet tests: the request signing certificate the suite
 * will present (the x5c of the client's jwks, configured or built-in, and of the second client's
 * when present) must chain to an access certificate provider in the configured WRPAC providers
 * list. Expected to be called as a WARNING.
 */
public class ValidateOwnRequestSigningCertificateAgainstWrpacLote extends AbstractValidateOwnCertificatesAgainstLote {

	@Override
	@PreEnvironment(required = "client_jwks")
	public Environment evaluate(Environment env) {
		if (noListConfigured(env, "request signing", LoteSlot.WRPAC)) {
			return env;
		}
		Map<String, List<X509Certificate>> chains = new LinkedHashMap<>();
		chains.put("'jwks' field in the 'Client' section", chainOfConfiguredJwks(env.getObject("client_jwks"), "'jwks' field in the 'Client' section"));
		if (env.containsObject("client2_jwks")) {
			chains.put("'jwks' field in the 'Second client' section",
				chainOfConfiguredJwks(env.getObject("client2_jwks"), "'jwks' field in the 'Second client' section"));
		}
		return check(env, chains, "request signing", LoteSlot.WRPAC);
	}
}
