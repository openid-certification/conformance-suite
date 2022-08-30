package net.openid.conformance.openinsurance.testmodule.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;

public class OpinLogConsentPermissions extends AbstractCondition {

	@PreEnvironment(strings = {"consent_permissions", "consent_permissions_log"})
	@Override
	public Environment evaluate(Environment env) {
		logSuccess(env.getString("consent_permissions_log"), args("permissions", env.getString("consent_permissions").replace(" ", "/")));
		return env;
	}
}
