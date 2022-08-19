package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class ForceToValidateConsentErrorAndMetaFieldNames extends AbstractCondition {
	@Override
	public Environment evaluate(Environment env) {
		env.putBoolean("force_consents_response", true);
		log("Forced to validate consent endpoint response");
		return env;
	}
}
