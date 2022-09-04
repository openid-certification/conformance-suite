package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateUserInfoStandardClaims extends AbstractValidateOpenIdStandardClaims {

	@Override
	@PreEnvironment(required = "userinfo")
	public Environment evaluate(Environment env) {

		JsonObject userInfo = env.getObject("userinfo");

		boolean result = new ObjectValidator(null, STANDARD_CLAIMS).isValid(userInfo);
		env.putObject("userinfo_unknown_claims", unknownClaims);
		if (result) {
			logSuccess("Userinfo is valid");
		} else {
			throw error("Userinfo is not valid");
		}

		return env;
	}

}
