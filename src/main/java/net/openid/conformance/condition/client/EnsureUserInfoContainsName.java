package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureUserInfoContainsName extends AbstractCondition {

	@Override
	@PreEnvironment(required = "userinfo")
	public Environment evaluate(Environment env)
	{
		String name = env.getString("userinfo", "name");
		if (Strings.isNullOrEmpty(name)) {
			throw error("name not found in userinfo");
		}

		logSuccess("Found name in userinfo", args("name", name));
		return env;
	}

}
