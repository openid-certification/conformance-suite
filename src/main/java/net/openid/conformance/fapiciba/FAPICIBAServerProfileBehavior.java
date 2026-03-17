package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.sequence.ConditionSequence;
import java.util.function.Supplier;

public class FAPICIBAServerProfileBehavior {

	protected AbstractFAPICIBAID1 module;

	public void setModule(AbstractFAPICIBAID1 module) {
		this.module = module;
	}

	public Environment getEnv() {
		return module.getEnv();
	}

	public Class<? extends ConditionSequence> getResourceConfiguration() {
		return AbstractFAPICIBAID1.FAPIResourceConfiguration.class;
	}

	public Class<? extends ConditionSequence> getAdditionalClientRegistrationSteps() {
		return null;
	}

	public Supplier<? extends ConditionSequence> getPreAuthorizationSteps() {
		return null;
	}

	public Class<? extends ConditionSequence> getProfileAuthorizationEndpointSetupSteps() {
		return null;
	}

	public Class<? extends ConditionSequence> getProfileIdTokenValidationSteps() {
		return AbstractFAPICIBAID1.PlainFapiProfileIdTokenValidationSteps.class;
	}

	public void applyProfileSpecificServerConfigChecks() {
		// No-op by default
	}

	public void applyProfileSpecificResourceEndpointSetup() {
		// No-op by default
	}

	public void applyProfileSpecificResourceEndpointRetry(boolean isSecondClient, Class<? extends ConditionSequence> addTokenEndpointClientAuthentication) {
		// No-op by default
	}

	public void validateProfileSpecificResourceEndpointResponse() {
		// No-op by default
	}

	public void validateProfileSpecificTokenEndpointExpiresIn() {
		// No-op by default
	}
}