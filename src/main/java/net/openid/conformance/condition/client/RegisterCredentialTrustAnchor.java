package net.openid.conformance.condition.client;

import net.openid.conformance.condition.client.lote.AbstractRegisterTrustSource;
import net.openid.conformance.condition.client.lote.TrustPurpose;

public class RegisterCredentialTrustAnchor extends AbstractRegisterTrustSource {

	/** Set when a credential trust anchor PEM or a PID or mDL providers LoTE URL is configured. */
	public static final String ENV_TRUST_SOURCE_CONFIGURED = "credential_trust_source_configured";

	@Override
	protected TrustPurpose purpose() {
		return TrustPurpose.CREDENTIAL;
	}

	@Override
	protected String trustSourceConfiguredKey() {
		return ENV_TRUST_SOURCE_CONFIGURED;
	}
}
