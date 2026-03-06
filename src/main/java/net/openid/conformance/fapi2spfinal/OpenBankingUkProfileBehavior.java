package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.fapi2spfinal.AbstractFAPI2SPFinalServerTestModule.OpenBankingUkResourceConfiguration;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.OpenBankingUkAuthorizationEndpointSetup;
import net.openid.conformance.sequence.client.OpenBankingUkPreAuthorizationSteps;
import net.openid.conformance.sequence.client.ValidateOpenBankingUkIdToken;

import java.util.function.Supplier;

/**
 * OpenBanking UK profile behavior.
 */
public class OpenBankingUkProfileBehavior extends FAPI2ProfileBehavior {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}

	@Override
	public Class<? extends ConditionSequence> resourceConfiguration() {
		return OpenBankingUkResourceConfiguration.class;
	}

	@Override
	public Supplier<? extends ConditionSequence> preAuthorizationSteps(
		AbstractFAPI2SPFinalServerTestModule module) {
		return () -> new OpenBankingUkPreAuthorizationSteps(
			module.isSecondClient(), false, module.getAddTokenEndpointClientAuthentication());
	}

	@Override
	public Class<? extends ConditionSequence> profileAuthorizationEndpointSetupSteps() {
		return OpenBankingUkAuthorizationEndpointSetup.class;
	}

	@Override
	public Class<? extends ConditionSequence> profileIdTokenValidationSteps() {
		return ValidateOpenBankingUkIdToken.class;
	}
}
