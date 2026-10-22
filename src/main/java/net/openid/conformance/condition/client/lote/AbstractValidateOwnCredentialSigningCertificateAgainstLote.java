package net.openid.conformance.condition.client.lote;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

/**
 * Pre-flight check for the modules in which the suite issues or presents credentials (the
 * OpenID4VCI wallet tests and the OpenID4VP verifier tests): the suite's signing certificate for
 * the selected credential format must chain to a provider in the configured PID or mDL providers
 * list. A credential chain has one trust source, so configuring both lists is reported too. Runs
 * when either list is configured. Expected to be called as a WARNING.
 */
public abstract class AbstractValidateOwnCredentialSigningCertificateAgainstLote extends AbstractValidateOwnCertificatesAgainstLote {

	/** The suite's signing chain for the format, leaf first, keyed by what it is. */
	protected abstract Map<String, List<X509Certificate>> signingChain(Environment env);

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {
		if (noListConfigured(env, "credential signing", LoteSlot.PID, LoteSlot.MDL)) {
			return env;
		}
		boolean pid = env.containsObject(LoteSlot.PID.envKey);
		boolean mdl = env.containsObject(LoteSlot.MDL.envKey);
		if (pid && mdl) {
			throw error("Both the " + LoteSlot.PID.fieldDescription() + " and the " + LoteSlot.MDL.fieldDescription()
				+ " are set; a credential chain has one trust source, so please clear the one that does not cover the credential under test");
		}
		return check(env, signingChain(env), "credential signing", pid ? LoteSlot.PID : LoteSlot.MDL);
	}
}
