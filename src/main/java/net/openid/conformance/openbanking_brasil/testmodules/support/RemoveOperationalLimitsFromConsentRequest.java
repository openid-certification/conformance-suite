package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class RemoveOperationalLimitsFromConsentRequest extends AbstractCondition {
	@Override
	public Environment evaluate(Environment env) {
		env.putBoolean("operational_limit_consent", false);
		return env;
	}
}
