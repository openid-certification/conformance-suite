package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureIdTokenContainsName extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token" )
	public Environment evaluate(Environment env) {
		String name = env.getString("id_token", "claims.name");

		if (Strings.isNullOrEmpty(name)) {
			throw error("name not found in id_token");
		}

		logSuccess("Found name in id_token", args("name", name));
		return env;
	}

}
