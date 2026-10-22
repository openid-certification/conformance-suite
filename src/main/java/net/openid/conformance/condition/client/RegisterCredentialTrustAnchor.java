package net.openid.conformance.condition.client;

import net.openid.conformance.condition.client.lote.AbstractRegisterTrustSource;
import net.openid.conformance.condition.client.lote.TrustPurpose;
import net.openid.conformance.testmodule.Environment;

public class RegisterCredentialTrustAnchor extends AbstractRegisterTrustSource {

	/** Set when a credential trust anchor PEM or a PID or mDL providers LoTE URL is configured. */
	public static final String ENV_TRUST_SOURCE_CONFIGURED = "credential_trust_source_configured";

	@Override
	protected TrustPurpose purpose() {
		return TrustPurpose.CREDENTIAL;
	}

	@Override
	public Environment evaluate(Environment env) {
		super.evaluate(env);
		// the chain checks that are optional outside HAIP run when any trust source is configured:
		// the PEM, or a list of trusted entities whose anchors SetupLoteFromConfiguration extracts
		if (!purpose().configuredSources(env).isEmpty()) {
			env.putString(ENV_TRUST_SOURCE_CONFIGURED, "true");
		}
		return env;
	}
}
