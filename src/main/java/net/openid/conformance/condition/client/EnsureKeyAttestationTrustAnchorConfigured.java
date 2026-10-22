package net.openid.conformance.condition.client;

import net.openid.conformance.condition.client.lote.AbstractEnsureTrustSourceConfigured;
import net.openid.conformance.condition.client.lote.TrustPurpose;

public class EnsureKeyAttestationTrustAnchorConfigured extends AbstractEnsureTrustSourceConfigured {

	@Override
	protected TrustPurpose purpose() {
		return TrustPurpose.KEY_ATTESTATION;
	}

	@Override
	protected String requiredBecause() {
		return "HAIP requires a trust anchor for the key attestation signer, so one of them is required.";
	}
}
