package net.openid.conformance.condition.client;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateUserInfoStandardClaims extends AbstractValidateOpenIdStandardClaims {

	@Override
	@PreEnvironment(required = "userinfo")
	public Environment evaluate(Environment env) {

		JsonObject userInfo = env.getObject("userinfo");

		if (new ObjectValidator(null, STANDARD_CLAIMS).isValid(userInfo)) {
			logSuccess("Userinfo is valid");
		} else {
			throw error("Userinfo is not valid");
		}

		return env;
	}

}
