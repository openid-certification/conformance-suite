package net.openid.conformance.condition.client;

import net.openid.conformance.condition.client.lote.AbstractEnsureTrustSourceConfigured;
import net.openid.conformance.condition.client.lote.TrustPurpose;

public class EnsureStatusListTrustAnchorConfigured extends AbstractEnsureTrustSourceConfigured {

	@Override
	protected TrustPurpose purpose() {
		return TrustPurpose.STATUS_LIST;
	}

	@Override
	protected String requiredBecause() {
		return "HAIP requires a trust anchor for the status list signer, so one of them is required.";
	}
}
