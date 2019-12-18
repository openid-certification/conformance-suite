package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractVerifyUserInfoAndIdTokenSameSub extends AbstractCondition {

	@Override
	@PreEnvironment(required = "userinfo")
	public Environment evaluate(Environment env) {

		String subUserInfo = env.getString("userinfo", "sub");
		String subIdToken = env.getString(getIdTokenKey(), "claims.sub");

		if (Strings.isNullOrEmpty(subUserInfo)) {
			throw error("Not found \"sub\" in UserInfo response ");
		}

		if (Strings.isNullOrEmpty(subIdToken)) {
			throw error("Not found \"sub\" in " + getIdTokenKey());
		}

		if (!subUserInfo.equals(subIdToken)) {
			throw error("\"sub\" in UserInfo isn't match with \"sub\" in " + getIdTokenKey(), args("sub_user_info", subUserInfo, "sub_id_token", subIdToken));
		}

		logSuccess("UserInfo and IdToken same sub", args("sub_user_info", subUserInfo, "sub_id_token", subIdToken));
		return env;
	}

	protected abstract String getIdTokenKey();
}
