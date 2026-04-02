package net.openid.conformance.fapiciba;

import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.OpenBankingUkPreAuthorizationSteps;

import java.util.function.Supplier;

public class OpenBankingUkCibaServerProfileBehavior extends FAPICIBAServerProfileBehavior {

	@Override
	public Class<? extends ConditionSequence> getResourceConfiguration() {
		return AbstractFAPICIBAID1.OpenBankingUkResourceConfiguration.class;
	}

	@Override
	public Class<? extends ConditionSequence> getAdditionalClientRegistrationSteps() {
		return AbstractFAPICIBAID1.OpenBankingUkClientRegistrationSteps.class;
	}

	@Override
	public Supplier<? extends ConditionSequence> getPreAuthorizationSteps() {
		return () -> new OpenBankingUkPreAuthorizationSteps(module.isSecondClient(), module.addTokenEndpointClientAuthentication);
	}

	@Override
	public Class<? extends ConditionSequence> getProfileAuthorizationEndpointSetupSteps() {
		return AbstractFAPICIBAID1.OpenBankingUkProfileAuthorizationEndpointSetupSteps.class;
	}

	@Override
	public Class<? extends ConditionSequence> getProfileIdTokenValidationSteps() {
		return AbstractFAPICIBAID1.OpenBankingUkProfileIdTokenValidationSteps.class;
	}
}
