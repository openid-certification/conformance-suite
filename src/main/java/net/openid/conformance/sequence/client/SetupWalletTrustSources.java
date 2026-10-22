package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.EnsureClientAttestationTrustAnchorConfigured;
import net.openid.conformance.condition.client.lote.LoteSlot;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.vci10wallet.condition.clientattestation.VCIRegisterClientAttestationTrustAnchor;
import net.openid.conformance.vci10wallet.condition.clientattestation.VCIRegisterKeyAttestationTrustAnchor;

/**
 * Registers the trust sources of the wallet's client attestation and key attestation signers: the
 * configured trust anchor PEMs, or the wallet providers list of trusted entities, which is set up
 * here. The client attestation's trust source is required only when client attestation is the
 * client authentication method.
 */
public class SetupWalletTrustSources extends AbstractConditionSequence {

	private final Environment env;
	private final boolean clientAttestation;

	/** @param env the environment holding the test configuration at the time of the call, which says whether the list is configured */
	public SetupWalletTrustSources(Environment env, boolean clientAttestation) {
		this.env = env;
		this.clientAttestation = clientAttestation;
	}

	@Override
	public void evaluate() {
		if (clientAttestation) {
			// the attestation must verify under a known and trusted attester, so a trust source is required
			callAndStopOnFailure(EnsureClientAttestationTrustAnchorConfigured.class, "OAuth2-ATCA07-9-5");
			callAndStopOnFailure(VCIRegisterClientAttestationTrustAnchor.class, "OAuth2-ATCA07-9-5");
		}
		callAndStopOnFailure(VCIRegisterKeyAttestationTrustAnchor.class);
		call(SetupLoteFromConfiguration.forConfigured(env, ConditionResult.FAILURE, LoteSlot.WALLET));
	}
}
