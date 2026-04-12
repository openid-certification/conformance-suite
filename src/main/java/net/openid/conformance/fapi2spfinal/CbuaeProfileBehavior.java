package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.CheckDiscEndpointAuthorizationRequestTypesSupportedContainsTestType;
import net.openid.conformance.condition.client.CheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode;
import net.openid.conformance.condition.client.CheckDiscEndpointRequestObjectSigningAlgValuesSupportedContainsPS256;
import net.openid.conformance.condition.common.RARSupport;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;

import java.util.function.Supplier;

/**
 * Profile behavior for CBUAE (Central Bank of UAE).
 * Only differs from plain FAPI in requiring mTLS everywhere.
 */
public class CbuaeProfileBehavior extends FAPI2ProfileBehavior {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}

	@Override
	public Supplier<? extends ConditionSequence> getProfileSpecificDiscoveryChecks() {
		return DiscoveryEndpointChecks::new;
	}

	public static class DiscoveryEndpointChecks extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndContinueOnFailure(CheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode.class, ConditionResult.FAILURE);
			callAndContinueOnFailure(CheckDiscEndpointRequestObjectSigningAlgValuesSupportedContainsPS256.class, ConditionResult.FAILURE);
			callAndStopOnFailure(RARSupport.ExtractRARFromConfig.class, ConditionResult.FAILURE);
			callAndContinueOnFailure(CheckDiscEndpointAuthorizationRequestTypesSupportedContainsTestType.class, ConditionResult.WARNING);
		}
	}
}
