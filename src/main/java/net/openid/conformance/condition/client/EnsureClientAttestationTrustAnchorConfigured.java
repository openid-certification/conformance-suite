package net.openid.conformance.condition.client;

import net.openid.conformance.condition.client.lote.AbstractEnsureTrustSourceConfigured;
import net.openid.conformance.condition.client.lote.TrustPurpose;

/**
 * With client attestation as the client authentication method the suite, as the issuer, must be
 * able to chain the wallet's attestation signer to something: the configured 'Client Attestation
 * Trust Anchor' or the wallet providers listed in the configured LoTE.
 */
public class EnsureClientAttestationTrustAnchorConfigured extends AbstractEnsureTrustSourceConfigured {

	@Override
	protected TrustPurpose purpose() {
		return TrustPurpose.CLIENT_ATTESTATION;
	}

	@Override
	protected String requiredBecause() {
		return "One of them is required when client attestation is the client authentication method.";
	}
}
