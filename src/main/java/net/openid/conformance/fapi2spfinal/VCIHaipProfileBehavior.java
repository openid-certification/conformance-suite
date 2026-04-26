package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.CheckDiscEndpointTokenEndpointAuthMethodsSupportedContainsAttestation;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.ConditionCallBuilder;
import net.openid.conformance.variant.VCI1FinalCredentialFormat;
import net.openid.conformance.vci10issuer.condition.VCIDetermineCredentialConfigurationTransferMethod;
import net.openid.conformance.vci10issuer.condition.VCIEnsureScopePresentInCredentialConfigurationForHaip;
import net.openid.conformance.vci10issuer.condition.VCIValidateNonceEndpointInIssuerMetadata;

/**
 * Profile behavior for VCI HAIP (High Assurance Interoperability Profile) tests.
 *
 * <p>Extends {@link VCIProfileBehavior} with HAIP-specific constraints for:
 * attestation-based client authentication discovery checks, scope-based credential
 * configuration resolution, required nonce endpoint metadata, and HAIP-specific
 * credential validation rules.
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

	@Override
	public ConditionSequence fetchServerConfiguration(boolean isOpenId) {
		return super.fetchServerConfiguration(isOpenId)
			.then(new AbstractConditionSequence() {
				@Override
				public void evaluate() {
					call(new ConditionCallBuilder(VCIValidateNonceEndpointInIssuerMetadata.class)
						.onFail(ConditionResult.FAILURE)
						.requirements("HAIP-4.1-5"));
				}
			});
	}

	@Override
	protected boolean isHaip() {
		return true;
	}
}
