package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckDiscoveryEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.EnsureDiscoveryEndpointResponseStatusCodeIs200;
import net.openid.conformance.condition.client.GetDynamicServerConfiguration;
import net.openid.conformance.condition.client.GetOauthDynamicServerConfiguration;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;

/**
 * Profile-specific behavior for discovery endpoint verification.
 * Default (plain FAPI) fetches standard OIDC/OAuth server configuration.
 * Subclasses override to customize behavior for specific profiles (e.g., VCI).
 */
public class FAPI2DiscoveryProfileBehavior {

	/**
	 * Fetch and store server configuration.
	 * Default fetches OIDC or OAuth server configuration and validates the response.
	 */
	public ConditionSequence fetchServerConfiguration(boolean isOpenId) {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				String specRequirements = "OIDCD-4";
				if (isOpenId) {
					callAndStopOnFailure(GetDynamicServerConfiguration.class);
				} else {
					callAndStopOnFailure(GetOauthDynamicServerConfiguration.class);
					specRequirements = "RFC8414-3.2";
				}
				callAndContinueOnFailure(EnsureDiscoveryEndpointResponseStatusCodeIs200.class, Condition.ConditionResult.FAILURE, specRequirements);
				callAndContinueOnFailure(CheckDiscoveryEndpointReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, specRequirements);
			}
		};
	}

	/**
	 * Called after server configuration is fetched.
	 * Default does nothing. VCI overrides to set discoveryUrl from the authorization server issuer.
	 */
	public ConditionSequence afterServerConfigurationFetched() {
		return null;
	}
}
