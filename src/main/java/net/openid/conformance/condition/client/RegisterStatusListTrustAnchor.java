package net.openid.conformance.condition.client;

import net.openid.conformance.condition.client.lote.AbstractRegisterTrustSource;
import net.openid.conformance.condition.client.lote.TrustPurpose;

public class RegisterStatusListTrustAnchor extends AbstractRegisterTrustSource {

	/** Set when a status list trust anchor PEM or a PID or mDL providers LoTE URL is configured. */
	public static final String ENV_TRUST_SOURCE_CONFIGURED = "status_list_trust_source_configured";

	@Override
	protected TrustPurpose purpose() {
		return TrustPurpose.STATUS_LIST;
	}

	@Override
	protected String trustSourceConfiguredKey() {
		return ENV_TRUST_SOURCE_CONFIGURED;
	}
}
