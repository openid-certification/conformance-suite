package net.openid.conformance.vci10wallet.condition.clientattestation;

import net.openid.conformance.condition.client.lote.AbstractRegisterTrustSource;
import net.openid.conformance.condition.client.lote.TrustPurpose;

public class VCIRegisterKeyAttestationTrustAnchor extends AbstractRegisterTrustSource {

	@Override
	protected TrustPurpose purpose() {
		return TrustPurpose.KEY_ATTESTATION;
	}
}
