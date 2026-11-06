package net.openid.conformance.poppy.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.runner.TestDispatcher;
import net.openid.conformance.testmodule.Environment;

public class EnsurePoppyRootWellKnownAlias extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {
		String alias = env.getString("config", "alias");
		if (!TestDispatcher.ROOT_WELL_KNOWN_ALIAS.equals(alias)) {
			throw error("The alias in the test configuration must be '" + TestDispatcher.ROOT_WELL_KNOWN_ALIAS
					+ "' so that the suite can serve /.well-known/poppy.json and /.well-known/oauth-protected-resource on the Company's domain",
				args("alias", alias, "required_alias", TestDispatcher.ROOT_WELL_KNOWN_ALIAS));
		}
		logSuccess("The test alias is the root well-known alias", args("alias", alias));
		return env;
	}
}
