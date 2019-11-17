package net.openid.conformance.condition.as;

import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class AddInvalidSubToUserInfoResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "user_info_endpoint_response")
	@PostEnvironment(required = "user_info_endpoint_response")
	public Environment evaluate(Environment env) {

		JsonObject userInfoResponse = env.getObject("user_info_endpoint_response");
		userInfoResponse.addProperty("sub", userInfoResponse.get("sub").getAsString() + "invalid");
		env.putObject("user_info_endpoint_response", userInfoResponse);

		logSuccess("Added invalid sub to userinfo endpoint output", userInfoResponse);

		return env;
	}

}
