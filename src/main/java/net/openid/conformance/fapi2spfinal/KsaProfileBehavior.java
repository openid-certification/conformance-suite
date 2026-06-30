package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates;
import net.openid.conformance.condition.client.BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicatesReorderedParams;
import net.openid.conformance.condition.client.CheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode;
import net.openid.conformance.condition.client.CheckDiscEndpointRequestObjectSigningAlgValuesSupportedContainsPS256;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.OpenBankingKSAPreAuthorizationSteps;

import java.util.function.Supplier;

/**
 * Profile behavior for KSA (Saudi Arabia Open Finance).
 * Requires mTLS everywhere. No RAR (unlike CBUAE).
 */
public class KsaProfileBehavior extends FAPI2ProfileBehavior {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}

	@Override
	public Supplier<? extends ConditionSequence> getPreAuthorizationSteps() {
		return () -> new OpenBankingKSAPreAuthorizationSteps(
			module.isSecondClient(),
			false, // includeXFapiFinancialId, as for FAPI2 OpenBanking UK
			module.addClientAuthentication,
			true); // KSA FAPI2 requires the signed-JWT consent format
	}

	@Override
	public Supplier<? extends ConditionSequence> getProfileSpecificDiscoveryChecks() {
		return DiscoveryEndpointChecks::new;
	}

	@Override
	public Class<? extends Condition> getBuildRequestObjectByReferenceRedirectCondition(boolean reorderParameters) {
		// KSA requires the authorization request (after PAR) to carry only client_id + request_uri;
		// the duplicated response_type / scope / redirect_uri parameters must not be present.
		return reorderParameters
			? BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicatesReorderedParams.class
			: BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates.class;
	}

	public static class DiscoveryEndpointChecks extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndContinueOnFailure(CheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode.class, ConditionResult.FAILURE);
			callAndContinueOnFailure(CheckDiscEndpointRequestObjectSigningAlgValuesSupportedContainsPS256.class, ConditionResult.FAILURE);
		}
	}
}
