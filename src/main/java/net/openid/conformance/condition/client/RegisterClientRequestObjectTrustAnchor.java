package net.openid.conformance.condition.client;

import net.openid.conformance.condition.client.lote.AbstractRegisterTrustSource;
import net.openid.conformance.condition.client.lote.TrustPurpose;

public class RegisterClientRequestObjectTrustAnchor extends AbstractRegisterTrustSource {

	@Override
	protected TrustPurpose purpose() {
		return TrustPurpose.REQUEST_OBJECT;
	}
}
