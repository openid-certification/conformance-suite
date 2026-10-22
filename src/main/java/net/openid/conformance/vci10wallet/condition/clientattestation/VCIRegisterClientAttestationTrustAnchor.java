package net.openid.conformance.vci10wallet.condition.clientattestation;

import net.openid.conformance.condition.client.lote.AbstractRegisterTrustSource;
import net.openid.conformance.condition.client.lote.TrustPurpose;

public class VCIRegisterClientAttestationTrustAnchor extends AbstractRegisterTrustSource {

	@Override
	protected TrustPurpose purpose() {
		return TrustPurpose.CLIENT_ATTESTATION;
	}
}
