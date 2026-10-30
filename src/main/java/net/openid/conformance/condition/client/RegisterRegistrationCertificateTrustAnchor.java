package net.openid.conformance.condition.client;

import net.openid.conformance.condition.client.lote.AbstractRegisterTrustSource;
import net.openid.conformance.condition.client.lote.TrustPurpose;

public class RegisterRegistrationCertificateTrustAnchor extends AbstractRegisterTrustSource {

	@Override
	protected TrustPurpose purpose() {
		return TrustPurpose.REGISTRATION_CERTIFICATE;
	}
}
