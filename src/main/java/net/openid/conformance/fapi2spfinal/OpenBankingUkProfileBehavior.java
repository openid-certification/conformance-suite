package net.openid.conformance.fapi2spfinal;

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
			module.addTokenEndpointClientAuthentication);
	}

	@Override
	public Class<? extends ConditionSequence> getProfileAuthorizationEndpointSetupSteps() {
		return OpenBankingUkAuthorizationEndpointSetup.class;
	}

	@Override
	public Class<? extends ConditionSequence> getProfileIdTokenValidationSteps() {
		return ValidateOpenBankingUkIdToken.class;
	}
}
