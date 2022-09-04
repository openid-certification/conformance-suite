package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureUserInfoContainsSub extends AbstractCondition {

	@Override
	@PreEnvironment(required = "userinfo")
	public Environment evaluate(Environment env)
	{
		String sub = env.getString("userinfo", "sub");
		if (Strings.isNullOrEmpty(sub)) {
			throw error("sub not found in userinfo");
		}

		logSuccess("Found sub in userinfo", args("sub", sub));
		return env;
	}

}
