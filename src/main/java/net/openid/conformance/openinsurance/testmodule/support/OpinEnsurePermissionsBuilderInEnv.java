package net.openid.conformance.openinsurance.testmodule.support;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class OpinEnsurePermissionsBuilderInEnv extends AbstractCondition {

	@PreEnvironment(strings = {"permissions_builder"})
	@Override
	public Environment evaluate(Environment env) {

		String permissionsBuilder = env.getString("permissions_builder");

		if(Strings.isNullOrEmpty(permissionsBuilder)) {
			throw  error("No permissions_builder string find in the environment.");
		}

		logSuccess(String.format("permissions_builder was found: %s.", permissionsBuilder));
		return env;
	}
}
