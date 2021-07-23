package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetUpNonExistentPermissions extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "consent_permissions")
	public Environment evaluate(Environment env) {
		String[] permissions = {"BAD_PERMISSION"};
		env.putString("consent_permissions", String.join(" ", permissions));
		logSuccess("Set incomplete permissions which do not exist", args("permissions", permissions));
		return env;
	}

}
