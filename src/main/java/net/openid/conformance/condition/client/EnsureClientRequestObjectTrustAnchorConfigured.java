package net.openid.conformance.condition.client;

import net.openid.conformance.condition.client.lote.AbstractEnsureTrustSourceConfigured;
import net.openid.conformance.condition.client.lote.TrustPurpose;

public class EnsureClientRequestObjectTrustAnchorConfigured extends AbstractEnsureTrustSourceConfigured {

	@Override
	protected TrustPurpose purpose() {
		return TrustPurpose.REQUEST_OBJECT;
	}

	@Override
	protected String requiredBecause() {
		return "A trust source for the request object signing certificate is required for HAIP.";
	}
}
