package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetProtectedResourceUrlToUserInfoEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(strings = "protected_resource_url")
	public Environment evaluate(Environment env) {

		String resourceUrl = env.getString("server", "userinfo_endpoint");

		if(Strings.isNullOrEmpty(resourceUrl)){
			throw error("userinfo missing from server discovery document");
		}

		env.putString("protected_resource_url", resourceUrl);

		logSuccess("Set protected resource URL", args("protected_resource_url", resourceUrl));

		return env;
	}

}
