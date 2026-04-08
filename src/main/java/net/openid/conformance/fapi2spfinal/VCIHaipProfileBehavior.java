package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.CheckDiscEndpointTokenEndpointAuthMethodsSupportedContainsAttestation;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.ConditionCallBuilder;
import net.openid.conformance.variant.VCI1FinalCredentialFormat;
import net.openid.conformance.vci10issuer.condition.VCIDetermineCredentialConfigurationTransferMethod;
import net.openid.conformance.vci10issuer.condition.VCIEnsureScopePresentInCredentialConfigurationForHaip;

/**
 * Profile behavior for VCI HAIP (High Assurance Interoperability Profile) tests.
 *
 * <p>Extends {@link VCIProfileBehavior} with HAIP-specific constraints.
 * HAIP requires attestation-based client authentication only.
 * Additional HAIP-specific constraints (DPoP ES256, nonce endpoint, credential validity,
 * x5c headers) are currently handled via {@code fapi2Profile == VCI_HAIP} checks in
 * {@link net.openid.conformance.vci10issuer.AbstractVCIIssuerTestModule}. Override methods
 * here if future HAIP behavior belongs in the profile behavior layer.
 */
public class VCIHaipProfileBehavior extends VCIProfileBehavior {

	@Override
	public Class<? extends AbstractCondition> getDiscoveryTokenEndpointAuthMethodsCheck() {
		return CheckDiscEndpointTokenEndpointAuthMethodsSupportedContainsAttestation.class;
	}

	@Override
	protected ConditionSequence configureCredentialConfigurationResolution(VCI1FinalCredentialFormat vciCredentialFormat) {
		ConditionSequence seq = super.configureCredentialConfigurationResolution(vciCredentialFormat);
		// HAIP requires scope to be present for every credential configuration
		seq.insertBefore(VCIDetermineCredentialConfigurationTransferMethod.class,
			new ConditionCallBuilder(VCIEnsureScopePresentInCredentialConfigurationForHaip.class)
				.onFail(ConditionResult.FAILURE)
				.requirements("HAIP-4.1", "HAIP-4.3")
				.dontStopOnFailure());
		return seq;
	}
}
