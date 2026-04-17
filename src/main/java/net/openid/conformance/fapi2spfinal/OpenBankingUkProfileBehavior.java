package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.CheckDiscEndpointClaimsParameterSupported;
import net.openid.conformance.condition.client.CheckJwksUriIsHostedOnOpenBankingDirectory;
import net.openid.conformance.condition.client.FAPIOBCheckDiscEndpointClaimsSupported;
import net.openid.conformance.condition.client.FAPIOBCheckDiscEndpointGrantTypesSupported;
import net.openid.conformance.condition.client.FAPIOBCheckDiscEndpointScopesSupported;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.OpenBankingUkAuthorizationEndpointSetup;
import net.openid.conformance.sequence.client.OpenBankingUkPreAuthorizationSteps;
import net.openid.conformance.sequence.client.ValidateOpenBankingUkIdToken;

import java.util.function.Supplier;

/**
 * Profile behavior for OpenBanking UK.
 * Requires mTLS everywhere, uses accounts endpoint as resource, has pre-authorization steps
 * for account requests, UK-specific authorization endpoint setup, and UK id_token validation.
 */
public class OpenBankingUkProfileBehavior extends FAPI2ProfileBehavior {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}

	@Override
	public Class<? extends ConditionSequence> getResourceConfiguration() {
		return AbstractFAPI2SPFinalServerTestModule.OpenBankingUkResourceConfiguration.class;
	}

	@Override
	public Supplier<? extends ConditionSequence> getPreAuthorizationSteps() {
		return () -> new OpenBankingUkPreAuthorizationSteps(
			module.isSecondClient(),
			false,
			module.addClientAuthentication);
	}

	@Override
	public Class<? extends ConditionSequence> getProfileAuthorizationEndpointSetupSteps() {
		return OpenBankingUkAuthorizationEndpointSetup.class;
	}

	@Override
	public Class<? extends ConditionSequence> getProfileIdTokenValidationSteps() {
		return ValidateOpenBankingUkIdToken.class;
	}

	@Override
	public Supplier<? extends ConditionSequence> getProfileSpecificDiscoveryChecks() {
		return DiscoveryEndpointChecks::new;
	}

	public static class DiscoveryEndpointChecks extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			// OBUK servers are required to return acrs, which means they must support requesting the acr claim (as well
			// as the intent id claim), and hence must support the claims parameter
			// FIXME No obvious FAPI2 equivalent.
			callAndContinueOnFailure(CheckDiscEndpointClaimsParameterSupported.class, ConditionResult.FAILURE, "OIDCD-3");
			callAndContinueOnFailure(CheckJwksUriIsHostedOnOpenBankingDirectory.class, ConditionResult.WARNING, "OBSP-3.4");
			callAndContinueOnFailure(FAPIOBCheckDiscEndpointClaimsSupported.class, ConditionResult.FAILURE, "OBSP-3.4");
			callAndContinueOnFailure(FAPIOBCheckDiscEndpointGrantTypesSupported.class, ConditionResult.FAILURE);
			callAndContinueOnFailure(FAPIOBCheckDiscEndpointScopesSupported.class, ConditionResult.FAILURE);
		}
	}
}
