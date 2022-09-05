package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ChangeSubInUserInfoResponseToBeInvalid extends AbstractCondition {

	@Override
	@PreEnvironment(required = "user_info_endpoint_response")
	@PostEnvironment(required = "user_info_endpoint_response")
	public Environment evaluate(Environment env) {

		JsonObject userInfoResponse = env.getObject("user_info_endpoint_response");
		userInfoResponse.addProperty("sub", OIDFJSON.getString(userInfoResponse.get("sub")) + "invalid");
		env.putObject("user_info_endpoint_response", userInfoResponse);

		log("Added invalid sub to userinfo endpoint output", userInfoResponse);

		return env;
	}

}
