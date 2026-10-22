package net.openid.conformance.condition.client;

import net.openid.conformance.condition.client.lote.AbstractEnsureTrustSourceConfigured;
import net.openid.conformance.condition.client.lote.TrustPurpose;

public class EnsureCredentialTrustAnchorConfigured extends AbstractEnsureTrustSourceConfigured {

	@Override
	protected TrustPurpose purpose() {
		return TrustPurpose.CREDENTIAL;
	}

	@Override
	protected String requiredBecause() {
		return "HAIP requires a trust anchor for the credential signer, so one of them is required.";
	}
}
